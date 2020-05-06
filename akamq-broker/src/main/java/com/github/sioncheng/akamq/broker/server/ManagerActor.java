package com.github.sioncheng.akamq.broker.server;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import com.github.sioncheng.akamq.broker.message.GoOffline;
import com.github.sioncheng.akamq.broker.message.Publish;
import com.github.sioncheng.akamq.broker.persist.InMemoryPersist;
import com.github.sioncheng.akamq.mqtt.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cyq
 * @create 2020-05-01 8:10 PM
 */
public class ManagerActor extends AbstractActor {

    final LoggingAdapter log;

    final Map<String, ActorRef> connectedActors ;

    final ActorRef pubSubActor;

    final Map<String, Map<String, ActorRef>> subscriptionMap;

    final InMemoryPersist inMemoryPersist;

    public ManagerActor() {
        connectedActors = new HashMap<>();
        log = Logging.getLogger(getContext().getSystem(), "manager-actor");
        pubSubActor = getContext().getSystem().actorOf(PubSubActor.props(getSelf()));
        subscriptionMap = new ConcurrentHashMap<>();
        inMemoryPersist = new InMemoryPersist();
    }

    public static Props props() {
        return Props.create(ManagerActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(MQTTConnect.class, this::processMQTTConnect)
                .match(MQTTDisconnect.class, this::processMQTTDisconnect)
                .match(MQTTSubscribe.class, this::processMQTTSubscribe)
                .match(MQTTPingRequest.class, this::processMQTTPingRequest)
                .match(MQTTPublish.class, this::processMQTTPublish)
                .match(Publish.class, this::processSelfPublish)
                .match(MQTTPublishAck.class, this::processMQTTPublishAck)
                .matchAny(this::processAny).build();
    }

    private void processMQTTConnect(MQTTConnect mqttConnect) {
        log.info("ManagerActor->processMQTTConnect {}", mqttConnect);

        byte fixB1 = MQTTMessageType.CONNECT_ACK << 4;
        byte fixB2 = 2;
        byte varB1 = 0; //todo, 0 or 1 according clean session flag

        //todo
        /**
         * 0	0x00连接已接受	连接已被服务端接受
         * 1	0x01连接已拒绝，不支持的协议版本	服务端不支持客户端请求的MQTT协议级别
         * 2	0x02连接已拒绝，不合格的客户端标识符	客户端标识符是正确的UTF-8编码，但服务端不允许使用
         * 3	0x03连接已拒绝，服务端不可用	网络连接已建立，但MQTT服务不可用
         * 4	0x04连接已拒绝，无效的用户名或密码	用户名或密码的数据格式无效
         * 5	0x05连接已拒绝，未授权	客户端未被授权连接到此服务器
         * 6-255		保留
         */
        byte varB2 = 0;

        boolean b = Authority.auth(mqttConnect.getConnectPayload().getUsername(),
                mqttConnect.getConnectPayload().getPassword());

        if (b) {
            ActorRef pre = connectedActors.get(mqttConnect.getConnectPayload().getClientId());
            if (null != pre) {
                pre.tell(new GoOffline(), getSelf());
            }


            ByteString byteString = ByteString.fromArray(new byte[]{fixB1, fixB2, varB1, varB2});
            getSender().tell(byteString, getSelf());

            ActorRef clientSessionActor = createClientSession(getSender());
            connectedActors.put(mqttConnect.getConnectPayload().getClientId(), clientSessionActor);

        } else {
            varB2 = 4; //
            ByteString byteString = ByteString.fromArray(new byte[]{fixB1, fixB2, varB1, varB2});
            getSender().tell(byteString, getSelf());
        }

    }

    private void processMQTTSubscribe(MQTTSubscribe subscribe) {
        log.info("ManagerActor->processSubscribe {}", subscribe);

        ByteStringBuilder subscribeResult = new ByteStringBuilder();
        for (MQTTSubscribeTopic topic: subscribe.getPayload().getTopics()) {
            subscribeResult.addOne((byte)0x00);

            ActorRef clientSession = connectedActors.get(subscribe.getClientId());

            ActorRef pre = subscribe(topic.getTopicFilter(), subscribe.getClientId(), clientSession);

            if (null != pre) {
                pre.tell(new GoOffline(), getSelf());
            }

        }

        byte fixB1 = (byte)(MQTTMessageType.SUBSCRIBE_ACK << 4);
        byte fixB2 = (byte)(subscribeResult.length() + 2);

        ByteStringBuilder byteStringBuilder = new ByteStringBuilder();
        ByteString byteString = byteStringBuilder
                .addOne(fixB1)
                .addOne(fixB2)
                .addOne((byte)(subscribe.getPacketId() >> 8))
                .addOne((byte)(subscribe.getPacketId() & 255))
                .result()
                .concat(subscribeResult.result());



        getSender().tell(byteString, getSender());



    }

    private void processMQTTDisconnect(MQTTDisconnect disconnect) {
        log.info("ManagerActor->processMQTTDisconnect {}", disconnect);

        connectedActors.remove(disconnect.getClientId());
    }

    private void processMQTTPingRequest(MQTTPingRequest request) {
        log.info("ManagerActor->processMQTTPingRequest {}", request);

        byte fixB1 = (byte)(MQTTMessageType.PING_RESPONSE << 4);
        byte fixB2 = 0x00;

        ByteString byteString = ByteString.fromArray(new byte[]{fixB1, fixB2});

        getSender().tell(byteString, getSelf());
    }

    private void processMQTTPublish(MQTTPublish mqttPublish) {
        log.info("ManagerActor->processPublish {}", mqttPublish);
        //todo, persist mqtt publish
        boolean b = true;

        if (mqttPublish.getQosLevel() == 1) {
            b = inMemoryPersist.savePublish(mqttPublish);
        }

        switch (mqttPublish.getQosLevel()) {
            case 0:
                break;
            case 1:
                if (b) {
                    byte fixB1 = (byte)(MQTTMessageType.PUBLISH_ACK << 4);
                    byte fixB2 = 2;
                    byte pidB1 = (byte)(mqttPublish.getPacketId() >> 8);
                    byte pidB2 = (byte)(mqttPublish.getPacketId() & 255);

                    ByteString byteString = ByteString.fromArray(new byte[]{fixB1,fixB2,pidB1,pidB2});

                    getSender().tell(byteString, getSelf());
                }
                break;
            case 2:
                break;
            default:
                break;
        }

        Publish publish = Publish.builder()
                .mqttPublish(mqttPublish)
                .build();

        self().tell(publish, getSelf());
    }

    private void processSelfPublish(Publish publish) {
        log.info("ManagerActor->processPublish {}", publish);

        MQTTPublish mqttPublish = publish.getMqttPublish();
        Map<String, ActorRef> map = subscriptionMap.get(mqttPublish.getTopic());
        for (Map.Entry<String, ActorRef> kv:map.entrySet()){
            ActorRef clientSession = kv.getValue();

            clientSession.tell(mqttPublish, getSelf());
        }

        Integer packetId = publish.getMqttPublish().getPacketId();
        if (null != packetId) {
            inMemoryPersist.removePublish(publish.getMqttPublish().getPacketId());
        }
    }

    private void processMQTTPublishAck(MQTTPublishAck mqttPublishAck) {
        log.info("ManagerActor->processMQTTPublishAck {}", mqttPublishAck);

        ActorRef clientSessionActor = connectedActors.get(mqttPublishAck.getClientId());
        if (null != clientSessionActor) {
            clientSessionActor.tell(mqttPublishAck, getSelf());
        }
    }

    private void processAny(Object o) {

        log.info("ManagerActor->processAny {}", o);
    }

    private ActorRef subscribe(String topic, String clientId, ActorRef clientSession) {
        Map<String, ActorRef> map = subscriptionMap.get(topic);
        if (null == map) {
            map = new HashMap<>();
            subscriptionMap.put(topic, map);
        }

        ActorRef pre = map.get(clientId);

        map.put(clientId, clientSession);

        return pre;

    }

    private ActorRef createClientSession(ActorRef clientActor) {
        Props props = ClientSessionActor.props(getSelf(), clientActor);
        return getContext().getSystem().actorOf(props);
    }
}

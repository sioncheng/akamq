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

    final Map<String, Map<String, ClientSession>> subscriptionMap;

    public ManagerActor() {
        connectedActors = new HashMap<>();
        log = Logging.getLogger(getContext().getSystem(), "manager-actor");
        pubSubActor = getContext().getSystem().actorOf(PubSubActor.props(getSelf()));
        subscriptionMap = new ConcurrentHashMap<>();
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
                .match(Publish.class, this::processMQTTPublish)
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

            connectedActors.put(mqttConnect.getConnectPayload().getClientId(), getSender());

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

            ClientSession clientSession = ClientSession.builder()
                    .clientActor(getSender())
                    .clientId(subscribe.getClientId())
                    .build();

            ClientSession pre = subscribe(topic.getTopicFilter(), clientSession);

            if (null != pre) {
                clientSession.getClientActor().tell(new GoOffline(), getSelf());
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
                .clientActor(getSender())
                .build();

        self().tell(publish, getSelf());
    }

    private void processMQTTPublish(Publish publish) {
        log.info("ManagerActor->processPublish {}", publish);
        publish(publish.getMqttPublish());
    }

    private void processAny(Object o) {

        log.info("ManagerActor->processAny {}", o);
    }

    private ClientSession subscribe(String topic, ClientSession clientSession) {
        Map<String, ClientSession> map = subscriptionMap.get(topic);
        if (null == map) {
            map = new HashMap<>();
            subscriptionMap.put(topic, map);
        }

        ClientSession pre = map.get(clientSession.getClientId());

        map.put(clientSession.getClientId(), clientSession);

        return pre;

    }

    private boolean publish(MQTTPublish mqttPublish) {

        Map<String, ClientSession> map = subscriptionMap.get(mqttPublish.getTopic());
        for (Map.Entry<String, ClientSession> kv:map.entrySet()){
            ClientSession clientSession = kv.getValue();

            byte fixB1 = MQTTMessageType.PUBLISH << 4;
            byte dupFlag = 0;
            byte qosLevel = (byte)(mqttPublish.getQosLevel().byteValue() << 1);
            fixB1 = (byte)(fixB1 + dupFlag + qosLevel);

            byte[] topic = mqttPublish.getRawTopic();
            int packetId = clientSession.getAndIncPacketId();

            byte[] messagePayload = mqttPublish.getRawMessagePayload();

            int remainLength = 2 + topic.length + messagePayload.length;
            if (qosLevel > 1) {
                remainLength += 2;
            }

            ByteStringBuilder byteStringBuilder = new ByteStringBuilder();
            byteStringBuilder.addOne(fixB1);
            byteStringBuilder.append(MQTTUtil.remainLengthToBytes(remainLength));
            byteStringBuilder.addOne((byte)(topic.length / 128));
            byteStringBuilder.addOne((byte)(topic.length % 128));
            byteStringBuilder.append(ByteString.fromArray(topic));
            if (qosLevel > 1) {
                byteStringBuilder.addOne((byte)(packetId / 256));
                byteStringBuilder.addOne((byte)(packetId & 255));
            }
            byteStringBuilder.append(ByteString.fromArray(messagePayload));

            ByteString byteString = byteStringBuilder.result();
            log.info("ManagerActor->publish {}", byteString);

            clientSession.getClientActor().tell(byteString, getSelf());
        }

        return true;
    }
}

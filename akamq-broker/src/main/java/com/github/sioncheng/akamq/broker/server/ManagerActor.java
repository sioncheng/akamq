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
import com.github.sioncheng.akamq.broker.message.PublishAck;
import com.github.sioncheng.akamq.broker.message.Heartbeat;
import com.github.sioncheng.akamq.broker.persist.InMemoryPersistForManager;
import com.github.sioncheng.akamq.broker.persist.PublishItem;
import com.github.sioncheng.akamq.mqtt.*;

import java.time.Duration;
import java.util.*;

/**
 * @author cyq
 * @create 2020-05-01 8:10 PM
 */
public class ManagerActor extends AbstractActor {

    final LoggingAdapter log;

    final Map<String, ActorRef> connectedActors ;

    final Map<String, Set<String>> subscriptionMap;

    final Map<String, InMemoryPersistForManager> inMemoryPersistMap;

    public ManagerActor() {
        connectedActors = new HashMap<>();
        log = Logging.getLogger(getContext().getSystem(), "manager-actor");
        subscriptionMap = new HashMap<>();
        inMemoryPersistMap = new HashMap<>();
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
                .match(Publish.class, this::processForwardPublish)
                .match(MQTTPublishAck.class, this::processMQTTPublishAck)
                .match(PublishAck.class, this::processPublishAck)
                .match(Heartbeat.class, this::processHeartbeat)
                .matchAny(this::processAny).build();
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        Duration d = Duration.ofSeconds(5);
        getContext().getSystem().getScheduler().scheduleAtFixedRate(d,
                d,
                getSelf(),
                Heartbeat.builder().timestamp(System.currentTimeMillis()).build(),
                getContext().getDispatcher(),
                getSelf());
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

            ActorRef clientSessionActor = createClientSession(getSender(), mqttConnect.getConnectPayload().getClientId());
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

            subscribe(topic.getTopicFilter(), subscribe.getClientId());

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

        InMemoryPersistForManager inMemoryPersist = inMemoryPersistMap.get(mqttPublish.getTopic());
        if (null == inMemoryPersist) {
            inMemoryPersist = new InMemoryPersistForManager();
            inMemoryPersistMap.put(mqttPublish.getTopic(), inMemoryPersist);
        }
        Set<String> sub = subscriptionMap.get(mqttPublish.getTopic());
        if (null == sub) {
            if (mqttPublish.getQosLevel() == 1) {
                byte fixB1 = (byte)(MQTTMessageType.PUBLISH_ACK << 4);
                byte fixB2 = 2;
                byte pidB1 = (byte)(mqttPublish.getPacketId() >> 8);
                byte pidB2 = (byte)(mqttPublish.getPacketId() & 255);

                ByteString byteString = ByteString.fromArray(new byte[]{fixB1,fixB2,pidB1,pidB2});

                getSender().tell(byteString, getSelf());
            }

            return;
        }

        long id = inMemoryPersist.savePublish(mqttPublish, sub);


        switch (mqttPublish.getQosLevel()) {
            case 0:
                break;
            case 1:
                if (id > 0) {
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
                .id(id)
                .build();

        self().tell(publish, getSelf());
    }

    private void processForwardPublish(Publish publish) {
        log.info("ManagerActor->processPublish {}", publish);

        InMemoryPersistForManager persist = inMemoryPersistMap.get(publish.getMqttPublish().getTopic());
        PublishItem publishItem = persist.getPublish(publish.getId());
        for (PublishItem.TargetMark target : publishItem.getTargetMarks()){
            ActorRef clientSession = connectedActors.get(target.getClientId());


            clientSession.tell(publish, getSelf());

            target.setStatus((byte)1);
        }
    }

    private void processMQTTPublishAck(MQTTPublishAck mqttPublishAck) {
        log.info("ManagerActor->processMQTTPublishAck {}", mqttPublishAck);

        ActorRef clientSessionActor = connectedActors.get(mqttPublishAck.getClientId());
        if (null != clientSessionActor) {
            clientSessionActor.tell(mqttPublishAck, getSelf());
        }
    }

    private void processPublishAck(PublishAck publishAck) {
        log.info("ManagerActor->processPublishAck {}", publishAck);

        Publish publish = publishAck.getPublish();

        InMemoryPersistForManager inMemoryPersistForManager = inMemoryPersistMap.get(publish.getMqttPublish().getTopic());

        PublishItem publishItem = inMemoryPersistForManager.getPublish(publish.getId());

        List<PublishItem.TargetMark> marks = publishItem.getTargetMarks();

        boolean allAck = false;
        for (PublishItem.TargetMark tm: marks){
            if (tm.getClientId().equals(publishAck.getClientId())) {
                tm.setStatus((byte)1);
                allAck = (publishItem.incAckCounter() == marks.size());
                break;
            }
        }

        if (allAck) {
            log.info("ManagerActor->processPublishAck all ack {}", publishItem);

            inMemoryPersistForManager.removePublish(publishItem.getId());
        }
    }

    private void processHeartbeat(Heartbeat heartbeat) {
        log.info("ManagerActor->processHeartbeat {}", heartbeat);
        for (Map.Entry<String, ActorRef> kv: connectedActors.entrySet()){
            kv.getValue().tell(heartbeat, getSelf());
        }
    }

    private void processAny(Object o) {

        log.info("ManagerActor->processAny {}", o);
    }

    private boolean subscribe(String topic, String clientId) {
        Set<String> sub = subscriptionMap.get(topic);
        if (null == sub) {
            sub = new HashSet<>();
            subscriptionMap.put(topic, sub);
        }

        sub.add(clientId);

        return true;

    }

    private ActorRef createClientSession(ActorRef clientActor, String clientId) {
        Props props = ClientSessionActor.props(getSelf(), clientActor, clientId);
        return getContext().getSystem().actorOf(props);
    }
}

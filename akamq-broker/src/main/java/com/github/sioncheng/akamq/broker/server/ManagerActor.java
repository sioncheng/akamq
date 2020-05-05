package com.github.sioncheng.akamq.broker.server;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import com.github.sioncheng.akamq.mqtt.*;

/**
 * @author cyq
 * @create 2020-05-01 8:10 PM
 */
public class ManagerActor extends AbstractActor {

    final LoggingAdapter log;

    public ManagerActor() {
        log = Logging.getLogger(getContext().getSystem(), "manager-actor");
    }

    public static Props props() {
        return Props.create(ManagerActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(MQTTConnect.class, this::processMQTTConnect)
                .match(MQTTSubscribe.class, this::processMQTTSubscribe)
                .match(MQTTPingRequest.class, this::processMQTTPingRequest)
                .match(MQTTPublish.class, this::processPublish)
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
        byte varB2 = 0; //

        ByteString byteString = ByteString.fromArray(new byte[]{fixB1, fixB2, varB1, varB2});

        getSender().tell(byteString, getSelf());


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

            ClientSession pre = PubSubManager.subscribe(topic.getTopicFilter(), clientSession);

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
                .addOne((byte)(subscribe.getId() >> 8))
                .addOne((byte)(subscribe.getId() & 255))
                .result()
                .concat(subscribeResult.result());



        getSender().tell(byteString, getSender());


    }

    private void processMQTTPingRequest(MQTTPingRequest request) {
        log.info("ManagerActor->processMQTTPingRequest {}", request);

        byte fixB1 = (byte)(MQTTMessageType.PING_RESPONSE << 4);
        byte fixB2 = 0x00;

        ByteString byteString = ByteString.fromArray(new byte[]{fixB1, fixB2});

        getSender().tell(byteString, getSelf());
    }

    private void processPublish(MQTTPublish mqttPublish) {
        log.info("ManagerActor->processPublish {}", mqttPublish);
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

        Thread t  = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10 * 1000);
                    PubSubManager.publish(mqttPublish, getSelf(), log);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        t.run();
    }

    private void processAny(Object o) {

        log.info("ManagerActor->processAny {}", o);
    }
}

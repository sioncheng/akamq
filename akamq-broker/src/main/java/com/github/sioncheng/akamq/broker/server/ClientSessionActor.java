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
import com.github.sioncheng.akamq.mqtt.MQTTMessageType;
import com.github.sioncheng.akamq.mqtt.MQTTPublish;
import com.github.sioncheng.akamq.mqtt.MQTTPublishAck;

import java.util.Random;

/**
 * @author cyq
 * @create 2020-05-05 7:19 PM
 */
public class ClientSessionActor extends AbstractActor {

    final ActorRef manager;

    final ActorRef clientActor;

    final InMemoryPersist inMemoryPersist;

    final LoggingAdapter log;

    int packetId;

    public ClientSessionActor(ActorRef manager, ActorRef clientActor) {
        this.manager = manager;
        this.clientActor = clientActor;
        this.inMemoryPersist = new InMemoryPersist();
        this.log = Logging.getLogger(getContext().getSystem(), "cs-actor");
        this.packetId = (int)(System.currentTimeMillis() % 1000);
    }

    public static Props props(ActorRef manager, ActorRef clientActor) {
        return Props.create(ClientSessionActor.class, manager, clientActor);
    }

    @Override
    public Receive createReceive() {

        return receiveBuilder()
                .match(GoOffline.class, this::processGoOffline)
                .match(MQTTPublish.class, this::processMQTTPublish)
                .match(Publish.class, this::processSelfPublish)
                .match(MQTTPublishAck.class, this::processPublishAck)
                .build();
    }

    private void processGoOffline(GoOffline goOffline) {
        log.info("ClientSessionActor->processGoOffline {}", goOffline);

        this.clientActor.tell(goOffline, getSelf());

    }

    private void processMQTTPublish(MQTTPublish mqttPublish) {
        log.info("ClientSessionActor->processMQTTPublish");

        if (mqttPublish.getQosLevel() == 0) {
            publish(mqttPublish);
        } else {
            inMemoryPersist.savePublish(mqttPublish);
            Publish publish = Publish.builder()
                    .mqttPublish(mqttPublish)
                    .build();
            getSelf().tell(publish, getSelf());
        }
    }

    private void processSelfPublish(Publish publish) {
        log.info("ClientSessionActor->processSelfPublish {}", publish);
        publish(publish.getMqttPublish());
    }

    private void processPublishAck(MQTTPublishAck mqttPublishAck) {
        log.info("ClientSessionActor->processPublishAck {}", mqttPublishAck);
        Integer packetId = mqttPublishAck.getPacketId();
        if (null != packetId) {
            inMemoryPersist.removePublish(packetId);
        }
    }

    private void publish(MQTTPublish mqttPublish) {
        byte fixB1 = MQTTMessageType.PUBLISH << 4;
        byte dupFlag = 0;
        byte qosLevel = (byte)(mqttPublish.getQosLevel().byteValue() << 1);
        fixB1 = (byte)(fixB1 + dupFlag + qosLevel);

        byte[] topic = mqttPublish.getRawTopic();
        int packetId = this.getAndIncPacketId();

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
        log.info("ClientSessionActor->publish {}", byteString);

        clientActor.tell(byteString, getSelf());
    }

    private int getAndIncPacketId() {
        if (this.packetId > 50000) {
            this.packetId = (int)(System.currentTimeMillis() % 1000);
            return this.packetId;
        } else {
            return this.packetId++;
        }
    }
}

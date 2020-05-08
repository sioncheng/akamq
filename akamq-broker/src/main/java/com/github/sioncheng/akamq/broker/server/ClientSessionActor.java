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
import com.github.sioncheng.akamq.mqtt.MQTTMessageType;
import com.github.sioncheng.akamq.mqtt.MQTTPublish;
import com.github.sioncheng.akamq.mqtt.MQTTPublishAck;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author cyq
 * @create 2020-05-05 7:19 PM
 */
public class ClientSessionActor extends AbstractActor {

    final LoggingAdapter log;

    final ActorRef manager;

    final ActorRef clientActor;

    final String clientId;

    final List<WaitWindowItem> waitWindowItems;

    int packetId;

    public ClientSessionActor(ActorRef manager, ActorRef clientActor, String clientId) {
        this.log = Logging.getLogger(getContext().getSystem(), "cs-actor");
        this.manager = manager;
        this.clientActor = clientActor;
        this.clientId = clientId;
        this.waitWindowItems = new LinkedList<>();
        this.packetId = (int)(System.currentTimeMillis() % 1000);
    }

    public static Props props(ActorRef manager, ActorRef clientActor, String clientId) {
        return Props.create(ClientSessionActor.class, manager, clientActor, clientId);
    }

    @Override
    public Receive createReceive() {

        return receiveBuilder()
                .match(GoOffline.class, this::processGoOffline)
                .match(Publish.class, this::processSelfPublish)
                .match(MQTTPublishAck.class, this::processPublishAck)
                .build();
    }

    private void processGoOffline(GoOffline goOffline) {
        log.info("ClientSessionActor->processGoOffline {}", goOffline);

        this.clientActor.tell(goOffline, getSelf());

    }

    private void processSelfPublish(Publish publish) {
        log.info("ClientSessionActor->processSelfPublish {}", publish);

        publish(publish);

        if (publish.getMqttPublish().getQosLevel() == 0) {
            PublishAck publishAck = PublishAck.builder()
                    .publish(publish)
                    .clientId(this.clientId)
                    .build();
            getSender().tell(publishAck, getSelf());
        }
    }

    private void processPublishAck(MQTTPublishAck mqttPublishAck) {
        log.info("ClientSessionActor->processPublishAck {}", mqttPublishAck);

        Iterator<WaitWindowItem> itemIterator = waitWindowItems.iterator();
        while (itemIterator.hasNext()) {
            WaitWindowItem item = itemIterator.next();
            if (item.getPacketId().equals(mqttPublishAck.getPacketId())) {

                PublishAck publishAck = PublishAck.builder()
                        .publish((Publish)item.getAttachment())
                        .clientId(this.clientId)
                        .build();

                manager.tell(publishAck, getSelf());

                itemIterator.remove();
                return;
            }
        }
    }

    private void publish(Publish publish) {
        MQTTPublish mqttPublish = publish.getMqttPublish();

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

        if (qosLevel > 1) {
            WaitWindowItem waitWindowItem = WaitWindowItem.builder()
                    .packetId(packetId)
                    .id(publish.getId())
                    .attachment(publish)
                    .build();

            waitWindowItems.add(waitWindowItem);
        }
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

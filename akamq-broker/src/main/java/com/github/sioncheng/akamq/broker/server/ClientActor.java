package com.github.sioncheng.akamq.broker.server;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.util.ByteIterator;
import akka.util.ByteString;
import com.github.sioncheng.akamq.broker.message.GoOffline;
import com.github.sioncheng.akamq.mqtt.*;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cyq
 * @create 2020-05-01 8:56 PM
 */
public class ClientActor extends AbstractActor {

    final ActorRef connection;

    final InetSocketAddress remote;

    final ActorRef manager;

    final LoggingAdapter log;

    ByteString buf;

    String clientId;

    public ClientActor(ActorRef connection, InetSocketAddress remote, ActorRef manager) {
        this.connection = connection;
        this.remote = remote;
        this.manager = manager;
        this.log = Logging.getLogger(getContext().getSystem(), "client-actor");
        this.buf = null;
        this.clientId = null;

        //sign death pact: this actor stops when the connection(actor) is closed
        getContext().watch(connection);
    }

    public static Props props(ActorRef connection, InetSocketAddress remote, ActorRef manager) {
        return Props.create(ClientActor.class, connection, remote, manager);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(Tcp.Received.class, this::processReceived)
                .match(Tcp.ConnectionClosed.class, this::processConnectionClosed)
                .match(ByteString.class, this::processByteString)
                .build();
    }

    private void processReceived(Tcp.Received received) {
        log.info("ClientActor->processReceived {}", received);

        if (null == this.buf) {
            this.buf = received.data();
        } else {
            this.buf = this.buf.concat(received.data());
        }

        if (this.buf.length() < 2) {
            return;
        }

        final ByteIterator iterator = this.buf.iterator();


        //fix header
        int head1 = iterator.getByte();
        if (head1 < 0) {
            head1 += 256;
        }
        int messageType = head1 >> 4;
        int dupFlag = (head1 >> 3) & 1;
        int qosLevel = (head1 >> 1) & 3;
        int retain = head1 & 1;

        int remainLength = 0;
        int multi = 1;
        while (true) {
            final byte head2 = iterator.getByte();
            remainLength = remainLength  + (head2 & 127) * multi;
            multi = multi * 128;
            if ((head2 & 128) != 128) {
                break;
            }
        }

        MQTTFixHeader fixHeader = MQTTFixHeader.builder()
                .messageType(messageType)
                .dupFlag(dupFlag)
                .qosLevel(qosLevel)
                .retain(retain)
                .remainLength(remainLength)
                .build();

        log.info("ClientActor->processReceived message type {}", messageType);

        try {
            switch (messageType) {
                case MQTTMessageType.CONNECT:
                    processConnect(fixHeader, iterator);
                    break;
                case MQTTMessageType.DISCONNECT:
                    processDisconnect(fixHeader, iterator);
                    break;
                case MQTTMessageType.SUBSCRIBE:
                    processSubscribe(fixHeader, iterator);
                    break;
                case MQTTMessageType.PING_REQUEST:
                    processPingRequest(fixHeader, iterator);
                    break;
                case MQTTMessageType.PUBLISH:
                    processPublish(fixHeader, iterator);
                    break;
                default:
                    break;
            }

            if (!iterator.hasNext()) {
                this.buf = null;
            } else {
                this.buf = ByteString.fromArray(iterator.getBytes(iterator.len()));
            }

        } catch (ParseMQTTException ex) {
            log.error("ClientActor->processReceived {}", ex);
            connection.tell(TcpMessage.close(), getSelf());
        }

    }

    private void processConnect(MQTTFixHeader fixHeader, ByteIterator iterator) throws ParseMQTTException {
        final byte[] bytes = iterator.getBytes(10);
        int protocolLength = bytes[0] * 250 + bytes[1];

        String s = new String(bytes, 2, 4);


        if (!"MQTT".equals(s)) {
            throw new ParseMQTTException(500, s + " is not mqtt");
        }

        byte protocolLevel = bytes[6];
        int connectFlags = bytes[7] + 256;

        int keepLive = bytes[8] * 256 + bytes[9];

        int userNameFlag = connectFlags >> 7;
        int passwordFlag = (connectFlags & 64) >> 6;
        int willRetainFlag = (connectFlags & 32) >> 5;
        int willQos = (connectFlags & 24) >> 3;
        int willFlag = (connectFlags & 4) >> 2;
        int cleanSession = (connectFlags & 2) >> 1;
        int retain = connectFlags & 1;

        if (0 != retain) {
            throw new ParseMQTTException(500, "retain must be 0");
        }

        MQTTConnectFlags mqttConnectFlags = MQTTConnectFlags.builder()
                .userNameFlag(userNameFlag)
                .passwordFlag(passwordFlag)
                .willFlag(willRetainFlag)
                .willQos(willQos)
                .willFlag(willFlag)
                .cleanSession(cleanSession)
                .build();

        log.info("ClientActor->processConnect connect flags {}", mqttConnectFlags);


        //expect client id, will topic, will message, username, password
        //variable strings
        String clientId = takeString(iterator);
        String willTopic = null;
        String willMessage = null;
        if (willFlag == 1) {
            willTopic = takeString(iterator);
            willMessage = takeString(iterator);
        }

        String username = null;
        String password = null;
        if (userNameFlag == 1) {
            username = takeString(iterator);
        }
        if (passwordFlag == 1) {
            password = takeString(iterator);
        }

        //
        this.clientId = clientId;
        MQTTConnectPayload mqttConnectPayload = MQTTConnectPayload.builder()
                .clientId(clientId)
                .willTopic(willTopic)
                .willMessage(willMessage)
                .username(username)
                .password(password)
                .build();

        log.info("ClientActor->processConnect {}", mqttConnectPayload);

        MQTTConnect mqttConnect = MQTTConnect.builder()
                .connectFlags(mqttConnectFlags)
                .connectPayload(mqttConnectPayload)
                .build();

        manager.tell(mqttConnect, getSelf());
    }

    private void processDisconnect(MQTTFixHeader mqttFixHeader, ByteIterator iterator) {
        log.info("ClientActor->processDisconnect {} {}", mqttFixHeader, iterator);

        MQTTDisconnect mqttDisconnect = MQTTDisconnect.builder()
                .clientId(this.clientId)
                .build();

        manager.tell(mqttDisconnect, getSelf());

        connection.tell(TcpMessage.close(), getSelf());
    }

    private void processSubscribe(MQTTFixHeader mqttFixHeader, ByteIterator iterator) {
        log.info("ClientActor->processSubscribe {} {}", mqttFixHeader, iterator);

        int id = iterator.next() * 256 + iterator.next();

        List<MQTTSubscribeTopic> topics = new ArrayList<>(4);
        while (true) {
            String topic = takeString(iterator);
            if (null == topic) {
                break;
            }
            int qos = iterator.next();

            MQTTSubscribeTopic subscribeTopic = MQTTSubscribeTopic.builder()
                    .topicFilter(topic)
                    .requestQos(qos)
                    .build();

            topics.add(subscribeTopic);

            log.info("ClientActor->processSubscribe {}", subscribeTopic);
        }

        MQTTSubscribePayload mqttSubscribePayload = MQTTSubscribePayload.builder()
                .topics(topics)
                .build();

        MQTTSubscribe mqttSubscribe = MQTTSubscribe.builder()
                .id(id)
                .payload(mqttSubscribePayload)
                .clientId(this.clientId)
                .build();

        manager.tell(mqttSubscribe, getSelf());
    }

    private void processPingRequest(MQTTFixHeader fixHeader, ByteIterator iterator) {
        log.info("ClientActor->processPingRequest {}", fixHeader, iterator);

        manager.tell(MQTTPingRequest.builder().build(), getSelf());
    }

    private void processPublish(MQTTFixHeader fixHeader, ByteIterator iterator) {
        log.info("ClientActor->processPublish {} {}", fixHeader, iterator);

        byte[] rawTopic = takeRawString(iterator);
        if (null == rawTopic) {
            log.error("ClientActor->processPublish no topic");
            return;
        }

        try {
            String topic = new String(rawTopic);

            Integer packetId = null;
            if (fixHeader.getQosLevel() > 0) {
                packetId = iterator.next() * 256 + iterator.next();
            }

            byte[] rawMessagePayload = iterator.getBytes(iterator.len());
            String messagePayload = new String(rawMessagePayload, "UTF-8");

            log.info("ClientActor->processPublish {} {}", topic, messagePayload);

            MQTTPublish mqttPublish = MQTTPublish.builder()
                    .qosLevel(fixHeader.getQosLevel())
                    .topic(topic)
                    .packetId(packetId)
                    .messagePayload(messagePayload)
                    .rawTopic(rawTopic)
                    .rawMessagePayload(rawMessagePayload)
                    .build();

            manager.tell(mqttPublish, getSender());

        } catch (UnsupportedEncodingException ex) {
            log.error("ClientActor->processPublish", ex);

        }
    }

    private String takeString(ByteIterator iterator) {
        if (!iterator.hasNext()) {
            return null;
        }

        int len = iterator.next() * 128 + iterator.next();
        return new String(iterator.getBytes(len));
    }

    private byte[] takeRawString(ByteIterator iterator) {
        if (!iterator.hasNext()) {
            return null;
        }

        int len = iterator.next() * 128 + iterator.next();
        return iterator.getBytes(len);
    }

    private void processConnectionClosed(Tcp.ConnectionClosed connectionClosed) {
        getContext().stop(getSelf());
    }

    private void processByteString(ByteString byteString) {
        log.info("ClientActor->processByteString {}", byteString);

        connection.tell(TcpMessage.write(byteString), getSelf());
    }

    private void processGoOffline(GoOffline goOffline) {
        log.info("ClientActor->processGoOffline {}", goOffline);
    }
}

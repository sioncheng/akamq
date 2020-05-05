package com.github.sioncheng.akamq.broker.server;

import akka.actor.ActorRef;
import akka.event.LoggingAdapter;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import com.github.sioncheng.akamq.mqtt.MQTTMessageType;
import com.github.sioncheng.akamq.mqtt.MQTTPublish;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cyq
 * @create 2020-05-03 10:02 PM
 */
public class PubSubManager {

    private static final Map<String, Map<String, ClientSession>> subscriptionMap =
            new ConcurrentHashMap<>();

    public static ClientSession subscribe(String topic, ClientSession clientSession) {
        Map<String, ClientSession> map = subscriptionMap.get(topic);
        if (null == map) {
            map = new HashMap<>();
            subscriptionMap.put(topic, map);
        }

        ClientSession pre = map.get(clientSession.getClientId());

        map.put(clientSession.getClientId(), clientSession);

        return pre;

    }

    public static boolean publish(MQTTPublish mqttPublish, ActorRef actorRef, LoggingAdapter log) {

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

            ByteStringBuilder byteStringBuilder = new ByteStringBuilder();
            byteStringBuilder.addOne(fixB1);
            byteStringBuilder.append(MQTTUtil.remainLengthToBytes(remainLength));
            byteStringBuilder.addOne((byte)(topic.length / 128));
            byteStringBuilder.addOne((byte)(topic.length % 128));
            byteStringBuilder.append(ByteString.fromArray(topic));
            byteStringBuilder.append(ByteString.fromArray(messagePayload));

            ByteString byteString = byteStringBuilder.result();
            log.info("PubSubManager->publish {}", byteString);

            clientSession.getClientActor().tell(byteString, actorRef);
        }

        return true;
    }
}

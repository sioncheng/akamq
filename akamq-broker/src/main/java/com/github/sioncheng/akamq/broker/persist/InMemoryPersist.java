package com.github.sioncheng.akamq.broker.persist;

import com.github.sioncheng.akamq.mqtt.MQTTPublish;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author cyq
 * @create 2020-05-06 10:27 AM
 */
public class InMemoryPersist {

    final List<PublishItem> publishItemList;

    public InMemoryPersist() {
        publishItemList = new LinkedList<>();
    }

    public boolean savePublish(MQTTPublish mqttPublish) {

        PublishItem publishItem = PublishItem.builder()
                .mqttPublish(mqttPublish)
                .timestamp(System.currentTimeMillis())
                .build();

        publishItemList.add(publishItem);

        return true;
    }

    public boolean removePublish(Integer packetId) {

        if (null == packetId) {
            return true;
        }

        publishItemList.removeIf(new Predicate<PublishItem>() {
            @Override
            public boolean test(PublishItem publishItem) {
                return publishItem.getMqttPublish().getPacketId().equals(packetId);
            }
        });

        return true;
    }
}

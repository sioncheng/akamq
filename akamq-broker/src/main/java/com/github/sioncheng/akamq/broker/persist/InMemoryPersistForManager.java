package com.github.sioncheng.akamq.broker.persist;

import com.github.sioncheng.akamq.mqtt.MQTTPublish;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author cyq
 * @create 2020-05-06 10:27 AM
 */
public class InMemoryPersistForManager {

    final List<PublishItem> publishItemList;

    long idCounter;

    public InMemoryPersistForManager() {
        publishItemList = new LinkedList<>();
        idCounter = 1;
    }

    public long savePublish(MQTTPublish mqttPublish, Set<String> targetClientIds) {

        for (PublishItem item:
             publishItemList) {
            if (item.getMqttPublish().getPacketId().equals(mqttPublish.getPacketId())) {
                return -1;
            }
        }

        List<PublishItem.TargetMark> targetMarks = targetClientIds.stream()
                .map(x -> PublishItem.TargetMark.builder().clientId(x).build())
                .collect(Collectors.toList());

        long id = idCounter++;
        PublishItem publishItem = PublishItem.builder()
                .mqttPublish(mqttPublish)
                .timestamp(System.currentTimeMillis())
                .targetMarks(targetMarks)
                .id(id)
                .ackCounter(0)
                .build();

        publishItemList.add(publishItem);

        return id;
    }

    public PublishItem getPublish(long id) {
        for (PublishItem publishItem:
             publishItemList) {

            if (publishItem.getId() == id) {
                return publishItem;
            }
        }

        return null;
    }


    public boolean removePublish(long id) {


        Iterator<PublishItem> itemIterator = publishItemList.iterator();
        while (itemIterator.hasNext()) {
            if (itemIterator.next().getId() == id) {
                itemIterator.remove();
                return true;
            }
        }

        return false;
    }


}

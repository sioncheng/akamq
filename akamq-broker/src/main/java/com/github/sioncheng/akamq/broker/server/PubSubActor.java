package com.github.sioncheng.akamq.broker.server;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.github.sioncheng.akamq.mqtt.MQTTPublish;
import com.github.sioncheng.akamq.mqtt.MQTTSubscribe;

/**
 * @author cyq
 * @create 2020-05-04 7:16 PM
 */
public class PubSubActor extends AbstractActor {

    private ActorRef manager;

    private LoggingAdapter log;

    public PubSubActor(ActorRef manager) {
        this.manager = manager;
        this.log = Logging.getLogger(getContext().getSystem(), "pub-sub-actor");
    }

    public static Props props(ActorRef manager) {
        return Props.create(PubSubActor.class, manager);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(MQTTSubscribe.class, this::processSubscription)
                .match(MQTTPublish.class, this::processPublish)
                .build();
    }

    private void processSubscription(MQTTSubscribe mqttSubscribe) {
        log.info("PubSubActor->processSubscription {}", mqttSubscribe);
    }

    private void processPublish(MQTTPublish mqttPublish) {
        log.info("PubSubActor->processPublish {}", mqttPublish);
    }
}



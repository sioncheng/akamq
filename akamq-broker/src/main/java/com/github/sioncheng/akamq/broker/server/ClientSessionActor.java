package com.github.sioncheng.akamq.broker.server;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.github.sioncheng.akamq.broker.message.Publish;

/**
 * @author cyq
 * @create 2020-05-05 7:19 PM
 */
public class ClientSessionActor extends AbstractActor {

    final ActorRef manager;

    final ActorRef clientActor;

    public ClientSessionActor(ActorRef manager, ActorRef clientActor) {
        this.manager = manager;
        this.clientActor = clientActor;
    }

    public static Props props(ActorRef manager, ActorRef clientActor) {
        return Props.create(ClientSessionActor.class, manager, clientActor);
    }

    @Override
    public Receive createReceive() {

        return receiveBuilder()
                .match(Publish.class, this::processPublish)
                .build();
    }

    private void processPublish(Publish publish) {

    }
}

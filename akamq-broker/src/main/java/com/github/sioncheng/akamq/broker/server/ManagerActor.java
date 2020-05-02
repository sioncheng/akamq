package com.github.sioncheng.akamq.broker.server;

import akka.actor.AbstractActor;
import akka.actor.Props;

/**
 * @author cyq
 * @create 2020-05-01 8:10 PM
 */
public class ManagerActor extends AbstractActor {

    public static Props props() {
        return Props.create(ManagerActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().matchAny(this::processAny).build();
    }

    private void processAny(Object o) {

        System.out.println(o);
    }
}

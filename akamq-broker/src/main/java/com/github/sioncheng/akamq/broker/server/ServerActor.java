package com.github.sioncheng.akamq.broker.server;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;

import java.net.InetSocketAddress;

/**
 * @author cyq
 * @create 2020-05-01 8:00 PM
 */
public class ServerActor extends AbstractActor {

    final ActorRef manager;

    final ServerActorConfiguration serverActorConfiguration;

    final LoggingAdapter log;

    public ServerActor(ActorRef manager, ServerActorConfiguration serverActorConfiguration) {
        this.manager = manager;
        this.serverActorConfiguration = serverActorConfiguration;
        this.log = Logging.getLogger(getContext().getSystem(), "server-actor");
    }

    public static Props props(ActorRef manager, ServerActorConfiguration serverActorConfiguration) {
        return Props.create(ServerActor.class, manager, serverActorConfiguration);
    }

    @Override
    public void preStart() throws Exception {
        final ActorRef tcp = Tcp.get(getContext().getSystem()).manager();
        final InetSocketAddress socketAddress = new InetSocketAddress(this.serverActorConfiguration.getHost(),
                this.serverActorConfiguration.getPort().intValue());
        tcp.tell(TcpMessage.bind(getSelf(), socketAddress, 100), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(Tcp.Bound.class, this::processBound)
                .match(Tcp.CommandFailed.class, this::processCommandFailed)
                .match(Tcp.Connected.class, this::processConnected)
                .matchAny(this::processAny)
                .build();
    }

    private void processBound(Tcp.Bound bound) {
        manager.tell(bound, getSelf());
    }

    private void processCommandFailed(Tcp.CommandFailed commandFailed) {
        manager.tell(commandFailed, getSelf());
        getContext().stop(getSelf());
    }

    private void processConnected(Tcp.Connected connected) {
        log.info("ServerActor->processConnected {}", connected);

        manager.tell(connected, getSelf());

        final ActorRef handler = getContext().actorOf(ClientActor.props(getSender(), connected.remoteAddress(), manager));
        getSender().tell(TcpMessage.register(handler), getSelf());
    }

    private void processAny(Object o) {
        log.info("ServerActor->processAny {}", o);
    }

}

package com.github.sioncheng.akamq.broker.server;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.github.sioncheng.akamq.broker.conf.BrokerConfiguration;

/**
 * @author cyq
 * @create 2020-05-01 7:28 PM
 */
public class BrokerServer{
    private BrokerConfiguration configuration;

    private boolean started;

    private ActorSystem actorSystem;

    private ActorRef managerActor;

    private ActorRef serverActor;

    private BrokerServer(BrokerConfiguration configuration) {
        this.configuration = configuration;
        this.started = false;
    }

    public static BrokerServer build(BrokerConfiguration configuration) {
        return new BrokerServer(configuration);
    }

    public void start() {
        if (this.started) {
            return;
        }

        this.started = true;

        this.actorSystem = ActorSystem.create("broker-server");

        this.managerActor = this.actorSystem.actorOf(ManagerActor.props());

        ServerActorConfiguration serverActorConfiguration = ServerActorConfiguration.builder()
                .host(this.configuration.getHost())
                .port(this.configuration.getPort())
                .build();
        this.serverActor = this.actorSystem.actorOf(ServerActor.props(this.managerActor, serverActorConfiguration));
    }

    public void stop() {

        if (null != this.actorSystem) {
            this.actorSystem.terminate();
        }
    }

}

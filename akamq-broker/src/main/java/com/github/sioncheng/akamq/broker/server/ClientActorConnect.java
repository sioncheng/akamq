package com.github.sioncheng.akamq.broker.server;

import akka.actor.ActorRef;
import com.github.sioncheng.akamq.mqtt.MQTTConnect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-02 10:20 PM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientActorConnect {

    private ActorRef clientActor;

    private MQTTConnect mqttConnect;
}

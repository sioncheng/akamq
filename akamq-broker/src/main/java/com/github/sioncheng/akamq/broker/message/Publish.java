package com.github.sioncheng.akamq.broker.message;

import akka.actor.ActorRef;
import com.github.sioncheng.akamq.mqtt.MQTTPublish;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-05 7:33 PM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Publish {

    private MQTTPublish mqttPublish;

    private ActorRef clientActor;
}

package com.github.sioncheng.akamq.broker.message;

import akka.actor.ActorRef;
import com.github.sioncheng.akamq.mqtt.MQTTSubscribe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-05 7:13 PM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Subscribe {

    private MQTTSubscribe mqttSubscribe;

    private ActorRef clientActor;
}

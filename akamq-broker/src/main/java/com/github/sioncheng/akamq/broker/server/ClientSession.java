package com.github.sioncheng.akamq.broker.server;

import akka.actor.ActorRef;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import scala.Int;

/**
 * @author cyq
 * @create 2020-05-03 10:08 PM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientSession {

    private String clientId;

    private ActorRef clientActor;

    private int packetId;

    @Override
    public int hashCode() {
        return clientId.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof ClientSession) {

            ClientSession that = (ClientSession)o;

            return StringUtils.equals(this.clientId, that.clientId);

        } else {
            return false;
        }
    }

    public int getAndIncPacketId() {
        int n =  this.packetId++;
        if (n > 50000) {
            this.packetId = 0;
            n = this.packetId++;
        }
        return n + 5000;
    }
}

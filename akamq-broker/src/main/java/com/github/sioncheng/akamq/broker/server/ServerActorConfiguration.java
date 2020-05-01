package com.github.sioncheng.akamq.broker.server;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import scala.Int;

/**
 * @author cyq
 * @create 2020-05-01 8:02 PM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServerActorConfiguration {

    private String host;

    private Integer port;
}

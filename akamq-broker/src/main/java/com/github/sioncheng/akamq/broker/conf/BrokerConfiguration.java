package com.github.sioncheng.akamq.broker.conf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-01 7:17 PM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BrokerConfiguration {

    private String host;

    private Integer port;


}

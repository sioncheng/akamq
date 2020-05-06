package com.github.sioncheng.akamq.mqtt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-06 11:37 AM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MQTTPublishAck {

    private String clientId;

    private Integer packetId;
}

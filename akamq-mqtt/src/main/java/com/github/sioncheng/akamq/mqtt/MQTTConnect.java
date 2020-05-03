package com.github.sioncheng.akamq.mqtt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-02 10:09 PM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MQTTConnect {

    private MQTTConnectFlags connectFlags;

    private MQTTConnectPayload connectPayload;
}

package com.github.sioncheng.akamq.mqtt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-05 8:10 PM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MQTTDisconnect {

    private String clientId;
}

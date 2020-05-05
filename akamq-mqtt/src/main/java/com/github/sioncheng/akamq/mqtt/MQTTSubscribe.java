package com.github.sioncheng.akamq.mqtt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-03 2:09 PM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MQTTSubscribe {

    private Integer id;

    private MQTTSubscribePayload payload;

    private String clientId;
}

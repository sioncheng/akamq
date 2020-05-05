package com.github.sioncheng.akamq.mqtt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-04 7:02 PM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MQTTPublish {

    private Integer qosLevel;

    private Integer retain;

    private String topic;

    private Integer packetId;

    private String messagePayload;

    private byte[] rawTopic;

    private byte[] rawMessagePayload;
}

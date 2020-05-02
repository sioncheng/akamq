package com.github.sioncheng.akamq.mqtt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-01 10:52 PM
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MQTTFixHeader {

    private Integer messageType;

    private Integer dupFlag;

    private Integer qosLevel;

    private Integer retain;

    private Integer remainLength;
}

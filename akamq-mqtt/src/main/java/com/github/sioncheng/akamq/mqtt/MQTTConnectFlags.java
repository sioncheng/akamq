package com.github.sioncheng.akamq.mqtt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-02 7:35 PM
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MQTTConnectFlags {

    private Integer userNameFlag;
    private Integer passwordFlag;
    private Integer willRetainFlag;
    private Integer willQos;
    private Integer willFlag;
    private Integer cleanSession;
    private Integer retain;
}

package com.github.sioncheng.akamq.mqtt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-02 10:06 PM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MQTTConnectPayload {

    private String clientId;

    private String willTopic;

    private String willMessage;

    private String username;

    private String password;
}

package com.github.sioncheng.akamq.broker.persist;

import com.github.sioncheng.akamq.mqtt.MQTTPublish;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-06 1:57 PM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishItem {

    private MQTTPublish mqttPublish;

    private Long timestamp;
}

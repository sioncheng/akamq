package com.github.sioncheng.akamq.broker.persist;

import com.github.sioncheng.akamq.mqtt.MQTTPublish;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * @author cyq
 * @create 2020-05-06 1:57 PM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishItem {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TargetMark {

        private String clientId;

        private Byte status;
    }

    private MQTTPublish mqttPublish;

    private Long timestamp;

    private List<TargetMark> targetMarks;

    private Long id;

    private int ackCounter;

    public int incAckCounter() {
        this.ackCounter += 1;

        return this.ackCounter;
    }
}

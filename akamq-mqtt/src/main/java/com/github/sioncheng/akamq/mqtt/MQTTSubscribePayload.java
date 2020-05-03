package com.github.sioncheng.akamq.mqtt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author cyq
 * @create 2020-05-03 2:00 PM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MQTTSubscribePayload {
    List<MQTTSubscribeTopic> topics;
}

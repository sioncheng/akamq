package com.github.sioncheng.akamq.broker.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-08 11:07 AM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishAck {

    private Publish publish;

    private String clientId;
}

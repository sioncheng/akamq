package com.github.sioncheng.akamq.broker.server;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-08 11:25 AM
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WaitWindowItem {

    private Integer packetId;

    private Long id;

    private Long timestamp;

    private Object attachment;

}

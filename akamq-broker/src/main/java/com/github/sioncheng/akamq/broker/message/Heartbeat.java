package com.github.sioncheng.akamq.broker.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cyq
 * @create 2020-05-10 8:29 PM
 */


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Heartbeat {

    private Long timestamp;
}

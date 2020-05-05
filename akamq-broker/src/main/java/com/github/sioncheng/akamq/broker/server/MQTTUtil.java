package com.github.sioncheng.akamq.broker.server;

import akka.util.ByteString;
import akka.util.ByteStringBuilder;

/**
 * @author cyq
 * @create 2020-05-05 3:07 PM
 */
public class MQTTUtil {

    public static ByteString remainLengthToBytes(int len) {
        ByteStringBuilder byteStringBuilder = new ByteStringBuilder();

        while (true) {
            if (len > 128) {
                int n = len % 128;
                byteStringBuilder.addOne((byte) (n | 0x80));
                len = len / 128;
            } else {
                byteStringBuilder.addOne((byte)len);
                break;
            }
        }

        return byteStringBuilder.result();
    }
}

package com.github.sioncheng.akamq.broker.conf;

import akka.util.ByteIterator;
import akka.util.ByteString;
import com.github.sioncheng.akamq.broker.server.MQTTUtil;
import org.junit.Test;

/**
 * @author cyq
 * @create 2020-05-03 12:10 PM
 */
public class ByteTest {

    @Test
    public void test() {
        byte b = -126;

        System.out.println((b + 256) >> 4);
        System.out.println(b >> 4);

        int remainLength = 128 * 127 * 120 + 1;
        System.out.println(remainLength);
        while (true) {
            int m = remainLength % 128;
            remainLength = remainLength / 128;
            System.out.println(m);
            if (remainLength > 0) {
                continue;
            } else {
                break;
            }
        }

        System.out.println(1 * 128 * 128 + 8 * 128 + 119);
        System.out.println(119 * 128 * 128 + 8 * 128 + 1);

        ByteString byteString = MQTTUtil.remainLengthToBytes(1950721);
        ByteIterator iterator = byteString.iterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }
    }
}

package com.github.sioncheng.akamq.broker.conf;

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
    }
}

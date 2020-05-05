package com.github.sioncheng.akamq.broker.server;

/**
 * @author cyq
 * @create 2020-05-05 7:40 PM
 */
public class Authority {

    public static boolean auth(String username, String password) {
        return "test".equals(username) && "test".equals(password);
    }
}

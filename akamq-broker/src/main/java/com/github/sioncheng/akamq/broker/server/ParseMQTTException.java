package com.github.sioncheng.akamq.broker.server;

/**
 * @author cyq
 * @create 2020-05-02 5:17 PM
 */
public class ParseMQTTException extends Exception {

    private int code;

    public int getCode() {
        return code;
    }

    public ParseMQTTException(int code, String message) {
        super(message);
    }
}

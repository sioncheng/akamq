package com.github.sioncheng.akamq.broker.server;

/**
 * @author cyq
 * @create 2020-05-02 5:17 PM
 */
public class ParseMQTTException extends Exception {

    private int code;

    private String message;

    public int getCode() {
        return code;
    }

    public ParseMQTTException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return String.format("%d, %s", code, message);
    }
}

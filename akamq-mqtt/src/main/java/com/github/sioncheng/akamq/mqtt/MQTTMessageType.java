package com.github.sioncheng.akamq.mqtt;

/**
 * @author cyq
 * @create 2020-05-01 10:52 PM
 */
public class MQTTMessageType {

    public static final int RESERVED = 0;

    public static final int CONNECT = 1;

    public static final int CONNECT_ACK = 2;

    public static final int PUBLISH  = 3;

    public static final int PUBLISH_ACK = 4;

    public static final int PUBLISH_RECEIVED = 5;

    public static final int PUBLISH_RELEASE = 6;

    public static final int PUBLISH_COMPLETE = 7;

    public static final int SUBSCRIBE = 8;

    public static final int SUBSCRIBE_ACK = 9;

    public static final int UN_SUBSCRIBE = 10;

    public static final int UN_SUBSCRIBE_ACK = 11;

    public static final int PING_REQUEST = 12;

    public static final int PING_RESPONSE = 13;

    public static final int DISCONNECT = 14;

    public static final int RESERVED_15 = 15;
}

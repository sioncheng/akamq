package com.github.sioncheng.akamq.mqtt;

/**
 * @author cyq
 * @create 2020-05-01 10:52 PM
 */
public class MQTTMessageType {

    public static final Integer RESERVED = 0;

    public static final Integer CONNECT = 1;

    public static final Integer CONNECT_ACK = 2;

    public static final Integer PUBLISH  = 3;

    public static final Integer PUBLISH_ACK = 4;

    public static final Integer PUBLISH_RECEIVED = 5;

    public static final Integer PUBLISH_RELEASE = 6;

    public static final Integer PUBLISH_COMPLETE = 7;

    public static final Integer SUBSCRIBE = 8;

    public static final Integer SUBSCRIBE_ACK = 9;

    public static final Integer UN_SUBSCRIBE = 10;

    public static final Integer UN_SUBSCRIBE_ACK = 11;

    public static final Integer PING_REQUEST = 12;

    public static final Integer PING_RESPONSE = 13;

    public static final Integer DISCONNECT = 14;

    public static final Integer RESERVED_15 = 15;
}

package com.github.sioncheng.akamq.mqtt;

/**
 * @author cyq
 * @create 2020-05-01 10:52 PM
 */
public class MQTTMessageType {

    public static final byte RESERVED = 0;

    public static final byte CONNECT = 1;

    public static final byte CONNECT_ACK = 2;

    public static final byte PUBLISH  = 3;

    public static final byte PUBLISH_ACK = 4;

    public static final byte PUBLISH_RECEIVED = 5;

    public static final byte PUBLISH_RELEASE = 6;

    public static final byte PUBLISH_COMPLETE = 7;

    public static final byte SUBSCRIBE = 8;

    public static final byte SUBSCRIBE_ACK = 9;

    public static final byte UN_SUBSCRIBE = 10;

    public static final byte UN_SUBSCRIBE_ACK = 11;

    public static final byte PING_REQUEST = 12;

    public static final byte PING_RESPONSE = 13;

    public static final byte DISCONNECT = 14;

    public static final byte RESERVED_15 = 15;
}

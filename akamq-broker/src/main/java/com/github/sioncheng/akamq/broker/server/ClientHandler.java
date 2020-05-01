package com.github.sioncheng.akamq.broker.server;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.io.Tcp;
import akka.util.ByteIterator;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * @author cyq
 * @create 2020-05-01 8:56 PM
 */
public class ClientHandler extends AbstractActor {

    final ActorRef connection;

    final InetSocketAddress remote;

    int status;

    ByteString buf;

    public ClientHandler(ActorRef connection, InetSocketAddress remote) {
        this.connection = connection;
        this.remote = remote;
        this.status = 0;
        this.buf = ByteString.emptyByteString();

        //sign death pact: this actor stops when the connection(actor) is closed
        getContext().watch(connection);
    }

    public static Props props(ActorRef connection, InetSocketAddress remote) {
        return Props.create(ClientHandler.class, connection, remote);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(Tcp.Received.class, this::processReceived)
                .match(Tcp.ConnectionClosed.class, this::processConnectionClosed)
                .build();
    }

    private void processReceived(Tcp.Received received) {
        this.buf = this.buf.concat(received.data());

        if (this.buf.length() < 2) {
            return;
        }

        final ByteIterator iterator = this.buf.iterator();
        final byte[] headBytes = iterator.getBytes(2);
        int messageType = headBytes[0] >> 4;
        int dupFlag = (headBytes[0] >> 3) & 1;
        int qosLevel = (headBytes[0] >> 1) & 3;
        System.out.println(messageType);
        System.out.println(dupFlag);
        System.out.println(qosLevel);
    }

    private void processConnectionClosed(Tcp.ConnectionClosed connectionClosed) {
        getContext().stop(getSelf());
    }
}

package com.github.sioncheng.akamq.broker;

import com.github.sioncheng.akamq.broker.conf.BrokerConfigurationLoader;
import com.github.sioncheng.akamq.broker.server.BrokerServer;

/**
 * @author cyq
 * @create 2020-05-01 5:04 PM
 */
public class Application {

    public static void main(String[] args) throws Exception {

        System.out.println("akamq broker");

        BrokerServer.build(BrokerConfigurationLoader.load()).start();

        System.in.read();
    }
}

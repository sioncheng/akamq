package com.github.sioncheng.akamq.broker.conf;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;

/**
 * @author cyq
 * @create 2020-05-01 7:18 PM
 */
public class BrokerConfigurationLoader {

    public static final String DEFAULT_CONF = "akamq-broker.yml";

    public static BrokerConfiguration load() {
        return load(DEFAULT_CONF);
    }

    public static BrokerConfiguration load(String conf) {

        InputStream inputStream = BrokerConfigurationLoader.class.getClassLoader()
                .getResourceAsStream(conf);

        Yaml yaml = new Yaml(new Constructor(BrokerConfiguration.class));

        return yaml.load(inputStream);
    }
}

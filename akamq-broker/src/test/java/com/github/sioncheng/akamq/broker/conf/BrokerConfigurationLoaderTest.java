package com.github.sioncheng.akamq.broker.conf;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author cyq
 * @create 2020-05-01 7:23 PM
 */
public class BrokerConfigurationLoaderTest {

    @Test
    public void testLoad() {
        BrokerConfiguration configuration = BrokerConfigurationLoader.load();
        Assert.assertNotNull(configuration);
        Assert.assertEquals("127.0.0.1", configuration.getHost());
        Assert.assertEquals(1883, configuration.getPort().intValue());
    }
}

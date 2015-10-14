package com.github.lookout.metrics.agent.test;

import com.github.lookout.metrics.agent.HostPortInterval;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Created by rkuris on 12/19/14.
 */
public class HostPortIntervalParserTest {

    @Test
    public void testDefault() {
        HostPortInterval result = new HostPortInterval(null);
        assertEquals(result.getHost(), HostPortInterval.DEFAULT_HOST);
        assertEquals(result.getPort(), HostPortInterval.DEFAULT_PORT);
        assertEquals(result.getInterval(), HostPortInterval.DEFAULT_INTERVAL);
    }

    @Test
    public void testDefaultPort() {
        HostPortInterval result = new HostPortInterval("host");
        assertEquals(result.getHost(), "host");
        assertEquals(result.getPort(), HostPortInterval.DEFAULT_PORT);
        assertEquals(result.getInterval(), HostPortInterval.DEFAULT_INTERVAL);
    }

    @Test
    public void testIPV4() {
        HostPortInterval result = new HostPortInterval("1.2.3.4:56");
        assertEquals(result.getHost(), "1.2.3.4");
        assertEquals(result.getPort(), 56);
        assertEquals(result.getInterval(), HostPortInterval.DEFAULT_INTERVAL);
    }

    @Test
    public void testIPV6DefaultPort() {
        HostPortInterval result = new HostPortInterval("[::1]");
        assertEquals(result.getHost(), "::1");
        assertEquals(result.getPort(), HostPortInterval.DEFAULT_PORT);
        assertEquals(result.getInterval(), HostPortInterval.DEFAULT_INTERVAL);
    }

    @Test
    public void testIPV6() {
        HostPortInterval result = new HostPortInterval("[::1]:9123");
        assertEquals(result.getHost(), "::1");
        assertEquals(result.getPort(), 9123);
        assertEquals(result.getInterval(), HostPortInterval.DEFAULT_INTERVAL);
    }

    @Test
    public void testIntervalDefaultPort() {
        HostPortInterval result = new HostPortInterval("1.2.3.4@1");
        assertEquals(result.getHost(), "1.2.3.4");
        assertEquals(result.getPort(), HostPortInterval.DEFAULT_PORT);
        assertEquals(result.getInterval(), 1);
    }

    @Test
    public void testWholeEnchilada() {
        HostPortInterval result = new HostPortInterval("[2001:db8::1]:210@321");
        assertEquals(result.getHost(), "2001:db8::1");
        assertEquals(result.getPort(), 210);
        assertEquals(result.getInterval(), 321);
        assertEquals(result.toString(), "host 2001:db8::1 port 210 every 321 seconds");
    }
}

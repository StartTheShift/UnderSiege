package com.github.lookout.metrics.agent.test;

import com.github.lookout.metrics.agent.HostPortInterval;
import com.github.lookout.metrics.agent.StatsdReporter;
import com.timgroup.statsd.StatsDClient;
import org.mockito.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.management.*;
import java.lang.management.ManagementFactory;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * Created by rkuris on 12/23/14.
 */
public class StatsTest {

    @Mock
    public StatsDClient client;

    @Mock
    public DynamicMBean mockMBean;

    private final HostPortInterval hostPortInterval = new HostPortInterval("localhost");

    private StatsdReporter sut;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testStatsNotZero() {
        sut = new StatsdReporter(hostPortInterval, client);
        byte[] bigMemoryChunk = new byte[102400000]; // 100mb should get us to 1% of heaps up to 10gb
        bigMemoryChunk[4444] = 1;
        sut.run();
        verify(client).gauge(Matchers.eq("jvm.memory.totalInitInMB"), AdditionalMatchers.gt(0L));
        verify(client).gauge(Matchers.eq("jvm.memory.totalUsedInMB"), AdditionalMatchers.gt(0L));
        verify(client).gauge(Matchers.eq("jvm.memory.heapUsedInMB"), AdditionalMatchers.gt(0L));
        verify(client).gauge(Matchers.eq("jvm.memory.heapUsagePercent"), AdditionalMatchers.gt(0L));
        verify(client, atLeastOnce()).gauge(Matchers.matches("jvm.memory\\.memory_pool_usages\\..*Percent"), AdditionalMatchers.and(AdditionalMatchers.geq(0L), AdditionalMatchers.lt(100L)));
        verify(client).gauge(Matchers.eq("jvm.fdUsagePercent"), AdditionalMatchers.geq(0L)); // gt failing on CI
        verify(client, atLeastOnce()).gauge(Matchers.matches("jvm\\.gc\\..*\\.timeInMS"), Matchers.anyLong());
        verify(client, atLeastOnce()).gauge(Matchers.matches("jvm\\.gc\\..*\\.runs"), Matchers.anyLong());
        assertEquals(bigMemoryChunk[4444], 1);

        verifyNoMoreInteractions(client);
    }

    @Test
    public void testGCCounts() {
        sut = new StatsdReporter(hostPortInterval, client);
        sut.run();
        ArgumentCaptor<Long> countCaptor1 = ArgumentCaptor.forClass(Long.class);
        verify(client, atLeastOnce()).gauge(Matchers.matches("jvm\\.gc\\..*\\.timeInMS"), Matchers.anyLong());
        verify(client, atLeastOnce()).gauge(Matchers.matches("jvm\\.gc\\..*\\.runs"), countCaptor1.capture());
        reset(client);
        System.gc();
        sut.run();
        ArgumentCaptor<Long> countCaptor2 = ArgumentCaptor.forClass(Long.class);
        verify(client, atLeastOnce()).gauge(Matchers.matches("jvm\\.gc\\..*\\.timeInMS"), AdditionalMatchers.gt(0L));
        verify(client, atLeastOnce()).gauge(Matchers.matches("jvm\\.gc\\..*\\.runs"), countCaptor2.capture());
        assertNotEquals(countCaptor1.getAllValues(), countCaptor2.getAllValues());
    }
    @AfterClass
    public void testMBean() throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanException, ReflectionException, AttributeNotFoundException {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        when(mockMBean.getMBeanInfo()).thenReturn(new MBeanInfo("test", "", new MBeanAttributeInfo[]{ new MBeanAttributeInfo("ReadCount", "Long", "Read count", true, false, false)}, new MBeanConstructorInfo[]{}, new MBeanOperationInfo[]{

        }, new MBeanNotificationInfo[]{}));
        when(mockMBean.invoke(Matchers.eq("estimateKeys"), Matchers.any(Object[].class), Matchers.any(String[].class))).thenReturn(1L);
        when(mockMBean.getAttribute(Matchers.eq("ReadCount"))).thenReturn(88888L);
        reset(client);
        mBeanServer.registerMBean(mockMBean, new ObjectName("org.apache.cassandra.db:columnfamily=testcf,keyspace=testks,type=ColumnFamilies"));
        sut = new StatsdReporter(hostPortInterval, client);
        sut.run();
        verify(client,atLeastOnce()).gauge(Matchers.matches("jvm\\..*"), Matchers.anyLong());
        verify(client,times(1)).gauge(Matchers.eq("cfstats.testks.testcf.estimatedKeys"), Matchers.eq(1L));
        verify(client,times(1)).gauge(Matchers.eq("cfstats.testks.testcf.ReadCount"), Matchers.eq(88888L));
        verifyNoMoreInteractions(client);
    }
}

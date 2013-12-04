/**
 * Copyright (C) 2012-2013 Sean Laurent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
/**
    Edited by Jon Haddad at SHIFT to work with
 */

package com.shift.undersiege;


import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.*;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import com.yammer.metrics.stats.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class StatsdReporter extends AbstractPollingReporter implements MetricProcessor<Long> {

    private static final Logger LOG = LoggerFactory.getLogger(StatsdReporter.class);

    protected final VirtualMachineMetrics vm;
    private StatsDClient statsd;
    private boolean printVMMetrics = false;
    protected final Clock clock;
    private MetricPredicate predicate = MetricPredicate.ALL;


    public StatsdReporter(String host, int port, String prefix) throws IOException {
        super(Metrics.defaultRegistry(), "statsd");
        statsd = new NonBlockingStatsDClient(prefix, host, port);
        vm = VirtualMachineMetrics.getInstance();
        clock = Clock.defaultClock();
    }


    @Override
    public void run() {
        try {
            final long epoch = clock.time() / 1000;
            if (this.printVMMetrics) {
                printVmMetrics(epoch);
            }
            printRegularMetrics(epoch);

            // Send UDP data

        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error writing to statsd", e);
            } else {
                LOG.warn("Error writing to statsd: {}", e.getMessage());
            }
        }
    }

    protected void printVmMetrics(long epoch) {
        // Memory
        int div = 1048576;
        statsd.gauge("jvm.memory.totalInitInMB", (int) vm.totalInit() / div);
        statsd.gauge("jvm.memory.totalUsedInMB", (int) vm.totalUsed() / div);

//        sendFloat("jvm.memory.totalMax", StatType.GAUGE, vm.totalMax());
//        sendFloat("jvm.memory.totalCommitted", StatType.GAUGE, vm.totalCommitted());
//
//        statsd.gauge("jvm.memory.helpInitInMB", (int) vm.heapInit() / 1000);
        statsd.gauge("jvm.memory.heapUsedInMB", (int) vm.heapUsed() / div);

//        sendFloat("jvm.memory.heapMax", StatType.GAUGE, vm.heapMax());
//        sendFloat("jvm.memory.heapCommitted", StatType.GAUGE, vm.heapCommitted());
//
        statsd.gauge("jvm.memory.heapUsageInMB", (int) vm.heapUsage() / div);
//        sendFloat("jvm.memory.nonHeapUsage", StatType.GAUGE, vm.nonHeapUsage());
//
        for (Map.Entry<String, Double> pool : vm.memoryPoolUsage().entrySet()) {
//            sendFloat("jvm.memory.memory_pool_usages." + sanitizeString(pool.getKey()), StatType.GAUGE, pool.getValue());
            statsd.gauge("jvm.memory.memory_pool_usages." + pool.getKey(), pool.getValue().intValue() / div);
        }


//        // Buffer Pool
//        final Map<String, VirtualMachineMetrics.BufferPoolStats> bufferPoolStats = vm.getBufferPoolStats();
//        if (!bufferPoolStats.isEmpty()) {
//            sendFloat("jvm.buffers.direct.count", StatType.GAUGE, bufferPoolStats.get("direct").getCount());
//            sendFloat("jvm.buffers.direct.memoryUsed", StatType.GAUGE, bufferPoolStats.get("direct").getMemoryUsed());
//            sendFloat("jvm.buffers.direct.totalCapacity", StatType.GAUGE, bufferPoolStats.get("direct").getTotalCapacity());
//
//            sendFloat("jvm.buffers.mapped.count", StatType.GAUGE, bufferPoolStats.get("mapped").getCount());
//            sendFloat("jvm.buffers.mapped.memoryUsed", StatType.GAUGE, bufferPoolStats.get("mapped").getMemoryUsed());
//            sendFloat("jvm.buffers.mapped.totalCapacity", StatType.GAUGE, bufferPoolStats.get("mapped").getTotalCapacity());
//        }
//
//        sendInt("jvm.daemon_thread_count", StatType.GAUGE, vm.daemonThreadCount());

//        sendInt("jvm.thread_count", StatType.GAUGE, vm.threadCount());
//        sendInt("jvm.uptime", StatType.GAUGE, vm.uptime());
        statsd.gauge("jvm.fd_usage", (int) vm.fileDescriptorUsage());

////
//        for (Map.Entry<Thread.State, Double> entry : vm.threadStatePercentages().entrySet()) {
//            sendFloat("jvm.thread-states." + entry.getKey().toString().toLowerCase(), StatType.GAUGE, entry.getValue());
//        }
//
        for (Map.Entry<String, VirtualMachineMetrics.GarbageCollectorStats> entry : vm.garbageCollectors().entrySet()) {
            final String name = "jvm.gc." + entry.getKey();
            statsd.gauge(name + ".timeInMS", (int) entry.getValue().getTime(TimeUnit.MILLISECONDS));
            statsd.gauge(name + ".runs", (int) entry.getValue().getRuns());
        }
    }

    protected void printRegularMetrics(long epoch) {
        printVmMetrics(epoch);

        Set<Map.Entry<String,SortedMap<MetricName,Metric>>> entries = getMetricsRegistry().groupedMetrics(predicate).entrySet();

        for (Map.Entry<String,SortedMap<MetricName,Metric>> entry : entries) {
            for (Map.Entry<MetricName, Metric> subEntry : entry.getValue().entrySet()) {

                final Metric metric = subEntry.getValue();

                if (metric != null) {
                    try {
                        metric.processWith(this, subEntry.getKey(), epoch);
                        //statsd.gauge(subEntry.getKey(), subEntry.getValue().);
                    } catch (Exception ignored) {
                        LOG.error("Error printing regular com.shift.undersiege:", ignored);
                    }
                }
            }
        }
    }

    @Override
    public void processMeter(MetricName name, Metered meter, Long epoch) throws Exception {
//        System.out.printf("Printing process meter %s %d\n", name.getName(), meter.count());

//
    }

    @Override
    public void processCounter(MetricName name, Counter counter, Long epoch) throws Exception {
        statsd.gauge(name.getName(), (int) counter.count());
    }

    @Override
    public void processHistogram(MetricName name, Histogram histogram, Long epoch) throws Exception {
//        System.out.printf("process histogram %s %f\n", name.getName(), histogram.mean());
    }

    @Override
    public void processTimer(MetricName name, Timer timer, Long context) throws Exception {
//        System.out.printf("timer %s\n", name.getName());
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void processGauge(MetricName name, Gauge<?> gauge, Long context) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
//        System.out.printf("gauge %s %s\n", name.getName(), gauge.toString());
        statsd.gauge(name.getName(), gauge.hashCode());
    }


}

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

package com.github.lookout.metrics.agent;


import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.*;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class StatsdReporter extends AbstractPollingReporter implements MetricProcessor<Long> {

    private static final Logger LOG = LoggerFactory.getLogger(StatsdReporter.class);

    protected final VirtualMachineMetrics vm;
    private final StatsDClient statsd;
    protected final Clock clock;
    private static final MetricPredicate predicate = MetricPredicate.ALL;

    private boolean reportedStartup = false;
    private final HostPortInterval hostPortInterval;
    private final HashMap<String, Integer> previous_run_times;
    private final HashMap<String, Integer> previous_run_counts;

    public StatsdReporter(final HostPortInterval hostPortInterval, final String prefix) {
        super(Metrics.defaultRegistry(), "statsd");
        this.hostPortInterval = hostPortInterval;
        statsd = new NonBlockingStatsDClient(prefix, hostPortInterval.getHost(), hostPortInterval.getPort());
        vm = VirtualMachineMetrics.getInstance();
        clock = Clock.defaultClock();
        previous_run_times = new HashMap<String, Integer>();
        previous_run_counts = new HashMap<String, Integer>();
    }


    @Override
    public void run() {
        if (!reportedStartup || LOG.isDebugEnabled()) {
            LOG.info("Statsd reporting to {}", hostPortInterval);
            reportedStartup = true;
        }
        try {
            final long epoch = clock.time() / 1000;
            printMetrics(epoch);
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error writing to statsd", e);
            } else {
                LOG.warn("Error writing to statsd: {}", e.getMessage());
            }
        }
    }

    protected void printMetrics(long epoch) {
        // Memory
        int div = 1048576;
        statsd.gauge("jvm.memory.totalInitInMB", (int) vm.totalInit() / div);
        statsd.gauge("jvm.memory.totalUsedInMB", (int) vm.totalUsed() / div);
        statsd.gauge("jvm.memory.heapUsedInMB", (int) vm.heapUsed() / div);

        statsd.gauge("jvm.memory.heapUsageInMB", (int) vm.heapUsage() / div);

        for (Map.Entry<String, Double> pool : vm.memoryPoolUsage().entrySet()) {
            statsd.gauge("jvm.memory.memory_pool_usages." + pool.getKey(), pool.getValue().intValue() / div);
        }

        statsd.gauge("jvm.fd_usage", (int) vm.fileDescriptorUsage());

        for (Map.Entry<String, VirtualMachineMetrics.GarbageCollectorStats> entry : vm.garbageCollectors().entrySet()) {
            // we only care about the delta times for the GC time and GC runs

            final String name = "jvm.gc." + entry.getKey();
            String stat_name_time = name + ".timeInMS";

            int total_run_time = (int) entry.getValue().getTime(TimeUnit.MILLISECONDS);
            Integer previous_total_run_time = previous_run_times.get(stat_name_time);

            if (previous_total_run_time == null) {
                previous_total_run_time = 0;
            }
            int delta_run_time = total_run_time - previous_total_run_time;
            previous_run_times.put(stat_name_time, total_run_time);

            statsd.gauge(stat_name_time, delta_run_time);
            String stat_run_count = name + ".runs";

            int total_runs = (int) entry.getValue().getRuns();

            Integer previous_total_runs = previous_run_counts.get(stat_run_count);

            if (previous_total_runs == null) {
                previous_total_runs = 0;
            }

            statsd.gauge(stat_run_count, total_runs - previous_total_runs);
            previous_run_counts.put(stat_run_count, total_runs);
        }

        final Set<Map.Entry<String, SortedMap<MetricName, Metric>>> entries = getMetricsRegistry().groupedMetrics(predicate).entrySet();

        for (final Map.Entry<String, SortedMap<MetricName, Metric>> entry : entries) {
            for (final Map.Entry<MetricName, Metric> subEntry : entry.getValue().entrySet()) {

                final Metric metric = subEntry.getValue();

                if (metric != null) {
                    try {
                        metric.processWith(this, subEntry.getKey(), epoch);
                    } catch (final Exception exception) {
                        LOG.error("Error processing key {}", subEntry.getKey(), exception);
                    }
                }
            }
        }
    }

    @Override
    public void processMeter(MetricName name, Metered meter, Long epoch) throws Exception {
        LOG.debug("Meter {} {} skipped", name.getName(), meter.count());
    }

    @Override
    public void processCounter(MetricName name, Counter counter, Long epoch) throws Exception {
        statsd.gauge(name.getName(), (int) counter.count());
    }

    @Override
    public void processHistogram(MetricName name, Histogram histogram, Long epoch) throws Exception {
        LOG.debug("Histogram {} mean {} skipped", name.getName(), histogram.mean());
    }

    @Override
    public void processTimer(MetricName name, Timer timer, Long context) throws Exception {
        LOG.debug("Timer {} skipped", name.getName());
    }

    @Override
    public void processGauge(MetricName name, Gauge<?> gauge, Long context) throws Exception {
        statsd.gauge(name.getName(), gauge.hashCode());
    }
}

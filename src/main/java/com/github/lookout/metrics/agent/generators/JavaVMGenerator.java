package com.github.lookout.metrics.agent.generators;

import com.timgroup.statsd.StatsDClient;
import com.yammer.metrics.core.VirtualMachineMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by rkuris on 5/28/15.
 */
public class JavaVMGenerator implements MetricGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraJMXGenerator.class);

    protected final VirtualMachineMetrics vm = VirtualMachineMetrics.getInstance();
    private final HashMap<String, Integer> previous_run_times = new HashMap<String, Integer>();
    private final HashMap<String, Integer> previous_run_counts = new HashMap<String, Integer>();

    private int bytesToMB(double bytes) {
        return (int)(bytes/(1024*1024));
    }
    private int doubleToPct(double pct) {
        return (int) Math.round(100 * pct);
    }

    @Override
    public void generate(StatsDClient statsDClient) {
        statsDClient.gauge("jvm.memory.totalInitInMB", bytesToMB(vm.totalInit()));
        statsDClient.gauge("jvm.memory.totalUsedInMB", bytesToMB(vm.totalUsed()));
        statsDClient.gauge("jvm.memory.heapUsedInMB", bytesToMB(vm.heapUsed()));

        statsDClient.gauge("jvm.memory.heapUsagePercent", doubleToPct(vm.heapUsage()));

        for (Map.Entry<String, Double> pool : vm.memoryPoolUsage().entrySet()) {
            statsDClient.gauge("jvm.memory.memory_pool_usages." + pool.getKey() + "Percent", doubleToPct(pool.getValue()));
        }

        statsDClient.gauge("jvm.fdUsagePercent", doubleToPct(vm.fileDescriptorUsage()));

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

            statsDClient.gauge(stat_name_time, delta_run_time);
            String stat_run_count = name + ".runs";

            int total_runs = (int) entry.getValue().getRuns();

            Integer previous_total_runs = previous_run_counts.get(stat_run_count);

            if (previous_total_runs == null) {
                previous_total_runs = 0;
            }

            statsDClient.gauge(stat_run_count, total_runs - previous_total_runs);
            LOG.debug("Reporting {} as {}", stat_run_count, total_runs - previous_total_runs);
            previous_run_counts.put(stat_run_count, total_runs);

        }
    }
}

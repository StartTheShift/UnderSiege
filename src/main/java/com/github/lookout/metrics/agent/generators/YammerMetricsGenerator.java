package com.github.lookout.metrics.agent.generators;

import com.timgroup.statsd.StatsDClient;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by rkuris on 5/28/15.
 */
public class YammerMetricsGenerator implements MetricProcessor<Long>, MetricGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(YammerMetricsGenerator.class);
    private MetricsRegistry registry = Metrics.defaultRegistry();
    private StatsDClient client;
    private static final MetricPredicate predicate = MetricPredicate.ALL;
    protected final Clock clock = Clock.defaultClock();

    @Override
    public void generate(StatsDClient statsDClient) {
        // get all the registered metrics via dropwizard
        this.client = statsDClient;
        final long epoch = TimeUnit.MILLISECONDS.toSeconds(clock.time());
        final Set<Map.Entry<String, SortedMap<MetricName, Metric>>> entries = registry.groupedMetrics(predicate).entrySet();

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
        client.gauge(name.getName(), counter.count());
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
        reportGaugeValue(name.getName(), gauge.value());
    }
    private void reportGaugeValue(String name, Object gaugeValue) {
        if (gaugeValue instanceof Long) {
            long value = ((Long) gaugeValue).longValue();
            LOG.debug("Reporting {} as {}", name, value);
            client.gauge(name, value);
        } else if (gaugeValue instanceof Double) {
            double value = ((Double)gaugeValue).doubleValue();
            LOG.debug("Reporting {} as {}", name, value);
            client.gauge(name, value);
        } else if (gaugeValue instanceof Map) {
            for (Map.Entry<?, ?> entry: ((Map<?,?>)gaugeValue).entrySet()) {
                reportGaugeValue(name + "." + entry.getKey().toString(), entry.getValue());
            }
        }
    }
}

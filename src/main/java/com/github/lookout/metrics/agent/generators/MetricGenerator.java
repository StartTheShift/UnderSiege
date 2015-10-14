package com.github.lookout.metrics.agent.generators;

import com.timgroup.statsd.StatsDClient;

/**
 * Created by rkuris on 5/28/15.
 */
public interface MetricGenerator {
    void generate(StatsDClient statsDClient);
}

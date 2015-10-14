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

import com.github.lookout.metrics.agent.generators.CassandraJMXGenerator;
import com.github.lookout.metrics.agent.generators.JavaVMGenerator;
import com.github.lookout.metrics.agent.generators.MetricGenerator;
import com.github.lookout.metrics.agent.generators.YammerMetricsGenerator;
import com.timgroup.statsd.StatsDClient;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.*;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class StatsdReporter extends AbstractPollingReporter {

    private static final Logger LOG = LoggerFactory.getLogger(StatsdReporter.class);


    private final StatsDClient statsd;

    private boolean reportedStartup = false;
    private final HostPortInterval hostPortInterval;

    private final Set<MetricGenerator> generators = new HashSet<>();

    public StatsdReporter(final HostPortInterval hostPortInterval, final StatsDClient statsd) {
        super(Metrics.defaultRegistry(), "statsd");
        this.hostPortInterval = hostPortInterval;
        this.statsd = statsd;

        // This really should be done with an injection framework, but that's too heavy for this
        generators.add(new CassandraJMXGenerator());
        generators.add(new JavaVMGenerator());
        generators.add(new YammerMetricsGenerator());
    }

    @Override
    public void run() {
        if (!reportedStartup || LOG.isDebugEnabled()) {
            LOG.info("Statsd reporting to {}", hostPortInterval);
            reportedStartup = true;
        }
        for (MetricGenerator generator : generators) {
            try {
                generator.generate(statsd);
            } catch (RuntimeException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Error writing to statsd", e);
                } else {
                    LOG.warn("Error writing to statsd: {}", e.getMessage());
                }
            }
        }
    }
}

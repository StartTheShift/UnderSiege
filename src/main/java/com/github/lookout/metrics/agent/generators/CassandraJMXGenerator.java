package com.github.lookout.metrics.agent.generators;

import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

/**
 * Extracts the relevant JMX values from a running cassandra instance.
 * This code doesn't report anything when it can't find the JMX stuff
 * we care about.
 */
public class CassandraJMXGenerator implements MetricGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraJMXGenerator.class);
    private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    private boolean phiAvailable = true;

    public CassandraJMXGenerator() {
    }

    private void gaugeAndLog(StatsDClient statsDClient, String gaugeName, Double value) {
        statsDClient.gauge(gaugeName, value);
        LOG.debug("Reporting {} as {}", gaugeName, value);
    }
    private void gaugeAndLog(StatsDClient statsDClient, String gaugeName, Long value) {
        statsDClient.gauge(gaugeName, value);
        LOG.debug("Reporting {} as {}", gaugeName, value);
    }

    /**
     * Construct the base gauge name from the JMX object name
     * The gauge is reported as cfstats.KEYSPACE.COLUMNFAMILY.
     * There is a trailing dot, so the actual gauge name can be
     * constructed by appending the item name to the end
     *
     * @param name The JMX ObjectName
     * @return A string in the format cfstats.KEYSPACE.COLUMNFAMILY
     */
    private String gaugeName(ObjectName name) {
        String ks = name.getKeyProperty("keyspace");
        String cf = name.getKeyProperty("columnfamily");
        return String.format("cfstats.%s.%s.", ks, cf);
    }

    /**
     * Some generated gauge names have things like slashes and dots in them.
     * This will replace all non-alnum characters with an underscore.
     * @param name
     * @return
     */
    private String quoteGaugeName(String name) {
        return name.replaceAll("\\W", "_");
    }

    /**
     * These are the per-column-family attributes that we will report to statsd
     */
    public String[] cfAttributeList = new String[]
            {   "ReadCount",
                "WriteCount",
                "RecentReadLatencyMicros",
                "RecentWriteLatencyMicros",
                "TombstonesPerSlice"
            };


    @Override
    public void generate(StatsDClient statsDClient) {
        // Step 1: report PHI values from FailureDetector if available
        if (phiAvailable) {
            try {
                TabularData table = (TabularData) mBeanServer.getAttribute(new ObjectName("org.apache.cassandra.net:type=FailureDetector"), "PhiValues");
                for (Object rawData : table.values()) {
                    final CompositeData data = (CompositeData) rawData;
                    final String gaugeName = "phi." + quoteGaugeName(data.get("Endpoint").toString());
                    gaugeAndLog(statsDClient, gaugeName, (Double) data.get("PHI"));
                }

            } catch (Exception e) {
                LOG.trace("Exception reading PhiValues, skipping", e);
                phiAvailable = false;
            }
        }

        // Step 2: Gossiper information
        try {
            ObjectName snitchName = new ObjectName("org.apache.cassandra.db:type=DynamicEndpointSnitch");
            Map<InetAddress, Double> gossipScores = (Map<InetAddress, Double>) mBeanServer.getAttribute(snitchName, "Scores");
            for (Map.Entry<InetAddress, Double> entry : gossipScores.entrySet()) {
                final String gaugeName = "gossip.score." + quoteGaugeName(entry.getKey().toString());
                gaugeAndLog(statsDClient, gaugeName, entry.getValue());
            }
            Double severity = (Double) mBeanServer.getAttribute(snitchName, "Severity");
            gaugeAndLog(statsDClient, "gossip.severity", severity);
        } catch (Exception e) {
            LOG.debug("Skipping gossip info", e);
        }

        // Step 3: per cf statistics
        Set<ObjectName> names;
        try {
            names = mBeanServer.queryNames(new ObjectName("org.apache.cassandra.db:columnfamily=*,keyspace=*,type=ColumnFamilies"), null);
        } catch (final MalformedObjectNameException e) {
            throw new RuntimeException(e); // impossible
        }
        for (ObjectName name : names) {
            Long keys = null;
            String gaugeName = gaugeName(name);
            try {
                keys = (Long) mBeanServer.invoke(name, "estimateKeys", new Object[]{}, new String[]{});
                gaugeAndLog(statsDClient, gaugeName + "estimatedKeys", keys);
            } catch (final InstanceNotFoundException|MBeanException|ReflectionException e) {
                LOG.debug("Failed to invoke estimateKeys method on {} (ignored)", name, e);
            }
            for (String attr : cfAttributeList) {
                try {
                    Object value = null;
                    try {
                        value = mBeanServer.getAttribute(name, attr);
                    } catch (final MBeanException|InstanceNotFoundException|ReflectionException e) {
                        LOG.debug("Couldn't fetch attribute {} from {} (ignored)", new Object[]{attr, name, e});
                    }
                    final String fullyQualifiedGaugeName = gaugeName + attr;
                    if (value instanceof Long) {
                        statsDClient.gauge(fullyQualifiedGaugeName, (Long) value);
                    } else if (value instanceof Double) {
                        statsDClient.gauge(fullyQualifiedGaugeName, (Double) value);
                    } else if (LOG.isWarnEnabled()){
                        LOG.warn("Type {} for attribute {} of {} is not supported", new Object[]{value.getClass(), attr, name});
                        continue;
                    }
                    LOG.trace("Reporting {} value {}", fullyQualifiedGaugeName, value);
                } catch (final AttributeNotFoundException e) {
                    // don't report missing attributes
                    LOG.debug("Missing attribute {} for {}", attr, name);
                }
            }
        }
    }
}

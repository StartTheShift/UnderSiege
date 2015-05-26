package com.github.lookout.metrics.agent;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class ReportAgent {
    public static void premain(final String agentArgs, final Instrumentation inst) throws IOException {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "unknown-host";
        }

        final String[] reportingHostPorts = (agentArgs != null) ? agentArgs.split(",") : new String[]{null};
        for (final String reportingHostPort : reportingHostPorts) {
            final HostPortInterval hostPortInterval = new HostPortInterval(reportingHostPort);
            final StatsDClient client = new NonBlockingStatsDClient(host, hostPortInterval.getHost(), hostPortInterval.getPort());
            final StatsdReporter reporter = new StatsdReporter(hostPortInterval, client);
            reporter.start(hostPortInterval.getInterval(), TimeUnit.SECONDS);
        }
    }

    public static void main(final String[] args) {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            // impossible
        }
    }
}


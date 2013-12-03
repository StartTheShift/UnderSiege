package com.shift.metrics;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import com.yammer.metrics.reporting.ConsoleReporter;
//import com.yammer.metrics.reporting.GraphiteReporter;
//

public class ReportAgent
{
    public static void premain(String agentArgs, Instrumentation inst) throws IOException
    {
        // comma separated list of
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e) {
            host = "unknown-host";
        }

        StatsdReporter reporter = new StatsdReporter(agentArgs, 8125, host);
        reporter.start(10, TimeUnit.SECONDS);

        System.out.println("STATSD STARTING sending to " + agentArgs + " with host prefix " + host);

    }
    public static void main(String[] args)
    {
        System.out.println("Main running.");
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        System.out.println("Done.");
    }
}


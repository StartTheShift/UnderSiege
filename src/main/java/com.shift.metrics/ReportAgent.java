package com.shift.metrics;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import com.yammer.metrics.reporting.ConsoleReporter;
//import com.yammer.metrics.reporting.GraphiteReporter;
//

public class ReportAgent
{
    public static void premain(String agentArgs, Instrumentation inst) throws IOException
    {
        String host = InetAddress.getLocalHost().getHostName();


        //ConsoleReporter.enable(5, TimeUnit.SECONDS);
        StatsdReporter reporter = new StatsdReporter(agentArgs, 8125);
        reporter.start(1, TimeUnit.SECONDS);

        System.out.println("STATSD STARTING sending to " + agentArgs + " with host prefix " + host);

    }
    public static void main(String[] args)
    {
        System.out.println("Main running.");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        System.out.println("Done.");
    }
}


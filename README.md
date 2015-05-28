Cassandra Statsd Reporting Tool
===============================

Thanks to UnderSiege for getting this project off the ground.

REQUIREMENTS
----------------
* Maven
* Java 7

BUILD
----------------

Check which metrics library cassandra is using by looking in
cassandra/lib/metrics-core*, verify that pom.xml points to the
same exact version. For example, if you have metrics-core-2.2.0.jar,
make sure pom.xml has <version>2.2.0</version>.

`mvn package`

Alternatively, grab the binary from bintray:

`curl -L http://dl.bintray.com/lookout/systems/com/github/lookout/metrics/agent/1.2/agent-1.2.jar -o agent-1.2.jar`

INSTALL
----------------

Copy the statsd library from the .m2 folder to cassandra/lib.
Add the following to your cassandra startup script:

Copy the agent-1.2.jar to a new directory cassandra/plugins

Change cassandra startup to add this agent. This can be done in
a stock install by adding the following to /etc/default/cassandra:

`export JVM_OPTS="-javaagent:/usr/share/cassandra/plugins/agent-1.2.jar=localhost"`

Note the '=localhost' at the end. This supports the following syntaxes:
`hostname:port@interval`
For example:
`your.statsd.host.com:9999@60`
The default port is 8125 and the default interval is 10 (seconds); these
can be omitted. IPV6 is also supported with the following syntax:
`[2001:db8::1]:8888@300`
which means connect to 2001:db8::1 on port 8888 every 300 seconds.

REPORTING
----------------
A log message will be added to the system.log at startup to
confirm that everything is running, it looks like this:

`INFO  [metrics-statsd-thread-1] 2014-12-19 19:05:37,120 StatsdReporter.java:65 - Statsd reporting to host localhost port 8125 every 10 seconds`

WHAT GETS REPORTED
------------------
Lots of stuff:

* Gossip statistics:
    gossip.score.<IP>, which help decide who is closer/faster for queries
    gossip.severity, which indicates how busy this node is self-reporting to others
* Per table statistics:
    cfstats.<keyspace>.<columnfamily>.ReadCount
    cfstats.<keyspace>.<columnfamily>.WriteCount
    cfstats.<keyspace>.<columnfamily>.RecentReadLatencyMicros
    cfstats.<keyspace>.<columnfamily>.RecentWriteLatencyMicros
    cfstats.<keyspace>.<columnfamily>.TombstonesPerSlice
    cfstats.<keyspace>.<columnfamily>.estimatedKeys
    The last one is great for monitoring general trends, but of course don't
    rely on that number to be very accurate.
* PHI reporter
    Also supported is the currently-experimental PHI reporter, in PHI.<IP>,
    coming to a Cassandra cluster near you soon.
* JVM GC metrics
* Anything else registered with yammer-metrics

DEBUGGING
----------------
Not working? There's a lot of tracing and debugging available. Change the
log4j-server.properties and add something like this to get extremely detailed
traces of what it's doing in the server.log.

`log4j.logger.com.github.lookout.metrics.agent.generators=TRACE`

TODO
----------------
Errors that happen during startup are not reported as well as they should
be, mostly because the logging system is not active during startup. The log
message is only generated when the actual metrics collector has run for the
first time.

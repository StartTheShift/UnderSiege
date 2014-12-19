Under Siege
===============

Cassandra Statsd Reporting Tool
--------------------------------

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

INSTALL
----------------

Copy the statsd library from the .m2 folder to cassandra/lib.
Add the following to your cassandra startup script:

Copy the agent-1.0.jar to a new directory cassandra/plugins

Change cassandra startup to add this agent. This can be done in
a stock install by adding the following to /etc/default/cassandra:

`export JVM_OPTS="-javaagent:/usr/share/cassandra/plugins/agent-1.0.jar=localhost"`

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

TODO
----------------
Errors that happen during startup are not reported as well as they should
be, mostly because the logging system is not active during startup. The log
message is only generated when the actual metrics collector has run for the
first time.

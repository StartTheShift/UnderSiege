Cassandra Statsd Reporting Tool
=================================

REQUIREMENTS
=============
* Maven
* Java

BUILD
======

`mvn package`

It may be necessary to build the jar against the same version of the metrics library being used in Cassandra.  

INSTAL
=======

Toss the appropriate version of statsd library (hopefully in your .m2 folder by now) in your cassandra/lib/ directory.
Add the following to your cassandra startup script:

`JVM_OPTS="$JVM_OPTS -javaagent:/path/to/built.jar=localhost"`

Or whatever path you've decided to put your agent.  

Note the '=localhost' at the end.  You should change this to your statsd instance.








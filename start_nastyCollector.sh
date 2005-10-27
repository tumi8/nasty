#!/bin/sh

JAVA_HOME=/usr/lib/SunJava2-1.4.2

cd nastyCollector

$JAVA_HOME/bin/java -cp mysql-connector-java-3.0.16-ga-bin.jar:. de/japes/net/nasty/collector/Collector

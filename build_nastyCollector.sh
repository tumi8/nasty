#!/bin/sh

if [ `id -u` -ne 0 ] ; then
	echo "Installation must be started as root. Exiting."
	exit -1
fi

echo;echo
echo "************************************************************************"
echo "This script builds the nasty collector only."
echo "************************************************************************"
echo;echo

JAVA_HOME=/usr/lib/SunJava2-1.4.2
CURR=`pwd`
CLASSPATH=$CURR/src
INSTALLPATH=$CURR/nastyCollector

mkdir $INSTALLPATH

$JAVA_HOME/bin/javac -d $INSTALLPATH `find src/de/japes/net/nasty/collector/ -name "*.java"` `find src/de/japes/parser/ -name "*.java"`  

if [ "$?" -ne "0" ] ; then
	echo "An error occured during compilation. Exiting."
	exit -1
fi

cp collector.cfg $INSTALLPATH
cp mysql-connector-java-3.0.16-ga-bin.jar $INSTALLPATH
echo;echo
echo "************************************************************************"
echo "The data collector was copied to $INSTALLPATH and can be configured"
echo "with $INSTALLPATH/collector.cfg."
echo "Start the collector with ./start_nastyCollector.sh"
echo "************************************************************************"
echo;echo


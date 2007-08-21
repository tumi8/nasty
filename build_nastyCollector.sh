#!/bin/sh

echo;echo
echo "************************************************************************"
echo "This script builds the nasty collector only."
echo "************************************************************************"
echo;echo

if [ ! -d $JAVA_HOME ] ; then
	echo -n "JAVA_HOME is incorrect. Enter correct path: "
	read JAVA_HOME

	if [ ! -d $JAVA_HOME ] ; then
		echo "Path is incorrect. Exiting."
		exit -1
	fi
fi

if [ ! -f $JAVA_HOME/bin/javac ] ; then
	echo "Java compiler could not be found. Exiting."
	exit -1
fi

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
cp lib/mysql-connector-java-3.0.16-ga-bin.jar $INSTALLPATH
echo;echo
echo "************************************************************************"
echo "The data collector was copied to $INSTALLPATH and can be configured"
echo "with $INSTALLPATH/collector.cfg."
echo "Start the collector with ./start_nastyCollector.sh"
echo "************************************************************************"
echo;echo


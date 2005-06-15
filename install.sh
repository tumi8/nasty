#!/bin/sh

if [ `id -u` -ne 0 ] ; then
	echo "Installation must be started as root. Exiting."
	exit -1
fi

CURR=`pwd`
CLASSPATH=$CURR/src
JAVA_HOME=`find /usr -type d -maxdepth 3 -name "j2sdk*" 2> /dev/null | head -1 `

if [ "$JAVA_HOME" = "" ] ; then
	echo -n "Couldn't find Java SDK. Enter correct path: "
	read JAVAPATH

	if [ ! -d $JAVAPATH ] ; then
		echo "Path is incorrect. Exiting."
		exit -1
	fi
fi

if [ ! -f $JAVA_HOME/bin/javac ] ; then
	echo "Java compiler could not be found. Exiting."
	exit -1
fi

mkdir bin

$JAVA_HOME/bin/javac -classpath $CURR/jsp-api.jar:$CURR/servlet-api.jar:$CURR/jCharts-0.7.5.jar:$CURR/itext-1.2.jar -d $CURR/bin `find src/ -name "*.java"`

if [ "$?" -ne "0" ] ; then
	echo "An error occured during compilation. Exiting."
	exit -1
fi

cd bin
cp --parents de/japes/servlets/nasty/*.class $CURR/tomcat_content/WEB-INF/classes
cp --parents de/japes/parser/*.class $CURR/tomcat_content/WEB-INF/classes
cp --parents de/japes/text/*.class $CURR/tomcat_content/WEB-INF/classes
cp --parents de/japes/beans/nasty/*.class $CURR/tomcat_content/WEB-INF/classes

mkdir /usr/local/nastyCollector
cp --parents de/japes/net/nasty/collector/*.class /usr/local/nastyCollector
cp --parents de/japes/parser/*.class /usr/local/nastyCollector
cp --parents de/japes/parser/nasty/*.class /usr/local/nastyCollector

cd $CURR

cp collector.cfg /usr/local/nastyCollector/
cp nastyColl /usr/local/bin
cp mysql-connector-java-3.0.16-ga-bin.jar /usr/local/nastyCollector/
cp itext-1.2.jar tomcat_content/WEB-INF/lib
cp jCharts-0.7.5.jar tomcat_content/WEB-INF/lib

cd tomcat_content

$JAVA_HOME/bin/jar cvf nasty.war ./*

if [ "$?" -ne "0" ] ; then
        echo "An error occured while creating Web Application.  Exiting."
        exit -1
fi

mv nasty.war ..
cd ..

echo;echo
echo "************************************************************************"
echo "Now copy the nasty.war file to \$TOMCAT_HOME/webapps and restart Tomcat."
echo "The data collector was copied to /usr/local/nastyCollector and can be "
echo "started via a script called nastyColl placed in /usr/local/bin."
echo "************************************************************************"
echo;echo


#!/bin/sh

JAVA_HOME=/usr/lib/SunJava2-1.4.2

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

mkdir -p bin
mkdir -p tomcat_content/WEB-INF/classes
mkdir -p tomcat_content/WEB-INF/lib

$JAVA_HOME/bin/javac -classpath jsp-api.jar:servlet-api.jar:lib/jCharts-0.7.5.jar:lib/itext-1.2.jar -d bin `find src/ -name "*.java"`

if [ "$?" -ne "0" ] ; then
	echo "An error occured during compilation. Exiting."
	exit -1
fi

cd bin

cp --parents de/japes/servlets/nasty/*.class ../tomcat_content/WEB-INF/classes
cp --parents de/japes/parser/*.class ../tomcat_content/WEB-INF/classes
cp --parents de/japes/text/*.class ../tomcat_content/WEB-INF/classes
cp --parents de/japes/beans/nasty/*.class ../tomcat_content/WEB-INF/classes

cd ..

rm -f tomcat_content/WEB-INF/lib/*

# copy standard taglib jar files:
# see also: http://jakarta.apache.org/taglibs/doc/standard-1.0-doc/GettingStarted.html
cp lib/jstl.jar tomcat_content/WEB-INF/lib
cp lib/standard.jar tomcat_content/WEB-INF/lib
cp lib/jaxen-full.jar tomcat_content/WEB-INF/lib
cp lib/saxpath.jar tomcat_content/WEB-INF/lib
cp lib/jdbc2_0-stdext.jar tomcat_content/WEB-INF/lib
cp lib/xercesImpl.jar tomcat_content/WEB-INF/lib
cp lib/xalan.jar tomcat_content/WEB-INF/lib
cp lib/xml-apis.jar tomcat_content/WEB-INF/lib

cp lib/itext-1.2.jar tomcat_content/WEB-INF/lib
cp lib/jCharts-0.7.5.jar tomcat_content/WEB-INF/lib
cp lib/mysql-connector-java-3.0.16-ga-bin.jar tomcat_content/WEB-INF/lib

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
echo "Further installation instructions:"
echo "1) Stop Tomcat."
echo "2) Delete old webapps directory if existing:"
echo "   \$TOMCAT_HOME/webapps/nasty"
echo "3) Delete old context file if existing:"
echo "   \$TOMCAT_HOME/conf/Catalina/localhost/nasty.xml (or similar path)"
echo "4) Copy nasty.war to \$TOMCAT_HOME/webapps."
echo "5) Restart Tomcat."
echo "************************************************************************"
echo;echo


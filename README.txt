EEEEE     TTTTT     M   M
E           T       MM MM
EEEEE       T       M M M
E           T       M   M
EEEEE       T       M   M
---------------------------
Edmunds  Traffic   Manager


Alpha Release (1.0.1)
===
This is an alpha release, the code itself is fairly high quality (it is used to power the Edmunds production website).
However the conversion from internal tool to an open source product is Alpha.

We have simply exposed the internal details of how we run ETM internally.

There are three primary implications of this, you will need to:
1.> Either modify the source or deploy the components to the same file locations as we use.
2.> Use a lot of property files or create DNS text records to reconfigure ETM.
3.> Implement the load balancer module yourself to control your load-balancer **

** - We have not released the load balancer module as it would only be useful to others who use the identical load balancer.
     And there are some licensing restrictions.


Technology Support.
===
We build ETM on Mac (ª) and Deploy on Linux (ª) we do not expect it to work on Windows (ª).

We use a very specific set of technologies:

Apache Httpd     - We depend upon the RegEx based (mod_rewrite) support in order to route URL's.
Apache Tomcat    - Our port detection code for the etm-client library only works on tomcat.
Apache ZooKeeper - Is required for all interprocess communication.
Apache Thrift    - Used to serialized beans into Zookeeper.
Apache Maven     - We identify artifacts by their maven co-ordinates.
Hotspot JVM      - Not tested on any other JVM's.

We have made no effort to support other technologies as a result you are likely to experience a lot of pain if you try to use alternatives.



Edmunds Hacks
===

By default ETM uses the following directories:

sudo mkdir -p /apps/apache-httpd/binsudo mkdir -p /deployments/edmunds/properties/common
sudo mkdir -p /logs/
sudo mkdir -p /var/lib/etm-agent

The quickest way to get ETM up and running is simply to create these directories and set them world writable (chmod 777)
However ensuring they are writable to the user you are going to use for ETM should work too.

You will also need to create two files:


/apps/apache-httpd/bin/apachectl   (Make sure this file is executable chmod +x)

--Start--
#!/bin/bashexit 0
--End--

/deployments/edmunds/properties/common/test-app-etm.properties

--Start--
etmClientSettings.enabled=true
--End--


Finally ETM will execute the command:

sudo service httpd restart

Make sure that this command will succeed when executed as the ETM user.



Downloading Supporting Software
===
wget http://download.oracle.com/otn-pub/java/jdk/6u27-b07/jdk-6u27-linux-x64-rpm.bin
wget http://mirrors.200p-sf.sonic.net/apache/tomcat/tomcat-6/v6.0.33/bin/apache-tomcat-6.0.33.tar.gz
wget http://apache.cs.utah.edu/zookeeper/zookeeper-3.3.3/zookeeper-3.3.3.tar.gz
wget http://www.eng.lsu.edu/mirrors/apache//maven/binaries/apache-maven-3.0.3-bin.tar.gz

Note: We test with Sun (ª) JDK version 1.6, if you have problems with any other version please re-test with this version before logging any bugs. 


The version of thrift we use is really old (0.2.0), hence it is only available in source form:
svn co http://svn.apache.org/repos/asf/thrift/tags/thrift-0.2.0

We also need old versions of the thrift support jar and and maven plugins:
wget http://maven.twttr.com/thrift/libthrift/0.2.0/libthrift-0.2.0.jar
wget https://github.com/dtrott/maven-thrift-plugin/zipball/maven-thrift-plugin-0.1.9 -O maven-thrift-plugin-0.1.9-src.zip



Cloning the ETM source repositories
===

git clone git://github.com/edmunds/automated-test.git
git clone git://github.com/edmunds/edmunds-configuration.git
git clone git://github.com/edmunds/zookeeper-common.git
git clone git://github.com/edmunds/etm-api.git
git clone git://github.com/edmunds/etm-client.git
git clone git://github.com/edmunds/etm-core.git
git clone git://github.com/edmunds/etm-agent.git


Setting up your Build environment
===
Install the Hotspot JDK and add it to your Path.

Unpack maven we assume to: /usr/local/share/maven
Add the maven environment variables to your environment:

--Start--
M2_HOME=/usr/local/share/maven
export M2_HOME

PATH=$PATH:$HOME/bin:$M2_HOME/bin
export PATH
--End--


Install Thrift components
===

Compile old thrift code generator:

--Start--
cd thrift-0.2.0
./bootstrap.sh
./configure
cd compiler/cpp
make
sudo cp thrift /usr/bin
--End--

Install Lib Thrift into maven repository

--Start--
mvn install:install-file -DgroupId=org.apache.thrift -DartifactId=libthrift -Dversion=0.2.0 -Dpackaging=jar -Dfile=libthrift-0.2.0.jar
--End--


Build and install maven thrift plugin version 0.9.0

--Start--
unzip maven-thrift-plugin-0.1.9-src.zip
cd dtrott-maven-thrift-plugin-eb9d203
mvn install
--End--





Compile ETM
===

--Start--
cd  automated-test
mvn install

cd  edmunds-configuration
mvn install

cd  zookeeper-common
mvn install

cd  etm-api
mvn install

cd  etm-client
mvn install

cd  etm-core
mvn install

cd  etm-agent
mvn install
--End--



Setup Deployment Environment
==

Unpack Zookeeper and Tomcat:
--Start--
tar -zvxf zookeeper-3.3.3.tar.gz
tar -zvxf apache-tomcat-6.0.33.tar.gz
--End--

Create file:  zookeeper-3.3.3/conf/zoo.cfg

--Start--
clientPort=2181
dataDir=/tmp/zoo
tickTime=2000
--End--

Start ZooKeeper and Tomcat

--Start--
cd zookeeper-3.3.3/bin
./zkServer.sh start
cd ../apache-tomcat-6.0.33/bin
./startup.sh
--End--





Install ETM Controller
==
--Start--
cp etm-core/etm-controller/target/etm-controller.war apache-tomcat-6.0.33/webapps
--End--


To test open a browser to:
http://localhost:8080/etm-controller/applications.htm


Install Test Application
==
--Start--
cp etm-client/test-app/target/test-app.war apache-tomcat-6.0.33/webapps
--Start--

Refresh the browser (http://localhost:8080/etm-controller/applications.htm) to verify the test application has registered.


Unpack and start the ETM agent.
==
--Start--
mkdir etm-agent/target/etm-agentcd etm-agent/target/etm-agentunzip ../etm-agent.zip
chmod 755 *.sh
./start.sh -fg
--End--

Go to the agents tab (http://localhost:8080/etm-controller/agents.htm) to verify if the agent is operating normally.

#!/bin/sh

dirlocation=`pwd`/../..
echo "We're working with $dirlocation"
cd $dirlocation
echo "Build eu.5ginFIRE.riftioyangschema2java"
if [ ! -d eu.5ginFIRE.riftioyangschema2java ]; then
	git clone https://github.com/5GinFIRE/eu.5ginFIRE.riftioyangschema2java.git
	cd eu.5ginFIRE.riftioyangschema2java/
else
	cd eu.5ginFIRE.riftioyangschema2java/
	git pull
fi

sudo docker run -it --rm -v "/home/ubuntu/.m2":/root/.m2 -v "$(pwd)":/opt/maven -w /opt/maven maven:3.5.2-jdk-8 mvn clean install


echo "Build nfv-requirements-extractor"
cd $dirlocation
if [ ! -d nfv-requirements-extractor ]; then
	git clone https://github.com/5GinFIRE/nfv-requirements-extractor.git
	cd nfv-requirements-extractor
else
	cd nfv-requirements-extractor
	git pull
fi

sudo docker run -it --rm -v "/home/ubuntu/.m2":/root/.m2 -v "$(pwd)":/opt/maven -w /opt/maven maven:3.5.2-jdk-8 mvn clean install

echo "Build eu.5ginfire.portal.api"
cd $dirlocation
cd eu.5ginfire.portal.api
git pull
sudo docker run -it --rm -v "/home/ubuntu/.m2":/root/.m2 -v "$(pwd)":/opt/maven -w /opt/maven maven:3.5.2-jdk-8 mvn clean install
cp target/eu.5ginfire.portal.api-0.0.1-SNAPSHOT.war docker-compose-nginx-jetty-mysql/jetty/wars/

echo "Updating eu.5ginfire.portal.web"
cd $dirlocation
cd eu.5ginfire.portal.api/docker-compose-nginx-jetty-mysql/nginx
if [ ! -d eu.5ginfire.portal.web ]; then
        git clone https://github.com/5GinFIRE/eu.5ginfire.portal.web.git
		cd eu.5ginfire.portal.web
else
        cd eu.5ginfire.portal.web
        git pull
fi

cp src/js/config.js.default src/js/config.js


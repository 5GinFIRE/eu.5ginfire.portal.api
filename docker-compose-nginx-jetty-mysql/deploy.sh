#!/bin/sh

dirlocation=`pwd`/../..
echo "We're working with $dirlocation"
cd $dirlocation
rm eu.5ginFIRE.riftioyangschema2java.git
rm nfv-requirements-extractor.git
git clone https://github.com/5GinFIRE/eu.5ginFIRE.riftioyangschema2java.git
cd eu.5ginFIRE.riftioyangschema2java
mvn clean install
cd $dirlocation
git clone https://github.com/5GinFIRE/nfv-requirements-extractor.git
cd nfv-requirements-extractor
mvn clean install
cd $dirlocation
cd eu.5ginfire.portal.api


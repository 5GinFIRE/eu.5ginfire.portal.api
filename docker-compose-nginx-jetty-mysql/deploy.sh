#!/bin/sh

dirlocation=`pwd`/../..
echo "We're working with $dirlocation"
cd $dirlocation
git clone https://github.com/5GinFIRE/eu.5ginFIRE.riftioyangschema2java.git
git clone https://github.com/5GinFIRE/nfv-requirements-extractor.git

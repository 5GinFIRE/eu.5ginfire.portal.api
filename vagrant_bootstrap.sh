#!/usr/bin/env bash

set -ex

apt-get update
apt-get upgrade

apt-get -y --force-yes install \
default-jdk \
git \
lxc-docker \
maven \
subversion \
apache2


echo "Done installing, putting in place bootstrap content.."



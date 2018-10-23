#!/bin/bash
CLUSTER_USER=$1
CLUSTER_PASSWORD=$2

mvn -Prelease -DskipTests=true
NOW=$(date +"%Y_%m_%d_%T")
curl -k --ftp-create-dirs -T target/nemo-0.0.1-SNAPSHOT-release.zip sftp://$CLUSTER_USER:$CLUSTER_PASSWORD@cluster.math.tu-berlin.de:/net/ils3/nemo_mercartor/nemo-release_$NOW.zip
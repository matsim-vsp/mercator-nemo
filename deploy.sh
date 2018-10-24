#!/bin/bash
SSH_PRIVATE_KEY=$1
CLUSTER_USER=$2

echo "Starting maven clean"
mvn clean
echo "Starting maven release build"
mvn -Prelease -DskipTests=true
NOW=$(date +"%Y_%m_%d_%T")

echo "preparing ssh key"
echo $SSH_PRIVATE_KEY > private_ssh_key
chmod 700 private_ssh_key
echo "Copying release file to math cluster"
scp -i private_ssh_key target/nemo-0.0.1-SNAPSHOT-release.zip $CLUSTER_USER@cluster.math.tu-berlin.de:/net/ils3/nemo_mercartor/nemo-release_$NOW.zip
#!/bin/bash
SSH_PRIVATE_KEY=$1
CLUSTER_USER=$2

echo "Starting maven clean"
mvn clean
echo "Starting maven release build"
mvn -Prelease -DskipTests=true
echo "Setting up ssh-agent"
eval $(ssh-agent -s)
ssh-add <(echo "$SSH_PRIVATE_KEY")
NOW=$(date +"%Y_%m_%d_%T")

echo "Copying release file to math cluster"
scp target/nemo-0.0.1-SNAPSHOT-release.zip $CLUSTER_USER@cluster.math.tu-berlin.de:/net/ils3/nemo_mercartor/nemo-release_$NOW.zip
#!/bin/sh
cd ..
java \
-DJerboa.resourceType=jar \
-cp .:target/*:$1 \
edu.jhu.jerboa.classification.InteractiveAnalytic $2 true


#!/bin/sh
cd ..
mvn clean package
java -DJerboa.resourceType=jar -DJerboaProperties.filename=analytics.properties -cp .:target/*:proj/analytic/models/* edu.jhu.jerboa.classification.InteractiveAnalytic

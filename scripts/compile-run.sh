#!/bin/sh
cd ../java
ant jar
java -DJerboa.resourceType=jar -DJerboaProperties.filename=config/analytics.properties -cp .:dist/*:opt/*:config/ edu.jhu.jerboa.classification.InteractiveAnalytic

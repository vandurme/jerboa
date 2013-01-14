#!/bin/sh
cd ../java
java -DJerboa.resourceType=jar -DJerboaProperties.filename=config/analytics.properties -cp .:dist/*:opt/*:config/ edu.jhu.jerboa.classification.InteractiveAnalytic

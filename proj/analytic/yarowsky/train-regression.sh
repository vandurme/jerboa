#!/bin/sh

# Benjamin Van Durme, vandurme@cs.jhu.edu,  9 Aug 2011

## Purpose: a template script for training a PA regression model.

if [ $# -lt 3 ]; then
    echo "Usage: train-regression.sh PROPERTY INPUT MODEL"
    echo "  PROPERTY: Java properties file, e.g., train.properties"
    echo "  INPUT: file containing instances in SVM-light format"
    echo "  MODEL: name of model file to write to"
    echo "Note: a Jerboa install must be in your CLASSPATH env variable"
    exit -1
fi

java -DJerboaProperties.filename=$1 \
    -Djava.util.logging.config.file=$1 \
    -DPA.form=REGRESSION \
    -DStaticLineStream.files=$2 \
    -DPA.filename=$3 \
    edu.jhu.jerboa.processing.ProcessStream 


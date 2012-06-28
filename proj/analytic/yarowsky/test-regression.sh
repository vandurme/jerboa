#!/bin/sh

# Benjamin Van Durme, vandurme@cs.jhu.edu,  9 Aug 2011

## Purpose: a template script for testing a PA regression model

if [ $# -lt 3 ]; then
    echo "Usage: test-regression.sh PROPERTY MODEL INPUT LABELS"
    echo "  PROPERTY: Java properties file, e.g., test.regression.properties"
    echo "  INPUT: file containing instances in SVM-light format"
    echo "  LABELS: name of file to write predictions to"
    echo "Note: a Jerboa install must be in your CLASSPATH env variable"
    exit -1
fi

java -DJerboaProperties.filename=$1 \
    -Djava.util.logging.config.file=$1 \
    -DPA.form=REGRESSION \
    -DStaticLineStream.files=$3 \
    -DPA.filename=$2 \
    -DClassifierTester.resultsFilename=$4 \
    edu.jhu.jerboa.processing.ProcessStream 

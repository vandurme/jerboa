#!/bin/sh

# Benjamin Van Durme, vandurme@cs.jhu.edu,  3 Nov 2010

## example: process.sh ~/jerboa do-something.properties -DXmx10g

ROOT=$1
FILE=$2
shift
shift

java -cp ${ROOT}/java/dist/*: \
    -DJerboaProperties.filename=${FILE} \
    -Djava.util.logging.config.file=${ROOT}/src/main/resources/logging.properties \
    $* \
    edu.jhu.jerboa.processing.ProcessStream 

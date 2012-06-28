#!/bin/sh

# Benjamin Van Durme, vandurme@cs.jhu.edu,  3 Nov 2010

ROOT=$1
FILE=$2
shift
shift

java -cp /home/hltcoe/bvandurme/jerboa/java/dist/*:/home/hltcoe/bvandurme/rebar/java/lib/accumulo/*:/home/hltcoe/bvandurme/rebar/java/lib/*:/home/hltcoe/bvandurme/rebar/java/dist/scale.jar \
    -DJerboaProperties.filename=${FILE} \
    -Djava.util.logging.config.file=${ROOT}/analytic/pipeline/logging.properties \
    $* \
    edu.jhu.jerboa.processing.ProcessStream 
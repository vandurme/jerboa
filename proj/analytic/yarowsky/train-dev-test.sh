#!/bin/bash

# Benjamin Van Durme, vandurme@cs.jhu.edu, 12 Jul 2011

## Purpose: given a set of instances, one per line, create files called:
## train-$1, dev-$1, and test-$1, containing 90%, 5%, 5% of the data,
## respectively, randomly selected.

## Usage: train-dev-test.sh INPUT_FILENAME OUTPUT_PREFIX

JerboaRoot=/home/hltcoe/bvandurme/Jerboa

trainPercentage=0.9
devPercentage=0.05
testPercentage=0.05

instances=$1
outputPrefix=$2
shuffled=`mktemp /tmp/shuffled.XXXX`

${JerboaRoot}/src/scripts/shuffle $instances > $shuffled
x=`wc -l $shuffled | cut -f1 -d' '`
trainLength=`echo "($x * $trainPercentage) / 1" | bc`
devLength=`echo "($x * $devPercentage) / 1" | bc`
testLength=`echo "(($x - $trainLength) - $devLength)" | bc`

echo "train: $trainLength	dev: $devLength	test: $testLength"

head -$trainLength $shuffled > $outputPrefix.train
head -`echo "($trainLength + $devLength)" | bc` $shuffled | tail -$devLength > $outputPrefix.dev
tail -$testLength $shuffled > $outputPrefix.test
rm $shuffled



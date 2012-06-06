#!/bin/bash

## vandurme: A derivative of a script written by Matt Post

# Purpose: trains a model using LIBLINEAR, searching over regularization values
# on a dev set created from the training data.

devPercentage=0.05

ROOT=$1
trainFile=$2
finalModelFile=$3

train=${ROOT}/3rd/liblinear-1.8/train
predict=${ROOT}/3rd/liblinear-1.8/predict

devFile=`mktemp /tmp/dev.XXXX`
newTrainFile=`mktemp /tmp/train.XXXX`
trainLength=`wc -l $trainFile | cut -f1 -d' '`
newTrainLength=`echo "($trainLength * (1 - $devPercentage)) / 1 " | bc`
head -$newTrainLength $trainFile > $newTrainFile
devLength=`echo "$trainLength - $newTrainLength" | bc`
tail -$devLength $trainFile  > $devFile

echo "Training on $newTrainLength lines of $trainFile, with dev tuning on the other $devLength lines"

best=0
bestsmooth=0.00001
model=`mktemp /tmp/model.XXXX`
for smooth in 0.00001 0.0001 0.001 0.01 0.1 1 10 100 1000 10000; do
    rm $model
    model=`mktemp /tmp/model.$smooth.XXXX`
    #$train -q -B 1 -c $smooth $newTrainFile $model
    $train -s 0 -q -c $smooth $newTrainFile $model
    accuracy=$($predict $devFile $model /dev/null | awk '{print $3}' | sed 's/%//')
    echo "SMOOTH $smooth ACC $accuracy BEST $best ($bestsmooth)"
    improved=$(echo "$accuracy > $best" | bc)
    if test $improved -eq 1; then
	best=$accuracy
	bestsmooth=$smooth
	cp $model $finalModelFile
    fi
done
rm $newTrainFile
rm $devFile
echo "best smooth on dev was $bestsmooth ($best)"
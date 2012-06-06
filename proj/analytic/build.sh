#!/bin/sh

# Benjamin Van Durme, vandurme@cs.jhu.edu,  9 Aug 2011

## Purpose: a template script for building an analytic. For building a new
## analytic, one would copy this file into a build directory, set the initial
## BUILD path, and add whatever customization arguments to the relevant commands
## that are needed.
##
## For more aggressive tailoring, one would copy the relevant property files
## referenced below, and modify those directly for the particular analytic.
## Recall in doing this that configuration options provided on the command line
## will supersede any property specified in a Jerboa property file, (see the
## various get methods in JerboaProperties.java), so adding customization to this
## script will already let you over-ride any property in the template property
## files referenced here.

if [ $# -lt 3 ]; then
    echo "Usage: build.sh TYPE ROOT BUILD"
    echo "  TYPE: [log-linear|pa-binary|pa-regression|pa-multi]"
    echo "  ROOT: e.g., /home/joe/jerboa"
    echo "  BUILD: e.g., /home/joe/jerboa/analytic/english-gender"
    exit -1
fi

TYPE=$1
## e.g., ROOT=/home/hltcoe/bvandurme/jerboa
ROOT=$2
## e.g., BUILD=${ROOT}/analytic/english-gender/enron
BUILD=$3
PROCESS=${ROOT}/scripts/process.sh


## This step is specifically for the benefit of the NGram feature, which one may
## not always nec. want/need for a given analytic, in which case you can skip
## this step.
echo "--------------------"
echo "--------------------"
echo "Creating ngram feature table from train set"
echo ""
echo "${PROCESS} ${ROOT} ${BUILD}/wordList.properties -DBUILD=${BUILD}"
${PROCESS} ${ROOT} ${BUILD}/wordList.properties -DBUILD=${BUILD}

## This performs feature extraction, and converts each training instances into a
## single line of the "SVM light" style format, with label followed by a sorted,
## sparse feature:value representation. That format can be used by Jerboa via
## the InstanceLineParser, as well as SVM Light, and here, liblinear.
echo "--------------------"
echo "--------------------"
echo "Making instance representations of train"
echo ""
case $TYPE in
    log-linear | pa-binary )
	      echo "${PROCESS} ${ROOT} ${BUILD}/instanceMaker.binary.properties -DBUILD=${BUILD}"
	      ${PROCESS} ${ROOT} ${BUILD}/instanceMaker.binary.properties -DBUILD=${BUILD}
	      ;;
    pa-regression )
	      echo "${PROCESS} ${ROOT} ${BUILD}/instanceMaker.regression.properties -DBUILD=${BUILD}"
	      ${PROCESS} ${ROOT} ${BUILD}/instanceMaker.regression.properties -DBUILD=${BUILD}
	      ;;
    pa-multi )
	      echo "Currently not converting to Instances when using PA Multiclass, to be done in future"
	##${PROCESS} ${ROOT} ${BUILD}/instanceMaker.multi.properties -DBUILD=${BUILD}
	      ;;
    * )
	      echo "TYPE not recognized"
	      exit -1
	      ;;
esac	

case $TYPE in
    ## Calls out to a 3rd party library, LibLinear, for building the model. One
    ## needs to download that library separately if you plan on using it, as it
    ## is not part of the Jerboa distribution.
    log-linear )
	      echo "--------------------"
	      echo "--------------------"
	      echo "Training model with liblinear"
	      echo ""
	      echo "${ROOT}/proj/analytic/train-liblinear.sh ${ROOT} ${BUILD}/train.instances ${BUILD}/model.logistic"
	      ${ROOT}/proj/analytic/train-liblinear.sh ${ROOT} ${BUILD}/train.instances ${BUILD}/model.logistic
	      TEST=test.logistic.properties
	      ;;

    pa-binary )
	      echo "--------------------"
	      echo "--------------------"
	      echo "Training binary classifier using Passive Aggressive";
	      echo ""
	      echo "${PROCESS} ${ROOT} ${BUILD}/train.pa.properties -DPA.form=BINARY -DBUILD=${BUILD}"
	      ${PROCESS} ${ROOT} ${BUILD}/train.pa.properties -DPA.form=BINARY -DBUILD=${BUILD}
	      ;;

    pa-regression )
	      echo "--------------------"
	      echo "--------------------"
	      echo "Training regression classifier using Passive Aggressive";
	      echo ""
	      echo "${PROCESS} ${ROOT} ${BUILD}/train.pa.properties -DPA.form=REGRESSION -DBUILD=${BUILD}"
	      ${PROCESS} ${ROOT} ${BUILD}/train.pa.properties -DPA.form=REGRESSION -DBUILD=${BUILD}
	      ;;
    pa-multi )
	      echo "--------------------"
	      echo "--------------------"
	      echo "Training multiclass classifier using Passive Aggressive";
	      echo ""
	      echo "${PROCESS} ${ROOT} ${BUILD}/train.pa-multi.properties -DBUILD=${BUILD}"
	      ${PROCESS} ${ROOT} ${BUILD}/train.pa-multi.properties -DBUILD=${BUILD}
	      ;;
esac

echo "--------------------"
echo "--------------------"
echo "Test classifier in batch mode, on test set"
echo ""
echo "${PROCESS} ${ROOT} ${BUILD}/test.$TYPE.properties -DBUILD=${BUILD}"
results=`${PROCESS} ${ROOT} ${BUILD}/test.$TYPE.properties -DBUILD=${BUILD}`
echo "Results: $results"
echo "echo \$results > ${BUILD}/batch-test-results.${TYPE}.txt"
echo "$results" > ${BUILD}/batch-test-results.$TYPE.txt


# echo "--------------------"
# echo "--------------------"
# echo "Test dynamic model with liblinear"
# echo ""
# echo "${PROCESS} ${BUILD}/dynamicClassifier.logistic.properties"
# ${PROCESS} ${BUILD}/dynamicClassifier.logistic.properties

# ${ROOT}/src/scripts/align-logistic-model-features.pl ${BUILD}/model.logistic ${BUILD}/feature-map.tsv | sort -k2 -t'' -gr > ${BUILD}/feature-weights.tsv
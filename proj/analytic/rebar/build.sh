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

if [ $# -lt 4 ]; then
    echo "Usage: build.sh TYPE ROOT BUILD SRC"
    echo "  TYPE: [log-linear|pa-binary|pa-regression|pa-multi]"
    echo "  ROOT: e.g., /home/joe/Jerboa"
    echo "  BUILD: e.g., /home/joe/Jerboa/analytic/english-gender"
    echo "  SRC: e.g., /home/joe/Jerboa/analytic/english-gender/src"
    exit -1
fi

TYPE=$1
# e.g., ROOT=/home/hltcoe/svolkova/Jerboa
ROOT=$2
# BUILD=${ROOT}/analytic/english-gender/build/enron
BUILD=$3
SRC=$4

# This step is specifically for the benefit of the NGram feature, which one may
# not always nec. want/need for a given analytic, in which case you can skip
# this step.
echo "--------------------"
echo "--------------------"
echo "Creating ngram feature table from train set"
echo ""
echo "${SRC}/process.sh ${ROOT} ${SRC}/wordList.properties -DBUILD=${BUILD}"
${SRC}/process.sh ${ROOT} ${SRC}/wordList.properties -DBUILD=${BUILD}

# This performs feature extraction, and converts each training instances into a
# single line of the "SVM light" style format, with label followed by a sorted,
# sparse feature:value representation. That format can be used by Jerboa via
# the InstanceLineParser, as well as SVM Light, and here, liblinear.
echo "--------------------"
echo "--------------------"
echo "Making instance representations of train"
echo ""
case $TYPE in
    log-linear | pa-binary )
	      echo "${SRC}/process.sh ${ROOT} ${SRC}/instanceMaker.binary.properties -DBUILD=${BUILD}"
	      ${SRC}/process.sh ${ROOT} ${SRC}/instanceMaker.binary.properties -DBUILD=${BUILD}
	      ;;
    pa-regression )
	      echo "${SRC}/process.sh ${ROOT} ${SRC}/instanceMaker.regression.properties -DBUILD=${BUILD}"
	      ${SRC}/process.sh ${ROOT} ${SRC}/instanceMaker.regression.properties -DBUILD=${BUILD}
	      ;;
    pa-multi )
	      echo "Currently not converting to Instances when using PA Multiclass, to be done in future"
	      ${SRC}/process.sh ${ROOT} ${SRC}/instanceMaker.multi.properties -DBUILD=${BUILD}
	      ;;
    * )
	      echo "TYPE not recognized"
	      exit -1
	      ;;
esac	

case $TYPE in
   # Calls out to a 3rd party library, LibLinear, for building the model. One
   # needs to download that library separately if you plan on using it, as it
   # is not part of the Jerboa distribution.
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
	      echo "${SRC}/process.sh ${ROOT} ${SRC}/train.pa.properties -DPA.form=BINARY -DBUILD=${BUILD}"
	      ${SRC}/process.sh ${ROOT} ${SRC}/train.pa.properties -DPA.form=BINARY -DBUILD=${BUILD}
	      ;;

    pa-regression )
	      echo "--------------------"
	      echo "--------------------"
	      echo "Training regression classifier using Passive Aggressive";
	      echo "${SRC}/process.sh ${ROOT} ${SRC}/train.pa.properties -DPA.form=REGRESSION -DBUILD=${BUILD}"
	      ${SRC}/process.sh ${ROOT} ${SRC}/train.pa.properties -DPA.form=REGRESSION -DBUILD=${BUILD}
	      ;;
    pa-multi )
	      echo "--------------------"
	      echo "--------------------"
	      echo "Training multiclass classifier using Passive Aggressive";
	      echo ""
	      echo "${SRC}/process.sh ${ROOT} ${SRC}/train.pa-multi.properties -DBUILD=${BUILD}"
	      ${SRC}/process.sh ${ROOT} ${SRC}/train.pa-multi.properties -DBUILD=${BUILD}
	      ;;
esac

${ROOT}/src/scripts/align-logistic-model-features.pl ${BUILD}/model.logistic ${BUILD}/feature-map.tsv | sort -k2 -t'	' -gr > ${BUILD}/feature-weights.tsv

echo "--------------------"
echo "--------------------"
echo "Test classifier in batch mode, on test set"
echo ""
echo "${SRC}/process.sh ${ROOT} ${SRC}/test.$TYPE.properties -DBUILD=${BUILD}"
results=`${SRC}/process.sh ${ROOT} ${SRC}/test.$TYPE.properties -DBUILD=${BUILD}`
echo "Results: $results"
echo "echo \$results > ${BUILD}/batch-test-results.${TYPE}.txt"
echo "$results" > ${BUILD}/batch-test-results.$TYPE.txt

# echo "--------------------"
# echo "--------------------"
# echo "Fit Approximation parameters"
# echo ""
# echo "rm ${BUILD}/approximation-fitting.txt"
# #rm -f ${BUILD}/approximation-fitting.txt
# best=-1
# for maxUpdate in 6.0 7.0 8.0 9.0 10.0 11.0; do
#     #for morrisBase in 1.2 1.3 1.4 1.5 1.6 1.7 1.8 1.9; do
#     morrisBase=1.3
#         granularity=100
#         #for granularity in 10 50 100 200; do
#             echo "${SRC}/process.sh ${ROOT} ${SRC}/dynamic.logistic.approxFit.properties -DBUILD=${BUILD} -DNGram.maxUpdate=${maxUpdate} -DNGram.morrisBase=${morrisBase} -DNGram.granularity=${granularity} >> ${BUILD}/approximation-fitting.txt"
#             echo "$maxUpdate $morrisBase $granularity" >> ${BUILD}/approximation-fitting.txt
#             ${SRC}/process.sh ${ROOT} ${SRC}/dynamic.logistic.approxFit.properties -DBUILD=${BUILD} -DNGram.maxUpdate=${maxUpdate} -DNGram.morrisBase=${morrisBase} -DNGram.granularity=${granularity}  >> ${BUILD}/approximation-fitting.txt 2> /dev/null
#             result=$(tail -1 ${BUILD}/approximation-fitting.txt | perl -pe 's/Loss:\s+(\d+).*/$1/')
#             echo "LOSS: $result"
#             improved=$(echo "$result < $best" | bc)
#             if test $improved -eq 1; then
#                 bestMaxUpdate=$maxUpdate
#                 bestMorrisBase=$morrisBase
#                 bestGranularity=$granularity
#                 best=$result
#             fi
#             if test $best -eq -1; then
#                 bestMaxUpdate=$maxUpdate
#                 bestMorrisBase=$morrisBase
#                 bestGranularity=$granularity
#                 best=$result
#             fi
#             echo "bestMaxUpdate: $bestMaxUpdate    bestMorrisBase: $bestMorrisBase    bestGranularity: $bestGranularity"
#         #done
#     #done
# done

# echo "--------------------"
# echo "--------------------"
# echo "Test dynamic model with liblinear"
# echo ""
# echo "${SRC}/process.sh ${SRC}/dynamic.logistic.properties"
# ${SRC}/process.sh ${ROOT} ${SRC}/dynamic.logistic.properties -DBUILD=${BUILD} -DNGram.approxState=false

# bestMaxUpdate=8
# bestMorrisBase=1.3
# bestGranularity=100

# echo "--------------------"
# echo "--------------------"
# echo "Test approximate dynamic model with liblinear"
# echo ""
# echo "${SRC}/process.sh ${ROOT} ${SRC}/dynamic.logistic.properties -DClassifierStreamProcessor.logFilename=${BUILD}/dynamic-results.logistic.approx.tsv -DNGram.approxState=true -DBUILD=${BUILD}  -DNGram.maxUpdate=${bestMaxUpdate} -DNGram.morrisBase=${bestMorrisBase} -DNGram.granularity=${bestGranularity} "
# ${SRC}/process.sh ${ROOT} ${SRC}/dynamic.logistic.properties -DClassifierStreamProcessor.logFilename=${BUILD}/dynamic-results.logistic.approx.tsv -DNGram.approxState=true -DBUILD=${BUILD}  -DNGram.maxUpdate=${bestMaxUpdate} -DNGram.morrisBase=${bestMorrisBase} -DNGram.granularity=${bestGranularity} 


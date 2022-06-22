#!/bin/bash

PROJECT=${1}
TIME_LIMIT=${2}
PATH_SELECTOR=${3}
SELECTOR_ALIAS=${4}
COVERAGE_PROCESSING=${5}


if [[ -n $COVERAGE_PROCESSING ]]; then
    COVERAGE_PROCESSING="true eval/report/${PROJECT}/${SELECTOR_ALIAS}"
    mkdir -p "eval/report/${PROJECT}/${SELECTOR_ALIAS}"
fi

WORKDIR="."
$WORKDIR/scripts/run_contest_estimator.sh $PROJECT $TIME_LIMIT "$PATH_SELECTOR" "" "$COVERAGE_PROCESSING"

./gradlew :utbot-junit-contest:test :utbot-junit-contest:jacocoTestReport

OUTPUT_FOLDER=eval/jacoco/$PROJECT/$SELECTOR_ALIAS
rm -rf $OUTPUT_FOLDER
mkdir -p $OUTPUT_FOLDER
mv utbot-junit-contest/build/reports/jacoco/test/html/* "$OUTPUT_FOLDER"/

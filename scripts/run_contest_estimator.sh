#!/bin/bash

PROJECT=${1}
TIME_LIMIT=${2}
PATH_SELECTOR=${3}

items=(${PATH_SELECTOR//##/ })
PATH_SELECTOR_TYPE=${items[0]}
PATH_SELECTOR_PATH=${items[1]}
PREDICTOR_TYPE=${items[2]}
IS_COMBINED_SELECTOR=${items[3]}
ITERATIONS=${items[4]}

echo "PATH_SELECTOR=$PATH_SELECTOR"
echo "PATH_SELECTOR_TYPE=$PATH_SELECTOR_TYPE"
echo "PATH_SELECTOR_PATH=$PATH_SELECTOR_PATH"

FEATURE_ARG=${4}
featureItems=(${FEATURE_ARG//##/ })
FEATURE_PROCESSING=${featureItems[0]}
FEATURE_PATH=${featureItems[1]}

echo "FEATURE_ARG=$FEATURE_ARG"
echo "FEATURE_PATH=$FEATURE_PATH"

COVERAGE_ARG=${5}
coverageItems=(${COVERAGE_ARG//##/ })
COVERAGE_PROCESSING=${coverageItems[0]}
COVERAGE_PATH=${coverageItems[1]}

WORKDIR="."
INPUT_FOLDER=contest_input

# We set UtSettings by properties file
SETTING_PROPERTIES_FILE="$WORKDIR/settings.properties"
touch $SETTING_PROPERTIES_FILE
echo "pathSelectorType=$PATH_SELECTOR_TYPE" > "$SETTING_PROPERTIES_FILE"

if [[ -n $PATH_SELECTOR_PATH ]]; then
  echo "rewardModelPath=$PATH_SELECTOR_PATH" >> "$SETTING_PROPERTIES_FILE"
fi

if [[ -n $IS_COMBINED_SELECTOR ]]; then
  echo "singleSelector=false" >> "$SETTING_PROPERTIES_FILE"
fi

if [[ -n $ITERATIONS ]]; then
  echo "iterations=$ITERATIONS" >> "$SETTING_PROPERTIES_FILE"
fi

if [[ -n $PREDICTOR_TYPE ]]; then
  echo "nnStateRewardPredictorType=$PREDICTOR_TYPE" >> "$SETTING_PROPERTIES_FILE"
fi

if [[ -n $FEATURE_PROCESSING ]]; then
  echo "enableFeatureProcess=true" >> "$SETTING_PROPERTIES_FILE"
  if [[ -z $FEATURE_PATH ]]; then
    FEATURE_PATH=eval/features/$PATH_SELECTOR_TYPE/$PROJECT
  fi
  echo "featurePath=$FEATURE_PATH" >> "$SETTING_PROPERTIES_FILE"
fi

if [[ -n $COVERAGE_PROCESSING ]]; then
  echo "collectCoverage=true" >> "$SETTING_PROPERTIES_FILE"
  echo "coverageStatisticsDir=$COVERAGE_PATH" >> "$SETTING_PROPERTIES_FILE"
fi


# Clean resources folder, because if there is more than one project, than there is may be error during jacoco report
RESOURCES_FOLDER="utbot-junit-contest/src/main/resources"
rm -rf $RESOURCES_FOLDER/classes/*
rm -rf $RESOURCES_FOLDER/projects/*
rm -rf $RESOURCES_FOLDER/evosuite

# Copy target project in resources folder
cp -rp $INPUT_FOLDER/classes/$PROJECT $RESOURCES_FOLDER/classes/$PROJECT
cp -rp $INPUT_FOLDER/projects/$PROJECT $RESOURCES_FOLDER/projects/$PROJECT

JAR_TYPE="utbot-junit-contest"
echo "JAR_TYPE: $JAR_TYPE"
LIBS_DIR="utbot-junit-contest/build/libs/"
UTBOT_JAR="$LIBS_DIR$(ls -l $LIBS_DIR | grep $JAR_TYPE | awk '{print $9}')"
MAIN_CLASS="org.utbot.contest.ContestEstimatorKt"
CLASSPATH=$RESOURCES_FOLDER/projects
echo "CLASS PATH: $CLASSPATH"
TARGET_CLASSES=$RESOURCES_FOLDER/classes
echo "TARGET CLASSES: $TARGET_CLASSES"
TIME_LIMIT_IN_SEC=$TIME_LIMIT
echo "TIME LIMIT IN SEC: $TIME_LIMIT_IN_SEC"
OUTPUT_DIR=$WORKDIR/utbot-junit-contest/build/output
echo "OUTPUT_DIR: $OUTPUT_DIR"
COMPILABLE_TESTS_TARGET_DIR=$OUTPUT_DIR/utbot-junit-contest/build/output
echo "COMPILABLE_TESTS_TARGET_DIR: $COMPILABLE_TESTS_TARGET_DIR"
JUNIT4_JAR="skip"
echo "JUNIT4_JAR: $JUNIT4_JAR"

#JVM Flags and Options
JVM_OPTS=""
JVM_OPTS=$JVM_OPTS" -Xms512m"
JVM_OPTS=$JVM_OPTS" -Xmx12288m"
JVM_OPTS=$JVM_OPTS" -XX:+UseG1GC"
JVM_OPTS=$JVM_OPTS" -verbose:gc"
JVM_OPTS=$JVM_OPTS" -XX:+PrintGCDetails"
JVM_OPTS=$JVM_OPTS" -XX:+PrintGCTimeStamps"
JVM_OPTS=$JVM_OPTS" -Xloggc:$WORKDIR/run_contest_gc.log"

JVM_OPTS=$JVM_OPTS" -Dutbot.settings.path=$SETTING_PROPERTIES_FILE"

#Custom TMP directory - usually for server, change it by yours
#JVM_OPTS=$JVM_OPTS" -Djava.io.tmpdir=/home/wx1143086/tmp"

echo "JVM_OPTS: $JVM_OPTS"

#Creating output directory
mkdir -p $OUTPUT_DIR/test
rm -rf $OUTPUT_DIR/test/*

echo "new directory is supposed to be created at %OUTPUT_DIR/test%"

#Running the jar
COMMAND_LINE="java $JVM_OPTS -cp $UTBOT_JAR $MAIN_CLASS $TARGET_CLASSES $CLASSPATH $TIME_LIMIT_IN_SEC $OUTPUT_DIR $COMPILABLE_TESTS_TARGET_DIR $JUNIT4_JAR"

set -o pipefail
echo "Command to run: $COMMAND_LINE  2>&1 | tee -a $WORKDIR/execution.out"
$COMMAND_LINE   2>&1 | tee -a $WORKDIR/execution.out
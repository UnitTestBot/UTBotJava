#!/bin/bash

PROJECT=${1}
SELECTORS=${2}
STMT_COVERAGE=${3}
WORKDIR="."

# We set QualityAnalysisConfig by properties file
SETTING_PROPERTIES_FILE="$WORKDIR/utbot-analytics/src/main/resources/config.properties"
touch $SETTING_PROPERTIES_FILE
echo "project=$PROJECT" > "$SETTING_PROPERTIES_FILE"
echo "selectors=$SELECTORS" >> "$SETTING_PROPERTIES_FILE"

JAR_TYPE="utbot-analytics"
echo "JAR_TYPE: $JAR_TYPE"
LIBS_DIR=utbot-analytics/build/libs/
UTBOT_JAR="$LIBS_DIR$(ls -l $LIBS_DIR | grep $JAR_TYPE | awk '{print $9}')"
echo $UTBOT_JAR
MAIN_CLASS="org.utbot.QualityAnalysisKt"

if [[ -n $STMT_COVERAGE ]]; then
    MAIN_CLASS="org.utbot.StmtCoverageReportKt"
fi



#Running the jar
COMMAND_LINE="java $JVM_OPTS -cp $UTBOT_JAR $MAIN_CLASS"

echo "COMMAND=$COMMAND"

$COMMAND_LINE

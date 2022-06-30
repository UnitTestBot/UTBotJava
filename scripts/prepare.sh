#!/bin/bash

./gradlew clean build -x test

INPUT_FOLDER=contest_input

# Copy resources folder in distinct folder to allow other scripts have specific project in contest resources folder
mkdir $INPUT_FOLDER
cp -r utbot-junit-contest/src/main/resources/* $INPUT_FOLDER
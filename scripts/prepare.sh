#!/bin/bash

./gradlew clean build -x test

INPUT_FOLDER=contest_input

mkdir $INPUT_FOLDER
cp -r utbot-junit-contest/src/main/resources/* $INPUT_FOLDER
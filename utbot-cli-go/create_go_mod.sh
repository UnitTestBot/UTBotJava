#!/bin/bash

WORKING_DIRECTORY=$1
cd $WORKING_DIRECTORY
go mod init simple
go get github.com/stretchr/testify/assert
cd /usr/src

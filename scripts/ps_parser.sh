#!/bin/bash

while read line; do
	#echo $line
	PID=$(echo $line | awk '{ print $1 }')
	RSS=$(echo $line | awk '{ print $2 * 1024 }')
	PID_EXECUTABLE=$(cat /proc/${PID}/stat | awk '{ print $2 }' | sed -n 's/^(\(.*\))$/\1/p' )
	DESCRIPTION=$(echo $line | grep -o "Gradle Test Executor [0-9]*")
	if [[ "${PID_EXECUTABLE=}" == "java" ]]; then
		echo "processe_memory_bytes{pid=\"${PID}\",pid_executable=\"${PID_EXECUTABLE}\",description=\"${DESCRIPTION}\"} ${RSS}"
	fi
done <<< $(ps -ax --no-headers --format=pid,rss,command --sort=-rss,pid)

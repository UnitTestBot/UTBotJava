#!/bin/bash

PUSHGATEWAY_HOSTNAME=${1}
PUSHGATEWAY_USER=${2}
PUSHGATEWAY_PASSWORD=${3}
PUSHGATEWAY_ADDITIONAL_PATH=/pushgateway

JMX_EXPORTER_PORT=12345
JMX_EXPORTER_CONFIG=/tmp/jmx-exporter.yml
JMX_EXPORTER_JAR=/tmp/jmx-exporter.jar
JMX_EXPORTER_URL=https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.17.0/jmx_prometheus_javaagent-0.17.0.jar

PROM_ADDITIONAL_LABELS=/service/github
SLEEP_TIME_SECONDS=15
VERSION_CADVISOR=v0.36.0
VERSION_CURL=7.84.0
VERSION_NODE_EXPORTER=v1.3.1
PORT_CADVISOR=9280
PORT_NODE_EXPORTER=9100

# container metrics
if ! netstat -tulpn | grep -q ${PORT_CADVISOR} ; then
  docker run -d --name cadvisor \
                --volume=/:/rootfs:ro \
                --volume=/var/run:/var/run:ro \
                --volume=/sys:/sys:ro \
                --volume=/var/lib/docker/:/var/lib/docker:ro \
                --volume=/dev/disk/:/dev/disk:ro \
                --publish=9280:8080 \
                --privileged \
                --device=/dev/kmsg \
                    gcr.io/cadvisor/cadvisor:${VERSION_CADVISOR}
  docker run -d --name curl-container \
                --net="host" \
                --entrypoint=/bin/sh \
                    curlimages/curl:${VERSION_CURL} \
                  "-c" "while true; do curl localhost:9280/metrics | grep -v 'id=\"\/\(system\|user\).slice' | sed -r 's/(^.*} .*) ([0-9]*)/\1/' | curl -u ${PUSHGATEWAY_USER}:${PUSHGATEWAY_PASSWORD} --data-binary @- https://${PUSHGATEWAY_HOSTNAME}${PUSHGATEWAY_ADDITIONAL_PATH}/metrics/job/pushgateway/instance/${GITHUB_RUN_ID}-${HOSTNAME}${PROM_ADDITIONAL_LABELS} ; sleep ${SLEEP_TIME_SECONDS}; done"
fi

# base linux system metrics
if ! netstat -tulpn | grep -q ${PORT_NODE_EXPORTER} ; then
  docker run -d --name node_exporter \
                --net="host" \
                --pid="host" \
                --volume="/:/host:ro,rslave" \
                    quay.io/prometheus/node-exporter:${VERSION_NODE_EXPORTER} \
                    --path.rootfs=/host
  docker run -d --name curl-node \
                --net="host" \
                --entrypoint=/bin/sh \
                    curlimages/curl:${VERSION_CURL} \
                  "-c" "while true; do curl localhost:9100/metrics | curl -u ${PUSHGATEWAY_USER}:${PUSHGATEWAY_PASSWORD} --data-binary @- https://${PUSHGATEWAY_HOSTNAME}${PUSHGATEWAY_ADDITIONAL_PATH}/metrics/job/pushgateway/instance/${GITHUB_RUN_ID}-${HOSTNAME}${PROM_ADDITIONAL_LABELS} ; sleep ${SLEEP_TIME_SECONDS}; done"
fi

# custom java processes memory metrics
chmod +x scripts/ps_parser.sh
while true; do
  ./scripts/ps_parser.sh | curl -u "${PUSHGATEWAY_USER}":"${PUSHGATEWAY_PASSWORD}" --data-binary @- "https://${PUSHGATEWAY_HOSTNAME}${PUSHGATEWAY_ADDITIONAL_PATH}/metrics/job/pushgateway/instance/${GITHUB_RUN_ID}-${HOSTNAME}${PROM_ADDITIONAL_LABELS}" 2>/dev/null
  sleep ${SLEEP_TIME_SECONDS}
done &

# jvm metrics
#
# to enable this part of monitoring you also need to pass -javaagent option to org.gradle.jvmargs of GRADLE_OPTS variable, for example:
#   GRADLE_OPTS: "-Dorg.gradle.jvmargs='-XX:MaxHeapSize=2048m -javaagent:/tmp/jmx-exporter.jar=12345:/tmp/jmx-exporter.yml -Dorg.gradle.daemon=false'"
curl ${JMX_EXPORTER_URL} -o ${JMX_EXPORTER_JAR}
chmod +x ${JMX_EXPORTER_JAR}
printf "rules:\n- pattern: \".*\"\n" > ${JMX_EXPORTER_CONFIG}
while true; do
  curl localhost:${JMX_EXPORTER_PORT} 2>/dev/null | curl -u "${PUSHGATEWAY_USER}":"${PUSHGATEWAY_PASSWORD}" --data-binary @- "https://${PUSHGATEWAY_HOSTNAME}${PUSHGATEWAY_ADDITIONAL_PATH}/metrics/job/pushgateway/instance/${GITHUB_RUN_ID}-${HOSTNAME}${PROM_ADDITIONAL_LABELS}" 2>/dev/null
  sleep ${SLEEP_TIME_SECONDS}
done &

#!/bin/bash

FILEBEAT_DIR=${1}
LOGSTASH_HOST=${2}

cat > ${FILEBEAT_DIR}/filebeat.yml <<EOF
filebeat.inputs:
- type: filestream
  id: ${GITHUB_RUN_ID}-${HOSTNAME}
  paths:
  - "$(find /home/runner/runners -type f -name "Worker*.log")"
  parsers:
  - multiline:
      type: pattern
      pattern: '^\['
      negate: true
      match: after
processors:
- add_fields:
    target: "@metadata"
    fields:
      service: github
      team: utbot
- add_fields:
    target: "github"
    fields:
      env.GITHUB_ACTIONS: "${GITHUB_ACTIONS}"
      env.GITHUB_ACTOR: "${GITHUB_ACTOR}"
      env.GITHUB_EVENT_NAME: "${GITHUB_EVENT_NAME}"
      env.GITHUB_JOB: "${GITHUB_JOB}"
      env.GITHUB_HEAD_REF: "${GITHUB_HEAD_REF}"
      env.GITHUB_REF: "${GITHUB_REF}"
      env.GITHUB_REF_NAME: "${GITHUB_REF_NAME}"
      env.GITHUB_REF_PROTECTED: "${GITHUB_REF_PROTECTED}"
      env.GITHUB_REF_TYPE: "${GITHUB_REF_TYPE}"
      env.GITHUB_REPOSITORY: "${GITHUB_REPOSITORY}"
      env.GITHUB_REPOSITORY_OWNER: "${GITHUB_REPOSITORY_OWNER}"
      env.GITHUB_RETENTION_DAYS: "${GITHUB_RETENTION_DAYS}"
      env.GITHUB_RUN_ATTEMPT: "${GITHUB_RUN_ATTEMPT}"
      env.GITHUB_RUN_ID: "${GITHUB_RUN_ID}"
      env.GITHUB_RUN_NUMBER: "${GITHUB_RUN_NUMBER}"
      env.GITHUB_SHA: "${GITHUB_SHA}"
      env.GITHUB_TRIGGERING_ACTOR: "${GITHUB_TRIGGERING_ACTOR}"
      env.GITHUB_WORKFLOW: "${GITHUB_WORKFLOW}"
      env.HOSTNAME: "${HOSTNAME}"
      env.RUNNER_NAME: "${RUNNER_NAME}"
output.logstash:
  hosts: ["${LOGSTASH_HOST}"]
  ssl.certificate_authorities: ["${FILEBEAT_DIR}/ca.crt"]
  ssl.certificate: "${FILEBEAT_DIR}/client.crt"
  ssl.key: "${FILEBEAT_DIR}/client.key"
EOF

docker run -d --rm --name filebeat_7.17.6 \
  -v "${FILEBEAT_DIR}:${FILEBEAT_DIR}" \
  -v "/home/runner/runners:/home/runner/runners" \
  docker.elastic.co/beats/filebeat:7.17.6 \
    filebeat -c ${FILEBEAT_DIR}/filebeat.yml -e -v

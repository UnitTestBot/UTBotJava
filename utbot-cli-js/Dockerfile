FROM azul/zulu-openjdk:17.0.7-17.42.19

ARG DEBIAN_FRONTEND=noninteractive

WORKDIR /usr/src/

RUN apt-get update \
    && apt-get install -y -q --no-install-recommends \
      curl \
    && curl -sL https://deb.nodesource.com/setup_18.x -o nodesource_setup.sh \
    && /bin/sh nodesource_setup.sh \
    && apt-get install -y -q --no-install-recommends \
      nodejs \
    && rm -rf /var/lib/apt/lists/*

# Install UTBot Javascript CLI

ARG ARTIFACT_PATH
COPY ${ARTIFACT_PATH} .

RUN UTBOT_JS_CLI_PATH="$(find /usr/src -type f -name 'utbot-cli*')" \
    && ln -s "${UTBOT_JS_CLI_PATH}" /usr/src/utbot-cli.jar \

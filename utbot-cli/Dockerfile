FROM azul/zulu-openjdk:11.0.15-11.56.19

ARG DEBIAN_FRONTEND=noninteractive

WORKDIR /usr/src/

RUN apt-get update \
    && apt-get install -y -q --no-install-recommends \
      wget \
      unzip \
    && rm -rf /var/lib/apt/lists/*

# Install Kotlin compiler

ENV KOTLIN_COMPILER_VERSION=1.8.0
ENV KOTLIN_HOME="/opt/kotlin/kotlinc"
ENV PATH="${KOTLIN_HOME}/bin:${PATH}"

RUN wget --no-verbose https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_COMPILER_VERSION}/kotlin-compiler-${KOTLIN_COMPILER_VERSION}.zip -O /tmp/${KOTLIN_COMPILER_VERSION}.zip \
    && unzip -q -d /opt/kotlin /tmp/${KOTLIN_COMPILER_VERSION}.zip

# Install UTBot Java CLI

ARG ARTIFACT_PATH
COPY ${ARTIFACT_PATH} .

RUN UTBOT_JAVA_CLI_PATH="$(find /usr/src -type f -name 'utbot-cli*')" \
    && ln -s "${UTBOT_JAVA_CLI_PATH}" /usr/src/utbot-cli.jar

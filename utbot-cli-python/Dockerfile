FROM azul/zulu-openjdk:17.0.7-17.42.19

ARG DEBIAN_FRONTEND=noninteractive

WORKDIR /usr/src/

RUN apt-get update \
    && apt-get install -y -q --no-install-recommends \
      curl \
      python3.9 \
      python3-distutils \
    && rm -rf /var/lib/apt/lists/* \
    && curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py \
    && python3 get-pip.py \
    && pip install -U \
      pytest

# Install UTBot Python CLI

ARG ARTIFACT_PATH
COPY ${ARTIFACT_PATH} .

RUN UTBOT_PYTHON_CLI_PATH="$(find /usr/src -type f -name 'utbot-cli*')" \
    && ln -s "${UTBOT_PYTHON_CLI_PATH}" /usr/src/utbot-cli.jar

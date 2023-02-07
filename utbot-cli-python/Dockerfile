FROM azul/zulu-openjdk:11.0.15-11.56.19

ARG DEBIAN_FRONTEND=noninteractive

WORKDIR /usr/src/

RUN apt-get update \
    && apt-get install -y -q --no-install-recommends \
      curl \
      python3.9 \
      python3.9-distutils \
    && rm -rf /var/lib/apt/lists/* \
    && curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py \
    && python3.9 get-pip.py \
    && pip install -U \
      pytest

# Install UTBot Python CLI

ARG UTBOT_PYTHON_CLI
COPY ${UTBOT_PYTHON_CLI} .

RUN UTBOT_PYTHON_CLI_PATH="$(find /usr/src -type f -name 'utbot-cli*')" \
    && ln -s "${UTBOT_PYTHON_CLI_PATH}" /usr/src/utbot-cli.jar

FROM azul/zulu-openjdk:17.0.7-17.42.19

ARG UTBOT_GO_CLI

WORKDIR /usr/src/

# Install UTBot Go CLI

ARG UTBOT_GO_CLI
ARG CREATE_GO_MODULE_SCRIPT

WORKDIR /usr/src/

COPY ${UTBOT_GO_CLI} .
COPY ${CREATE_GO_MODULE_SCRIPT} .

# Install Go
RUN apt-get update && \
	apt-get install -y wget && \
	wget  https://go.dev/dl/go1.19.linux-amd64.tar.gz && \
	tar -xvf go1.19.linux-amd64.tar.gz && \
	mv go /usr/local && \
	rm go1.19.linux-amd64.tar.gz && \
	apt-get clean

# Install gcc	
RUN apt-get update \
    && apt-get install -y build-essential \
        software-properties-common \
    && add-apt-repository -y ppa:ubuntu-toolchain-r/test \
    && apt-get install -y gcc-9 \
    && update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-9 60 \
    && update-alternatives --config gcc \
    && apt-get clean

ENV GOROOT /usr/local/go
ENV PATH $GOPATH/bin:$GOROOT/bin:$PATH

RUN UTBOT_GO_CLI_PATH="$(find /usr/src -type f -name 'utbot-cli*')" \
    && ln -s "${UTBOT_GO_CLI_PATH}" /usr/src/utbot-cli.jar

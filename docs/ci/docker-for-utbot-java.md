<!---
name: Docker for UTBot Java
route: /docs/java/ci/docker-for-utbot-java
parent: Documentation
menu: CI
description: Setting up docker for UTBot Java building/debugging
--->

# Reproducible environment

It's available to download docker image with the environment for UTBot. The environment is also used in the crucial CI scripts focused on building project and running tests.

The docker image pre-installed environment includes:
1. *Java 17* + *JDK* package
3. *Gradle 7.6.1*
3. *Kotlin compiler 1.8.0*

It's based on Ubuntu [SOME VERSION].

## How to install Docker

Using reproducible environment requires Docker installed.

The detailed information of how to install Docker can be found on the [official site](https://docs.docker.com/engine/install/).

## How to run tests in docker container

Do the following steps to run tests in docker container:

1. Pull docker image
```
docker pull unittestbot/java-env:java17-zulu-jdk-gradle7.6.1-kotlinc1.8.0
```
2. Run docker container
```bash
# -v <utbot-repository-root>:/usr/utbot-debug - mounts the host directory into the container directory
# -it - make the container look like a terminal connection session
# -w /usr/utbot-tests - sets up working directory inside the container
docker run -it -v <utbot-repository-root>:/usr/utbot-tests --name utbot-tests -w /usr/utbot-tests unittestbot/java-env:java17-zulu-jdk-gradle7.6.1-kotlinc1.8.0
```
3. Do whatever you want

* Build UTBot and run tests:
```
gradle clean build --no-daemon
```
* Build UTBot without running tests:
```
gradle clean build --no-daemon -x test
```
* Run tests for *utbot-framework* project *CustomerExamplesTest* class:
```
gradle :utbot-framework:test --no-daemon --tests "org.utbot.examples.collections.CustomerExamplesTest"
```
4. Exit container
```
exit
```

## How to debug UTBot in docker container

Do the following steps to debug UTBot in docker container:

1. Set up configuration for remote debug in IntelliJ IDEA

**Run/Debug Configurations** → **Add New Configuration** → Choose **Remote JVM Debug** → Set up **Configuration name** → **Ok**

2. Pull docker image
```
docker pull unittestbot/java-env:java17-zulu-jdk-gradle7.6.1-kotlinc1.8.0
```
3. Run docker container
```bash
# -v <utbot-repository-root>:/usr/utbot-debug - mounts the host directory into the container directory
# -it - make the container look like a terminal connection session
# -w /usr/utbot-tests - sets up working directory inside the container
# -p 5005:5005 - mounts the host port into the container port (debugging port)
docker run -it -p 5005:5005 -v <utbot-repository-root>:/usr/utbot-debug --name utbot-debug -w /usr/utbot-tests unittestbot/java-env:java17-zulu-jdk-gradle7.6.1-kotlinc1.8.0
```
4. Set up gradle options for remote debug:
```
export GRADLE_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```
5. Start building and running tests
```
gradle clean build --no-daemon
```
6. Attach in IntelliJ IDEA to the gradle process in the container

Set up **breakpoints** wherever you want → **Run** new **Configuration** in **Debug** mode

7. Exit container
```
exit
```

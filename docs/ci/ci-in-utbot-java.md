<!---
name: CI in UTBot Java
route: /docs/java/ci/ci-in-utbot-java
parent: Documentation
menu: CI
description: CI processes description
--->

# CI features

UTBot Java offers contributors bunch of workflows e.g., the workflow _building the project and running tests_, the workflow _archiving plugin and CLI_.

The main CI features in UTBot Java:
* reproducible environment
* available monitoring processes

## Reproducible environment

Depending on the resources where you are intended to build and test software environment will be different. The key goal is to provide the same environment on different resources. To do that we use Docker images with appropriate software, environment variables and OS settings.

Crucial CI workflows run in those docker containers thus you can reproduce the environment locally. The environment can be used for running tests or for debugging ([see detailed information](https://github.com/UnitTestBot/UTBotJava/wiki/docker-for-utbot-java)).

If you have any questions of where images are placed, how many they are, what software versions are used, visit [repository](https://github.com/UnitTestBot/infra-images) please (now is private, will be changed in the future), leave an issue with your questions or ask in DM.

## All stages Monitoring

Since the workflow has started you can check access to the metrics on our monitoring service (ask teammates for url). The server offers developers the following dashboards:

* **Node Exporter Full** - metrics of consuming the RAM, CPU, Network and other resources on the host
* **JVM dashboard** (don't forget to set job to `pushgateway`) - Java metrics
* **Test executor statistics*** - RAM consuming by Java processes
* **cAdvisor: container details*** - system resources consuming by certain container
* **cAdvisor: host summary*** - summarized system resources consuming by all containers

**Note:** * developed by UTBot team

When you open a dashboard you need to choose valid instance. GitHub runs **each job on separate runner** so instance ID (`HOSTNAME` env var) would be different. But all instances have **the same Run ID** (`GITHUB_RUN_ID` env var). Follow this steps:

1. Go to Actions and open your Run;
2. Expand job list and choose any job you need;
3. At the right you'll see a list of steps. You need step `Run monitoring`;
4. Find the string like:
```
Find your Prometheus metrics using label {instance="2911909439-7f83f93ff335"}
```
5. Copy value between double quotes and go to monitoring dashboard. Set `github` service and expand instance list, CTRL+F and paste copied value. Choose your instance

<img width="398" alt="image" src="https://user-images.githubusercontent.com/25527604/186348770-c4d88867-5656-4733-bf8a-84cf2c2a638c.png">

**Note:** label consists of two part - `${GITHUB_RUN_ID}-${HOSTNAME}`. Use only one part to find all jobs of your Run.

# Available workflows

| Workflow name  | What it's supposed to do | What it triggers on |
| --- | --- | --- |
| UTBot Java: build and run tests | Builds the project and runs tests for it  | **push** or **pull request** to the **main** branch |
| [M] UTBot Java: build and run tests  | Builds the project and runs tests for it | **manual** call or call from **another workflow** |
| [M] Run chosen tests | Runs a single test or tests in chosen package/class | **manual** call |
| Plugin and CLI: publish as archives | Archives plugin and CLI and stores them attached to the workflow run report | **push** to the **main** branch |
| [M] Plugin and CLI: publish as archives | Archives plugin and CLI and stores them attached to the workflow run report | **manual** call or call from **another workflow** |
| [M] Publish on GitHub Packages | Publishes artifacts such as _utbot-api_, _utbot-core_, _utbot-framework_, etc., on GitHub Packages | **manual** call |
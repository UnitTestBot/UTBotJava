# Monitoring Settings

## Configuration files

There are monitoring configuration files for each project in `project/<project name>/monitoring.properties`.

### Monitoring configure
The file `monitoring.properties` is passed as a java property `-Dutbot.monitoring.settings.path`. It configures `org.utbot.monitoring.MonitoringSettings` class.

#### Properties description:
- `project` is a name of project that will be run in monitoring.
- `classTimeoutSeconds` is a unit-test generation timeout for one class.
- `runTimeoutMinutes` is a timeout for one whole run of the project.
- `fuzzingRatios` is a list of numbers that configure the ratio of fuzzing time to total generation time.

## Which project can be run?

### Prerequisites

Firstly, you should read [this](../utbot-junit-contest/README.md) paper about available projects and how to extend them.

### How to add projects to monitoring

To add a project to monitoring you should create a folder with a project name and create a file `monitoring.properties` with needed configurations.

# Monitoring Settings

## Configuration files

There are some files that configure running of monitoring:
- `monitoring.properties`
- `utbot.monitoring.settings.properties`

### UTBot configure
The file `utbot.monitoring.settings.properties` is passed as a java property `-Dutbot.settings.path`. It configures `org.utbot.framework.UtSettings` class.

Note: some settings will rewrite in runtime. See `setOptions()` function of `org.utbot.contest` in `Contest.kt` file.

### Monitoring configure
The file `monitoring.properties` is passed as a java property `-Dutbot.monitoring.settings.path`. It configures `org.utbot.monitoring.MonitoringSettings` class.

#### Properties description:
- `projects` - a list of projects that will be run in monitoring. 
- `classTimeoutSeconds` - a unit-test generation timeout for one class.
- `runTimeoutMinutes` - a timeout for one whole run of all projects.
- `fuzzingRatios` - a list of numbers that configure the ratio of fuzzing time to total generation time.

## Which project can be run?

### Structure

All available projects are placed in `resources` folder of `utbot-junit-contest`. 
There are two folders:
- `projects` - consists of folders with projects' jar files.
- `classes` - consists of folders which are named as projects and contains `list` file with a list of fully qualified names of classes.

### How to add a new project?
You can add jar files to the `projects` folder described above, and also add `list` file with a list of classes that will be provided to generation.

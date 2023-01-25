# Contest estimator

Contest estimator runs UnitTestBot on the provided projects and returns the generation statistics such as instruction coverage.

There are two entry points:
- [ContestEstimator.kt][ep 1] is the main entry point. It runs UnitTestBot on the specified projects, calculates statistics for the target classes and projects, and outputs them to a console.
- [StatisticsMonitoring.kt][ep 2] is an additional entry point, which does the same as the previous one but can be configured from a file and dumps the resulting statistics to a file.
It is used to [monitor and chart][monitoring] statistics nightly.


[ep 1]: src/main/kotlin/org/utbot/contest/ContestEstimator.kt
[ep 2]: src/main/kotlin/org/utbot/monitoring/StatisticsMonitoring.kt
[monitoring]: ../docs/NightStatisticsMonitoring.md

## Key functions

| Function name | File name           | Description                                                                            |
|---------------|---------------------|----------------------------------------------------------------------------------------|
| runGeneration | Contest.kt          | Runs UnitTestBot and manages its work                                                  |
| runEstimator  | ContestEstimator.kt | Configures a project classpath, runs the main generation loop, and collects statistics |


## Projects

The projects are provided to Contest estimator in advance.

### Structure
All available projects are placed in the [resources][resources] folder, which contains:
- [projects][projects] consisting of the folders with the project JAR files in them.
- [classes][classes] consisting of the folders — each named after the project and containing the `list` file with the fully qualified class names.
It also may contain an `exceptions` file with the description of the expected exceptions, that utbot should find.  
Description is presented in the format: `<class fully qualified name>.<method name>: <expected exception fqn> <another fqn> ...`.
For example, see this [file](src/main/resources/classes/codeforces/exceptions).

### How to add a new project
You should add both the JAR files to the `projects` folder and the file with a list of classes to the `classes` folder.

[resources]: src/main/resources
[projects]: src/main/resources/projects
[classes]: src/main/resources/classes

## Statistics
Statistics are collected and memorized by the corresponding classes placed in [Statistics.kt][statistics].
Then [monitoring][ep 2] dumps them using auxiliary classes that are defined in [MonitoringReport.kt][report] — they describe the format of output data. 

[statistics]: src/main/kotlin/org/utbot/contest/Statistics.kt
[report]: src/main/kotlin/org/utbot/monitoring/MonitoringReport.kt

# Contest Estimator

The main responsibility of Contest estimator is running UTBot on prepared projects in advance and providing some statistics such as instruction coverage.

There are several entry points:
- [ContestEstimator.kt][ep 1] - the main entry point of Contest estimator, it runs UTBot on specified projects, calculates some statistics for target classes and projects and outputs them in a console.
- [StatisticsMonitoring.kt][ep 2] - an additional entry point of Contest estimator which does the same as the previous one, but can be configured from a file and dumps output statistics to a file.
It is used to [monitor and chart][monitoring] statistics every night.


[ep 1]: src/main/kotlin/org/utbot/contest/ContestEstimator.kt
[ep 2]: src/main/kotlin/org/utbot/monitoring/StatisticsMonitoring.kt
[monitoring]: ../docs/NightStatisticsMonitoring.md

## Key functions

| Function name | File name           | Description                                                                                               |
|---------------|---------------------|-----------------------------------------------------------------------------------------------------------|
| runGeneration | Contest.kt          | This function runs UTBot and manage it's work                                                             |
| runEstimator  | ContestEstimator.kt | This function configure a project's classpath and does main loop of generation with collection statistics |


## Projects

There are some prepared projects in advance.

### Structure
All available projects are placed in [resources][resources] folder.
There are two folders:
- [projects][projects] - consists of folders with projects' jar files.
- [classes][classes] - consists of folders which are named as projects and contains `list` file with a list of fully qualified names of classes.

### How to add a new project?
You can add jar files to the `projects` folder described above, and also add `list` file with a list of classes that will be provided to generation.

[resources]: src/main/resources
[projects]: src/main/resources/projects
[classes]: src/main/resources/classes

## Statistics
Now statistics are collected and memorised by some classes placed in [Statistics.kt][statistics]. 
Then [monitoring][ep 2] dumps them using auxiliary classes defined in [MonitoringReport.kt][report] which describe format of output data. 

[statistics]: src/main/kotlin/org/utbot/contest/Statistics.kt
[report]: src/main/kotlin/org/utbot/monitoring/MonitoringReport.kt

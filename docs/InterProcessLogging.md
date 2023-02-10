# Interprocess logging

This document described how logging is performed across all 3 different processes: IDEA, Engine and Instrumented.

## Architecture

All logging relies on log4j2.
When UtBot is used as IDEA plugin - [`utbot-intellij/log4j2.xml`](../utbot-intellij/src/main/resources/log4j2.xml)
is used as configuration file.
In other cases(`ContestEstimator`, `Gradle/Maven` tasks, `CLI`, tests) it searches for the first `log4j2.xml` in resources in classpath.

### IDE process
IDEA part of UtBot write logs to `idea.log`. 
Log configuration file is used as is, so if you want to configure logs in IDEA part - use it straight.
If you want to change log configuration in already built plugin -
use `Help > Diagnostic Tools > Debug Log Settings...` to change `log4j2.xml` configuration for plugin.


### UtBot plugin


UtBot plugin creates log directory `org.utbot.intellij.plugin.process.EngineProcessKt.engineProcessLogDirectory`
where places log files. 

### Engine process 
Things are a bit more complicated here. There are cases - Engine process started from IDEA, and Engine process started separately.

#### Engine process started from IDEA

As plugin does not support multiple generation processes at a time - logs from any Engine process are written to the same file.

Default log file directory is `%user_temp%/UtBot/rdEngineProcessLogs`.

[`utbot-intellij/log4j2.xml`](../utbot-intellij/src/main/resources/log4j2.xml) is copied in
UtBot temporary directory - `org.utbot.intellij.plugin.process.EngineProcessKt.engineProcessLogConfigurationsDirectory`,
and then provided to Engine process via following CLI switch:
   ```
   -Dlog4j2.configurationFile=%configuration_file%
   ```

where `%configuration_file%` will be either:
1. Modified copy of [`utbot-intellij/log4j2.xml`](../utbot-intellij/src/main/resources/log4j2.xml) at UtBot temp directory.

   More precisely, there are 2 appenders in configuration file:
    ```xml
        <Appenders>
          <Console name="IdeaAppender" .../>
          <RollingFile name="EngineProcessAppender" .../>
        </Appenders>
    ```
   By default `IdeaAppender` is used everywhere in file, which is used in IDEA plugin.

   When working as Engine process - temporary `log4j2.`xml would be created, in which
   substring `ref="IdeaAppender"` will be replaced with `ref="EngineProcessAppender"`,
   thus changing all appenders and log pattern, but preserving same categories and log levels for loggers.

   After that, logs will be written by `RollingFileAppender` in `utbot-engine-current.log` file with rolling over
   after file reaches 20MB size. Previous log files are named `utbot-engine-%i.log`. Log file with
   maximal index is the last one rolled. For example, `utbot-engine-1.log` is created earlier than `utbot-engine-10.log`.

   In IDEA log following lines are printed each time Engine process started:
   ```
   | UtBot - EngineProcess             | Engine process started with PID = 4172
   | UtBot - EngineProcess             | Engine process log directory - C:\Users\user_name\AppData\Local\Temp\UTBot\rdEngineProcessLogs
   | UtBot - EngineProcess             | Engine process log file - C:\Users\user_name\AppData\Local\Temp\UTBot\rdEngineProcessLogs\utbot-engine-current.log
   ```

2. Path from `UtSettings.engineProcessLogConfigFile`.

   This option allows to provide path to external Log4j2 configuration file instead of [`utbot-intellij/log4j2.xml`](../utbot-intellij/src/main/resources/log4j2.xml).
   At `~/.utbot/settings.properties` you can set path to custom configuration file,
   which would apply for engine process, for example:
    ```
    engineProcessLogConfigFile=C:\wrk\UTBotJava\engineProcessLog4j2.xml
    ```
   This allows you to configure logs for Engine process even for already built plugin,
   you need only to restart IDE.

#### Engine process started separately

This is the case for `ContestEstimator`, `Gradle/Maven` tasks, `CLI`, tests, etc.
Configuration is taken from `log4j2.xml` in resources from the first `log4j2.xml` in resources in classpath.

### Instrumented process

Instrumented process sends its logs to the parent Engine process.
Logs are sent via corresponding RD model: `org.utbot.rd.models.LoggerModel`.
See `org.utbot.instrumentation.rd.InstrumentedProcess.Companion.invoke` and 
`org.utbot.instrumentation.process.InstrumentedProcessMainKt.main`.

## RD logs
Rd has its own logging system, based on `com.jetbrains.rd.util.Logger` interface. It is convenient to use 
RD logging as default logging system in instrumented process because log4j2 classes in utbot would be confused
at concrete execution with log4j2 classes in tested project - we will have duplicated versions of log4j2 libs,
this would break instrumentation and coverage statistics.

You should always override default RD logging strategy as by default it writes to stdout/stderr - use `com.jetbrains.rd.util.Logger.Companion.set` method to provide custom 
`com.jetbrains.rd.util.ILoggerFactory`. Already created loggers will be automatically reinstantiated to obtain
new logger from provided factory. You can obtain logger via `com.jetbrains.rd.util.getLogger` function.
Check `EngineProcessMain` for RD logging example.

For available RD factories see `org.utbot.rd.loggers` package - it contains useful implemented factories, 
which log message in the same format as described in `utbot-intellij/src/main/resources/log4j2.xml`.

## Implementation details

### Additivity

Sometimes same log entry might be written to log multiple times. At log you will see something like:
```
13:55:41.204 | INFO  | AnalyticsConfigureUtil    | PathSelectorType: INHERITORS_SELECTOR
13:55:41.204 | INFO  | AnalyticsConfigureUtil    | PathSelectorType: INHERITORS_SELECTOR
```

This is because of loggers *additivity* - their full names defines tree structure, and events from children
are visible for parents. For example, following `log4j2.xml` configuration in IDEA will produce such problem:
```xml
...
<Loggers>
        <Logger name="org.utbot.intellij" level="info">
            <AppenderRef ref="IdeaAppender"/>
        </Logger>
        <Logger name="org.utbot" level="info">
            <AppenderRef ref="IdeaAppender"/>
        </Logger>
</Loggers>
...
```

This happens because `org.utbot` logger is parent for `org.utbot.intellij`, and all events from 
`org.utbot.intellij` are also transferred to parent. This is called `additivity`.

The solution is to manually add ```additivity="false"``` tag to all loggers:
```xml
...
<Loggers>
        <Logger name="org.utbot.intellij" level="info" additivity="false">
            <AppenderRef ref="IdeaAppender"/>
        </Logger>
        <Logger name="org.utbot" level="info" additivity="false">
            <AppenderRef ref="IdeaAppender"/>
        </Logger>
</Loggers>
...
```

Consider this problem when you manually configure log level and appender for logger. 

More information is available [here](https://logging.apache.org/log4j/2.x/manual/configuration.html#Additivity).

### Useful
See auxiliary methods to work with logs at `UtRdLogUtil.kt` and `Logging.kt`. 
If you want to trace how long execution took - use `org.utbot.common.LoggingKt.logMeasure`
method with corresponding log level scope. 

In the Engine process log entries from Instrumented process are logged by `org.utbot.instrumentation.rd.InstrumentedProcessKt.rdLogger`.

## How to use log4j2 loggers

See related document - [How to use loggers](../HowToUseLoggers.md).

## Miscellaneous

### Performance considerations

`Debug` level is preferred in the most cases for developing. `Info` is sufficient for release. 

`Trace` log level for RD loggers(for ex. if you specify `Trace` for all loggers, or as default level for root logger)
will enable logging all technical send/receive event from protocol,
causing ~50mb additional logs per generation and ***heavily*** polluting log. This might be useful 
when troubleshooting inter-process communication, but in all other cases prefer `Debug` level or 
specify `Trace` level per logger explicitly.

If your `Debug` level log message requires heavy string interpolation - wrap it in lambda, for example:
```kotlin
val someVeryBigDataStructure = VeryBigDataStructure()

logger.debug("data structure representation - $someVeryBigDataStructure") // <---- interpolation
```
In that code even though message uses `Debug` level, interpolation will always occur because
message is passed as a parameter, which are evaluated at call site. 
In case logger is configured to `Info` level or higher - this means message will be built, but not logged, 
resulting in unnecessary work, possibly causing performance issue.
Consider using lambdas:
```kotlin
// message will be created only if debug log level is available
logger.debug { "data structure representation - $someVeryBigDataStructure"}
```

Although now logs are sent from one process to another - performance penalties were not noticed. 
Additional performance can be achieved playing with `bufferedIO` and `immediateFlush` properties in `log4j2.xml`. 
For example, you can make following changes in `utbot-intellij`:
```xml
<RollingFile ... bufferedIO="true" immediateFlush="false" ... >
```

This will reduce number of IO operations and use log4j2 buffer more efficiently. The cost it that 
when process terminates - log4j2 terminates logging service before buffer is flushed, and 
you will lose last portion of logs. This might be undesired behaviour in tests and debugging, 
but probably acceptable in release.

### Docker and Gradle

To see logs in Gradle from console, Docker and CI - add following `build.gradle.kts`:
```kotlin
allprojects {
    tasks {
        withType<Test> {
            testLogging.showStandardStreams = true
            testLogging.showStackTraces = true
        }
    }
}
```

## Links

Related topics:
1. [Multiprocess architecture](RD%20for%20UnitTestBot.md)
2. [Inter process debugging](./contributing/InterProcessDebugging.md)

Log4j2:
2. [Architecture](https://logging.apache.org/log4j/2.x/manual/architecture.html) - overall log4j2 description.
2. [Layouts](https://logging.apache.org/log4j/2.x/manual/layouts.html) - how to format log messages. 
UtBot uses `Pattern layout` everywhere.
3. [Appenders](https://logging.apache.org/log4j/2.x/manual/appenders.html) - about different ways to store log entries, 
different storages for log entries and how to configure them. UtBot uses `Console`, `File` and `RollingFile` appenders.
4. [Configuration](https://logging.apache.org/log4j/2.x/manual/configuration.html) - what you can write in configuration file, 
precise algorithm which file is used and many other useful info. It is **highly advised** to read `Additivity` part.
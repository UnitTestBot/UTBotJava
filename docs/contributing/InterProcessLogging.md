# Interprocess logging

This document describes
how logging is implemented across the UnitTestBot Java [processes](https://github.com/UnitTestBot/UTBotJava/blob/main/docs/RD%20for%20UnitTestBot.md):
the IDE process, the Engine process, and the Instrumented process.

## Architecture

The UnitTestBot Java logging system relies on `log4j2` library.

For UnitTestBot Java used as an IntelliJ IDEA plugin, the configuration file for logging is [`utbot-intellij/log4j2.xml`](../../utbot-intellij/src/main/resources/log4j2.xml).

When used as Contest estimator or the Gradle/Maven plugins, via CLI or during the CI test runs,
UnitTestBot Java engine searches classpath for the first `log4j2.xml` in the `resources` directory.

### IDE process

The IDE process writes logging information to standard `idea.log` files and puts them into the default log directory.

To configure logs for the IDE process, use the log configuration file in a straightforward way.

To change the log configuration for the prebuilt plugin,
go to **Help** > **Diagnostic Tools** > **Debug Log Settings...** and configure `log4j2.xml`.

To store log data for the Engine process started from the IDE process, the UnitTestBot Java plugin creates a directory:
`org.utbot.intellij.plugin.process.EngineProcessKt.engineProcessLogDirectory`.

### Engine process

The Engine process can be started either from IntelliJ IDEA or separately — as a standalone engine.

#### Engine process started from IntelliJ IDEA

As the plugin does not support multiple generation processes,
the logs from the Engine process are written to the same file.

The default log file directory is `%user_temp%/UtBot/rdEngineProcessLogs`.

The [`utbot-intellij/log4j2.xml`](../../utbot-intellij/src/main/resources/log4j2.xml) file is copied to the
UnitTestBot Java temporary directory:
`org.utbot.intellij.plugin.process.EngineProcessKt.engineProcessLogConfigurationsDirectory`.
Then this file is provided to the Engine process via the following CLI switch:
   ```
   -Dlog4j2.configurationFile=%configuration_file%
   ```

Here, `%configuration_file%` can take one of two values:
1. A modified copy of [`utbot-intellij/log4j2.xml`](../../utbot-intellij/src/main/resources/log4j2.xml) file, which is stored in UnitTestBot Java temporary directory.

   More precisely, there are 2 appenders in the configuration file:
    ```xml
        <Appenders>
          <Console name="IdeaAppender" .../>
          <RollingFile name="EngineProcessAppender" .../>
        </Appenders>
    ```
   By default, `IdeaAppender` is used everywhere in a file for the IDE plugin.

   For the Engine process, a temporary `log4j2.xml` is created,
   where the `ref="IdeaAppender"` substring is replaced with `ref="EngineProcessAppender"`:
   this replacement changes all the appenders and the log pattern
   but keeps categories and log levels for the loggers the same.

   As soon as the file reaches 20 MB size, `RollingFileAppender` writes the logs to the `utbot-engine-current.log` file.
   The created log files are named `utbot-engine-%i.log`.
   A log file with the largest index is the latest one: `utbot-engine-1.log` has been created earlier than `utbot-engine-10.log`.

   Each time the Engine process starts, the following lines are printed into the IntelliJ IDEA log:
   ```
   | UtBot - EngineProcess             | Engine process started with PID = 4172
   | UtBot - EngineProcess             | Engine process log directory - C:\Users\user_name\AppData\Local\Temp\UTBot\rdEngineProcessLogs
   | UtBot - EngineProcess             | Engine process log file - C:\Users\user_name\AppData\Local\Temp\UTBot\rdEngineProcessLogs\utbot-engine-current.log
   ```

2. A path from `UtSettings.engineProcessLogConfigFile`.

   The option provides the external `log4j2` configuration file with the path instead of [`utbot-intellij/log4j2.xml`](../../utbot-intellij/src/main/resources/log4j2.xml).
   In the `~/.utbot/settings.properties` file, one can set this path to a custom configuration file applicable to the Engine process, for example:
    ```
    engineProcessLogConfigFile=C:\wrk\UTBotJava\engineProcessLog4j2.xml
    ```
   This allows you to configure logs for the Engine process even for the prebuilt plugin (you need to restart an IDE).

#### Engine process started separately

When used as Contest estimator or the Gradle/Maven plugins, via CLI or during the CI test runs,
UnitTestBot Java engine searches classpath for the first `log4j2.xml` in the `resources` directory
to get configuration information.

### Instrumented process

The Instrumented process sends the logs to its parent — to the Engine process.
Logs are sent via the corresponding Rd model: `org.utbot.rd.models.LoggerModel`.

See also `org.utbot.instrumentation.rd.InstrumentedProcess.Companion.invoke` and 
`org.utbot.instrumentation.process.InstrumentedProcessMainKt.main`.

## Rd logging system

Rd has the custom logging system based on `com.jetbrains.rd.util.Logger` interface.
It is convenient to set the Rd logging system as default for the Instrumented process:
during concrete execution,
the `log4j2` classes in UnitTestBot Java could be confused with the `log4j2` classes from the project under test.
Duplicated `log4j2` libraries can break instrumentation and coverage statistics.

You should always override the default Rd logging strategy, which writes log data to `stdout/stderr`.
Use `com.jetbrains.rd.util.Logger.Companion.set` method to provide custom 
`com.jetbrains.rd.util.ILoggerFactory`.
The created loggers will be automatically re-instantiated to obtain a new logger from the provided factory.
You can obtain a logger via the `com.jetbrains.rd.util.getLogger` function.
Check `EngineProcessMain` for Rd logging example.

For available Rd factories, see the `org.utbot.rd.loggers` package: it contains the implemented factories. 
The format of the log messages is the same as described in `utbot-intellij/src/main/resources/log4j2.xml`.

## Implementation details

### Additivity

An entry may appear in a log many times due to _additivity_. The resulting log may look like this:
```
13:55:41.204 | INFO  | AnalyticsConfigureUtil    | PathSelectorType: INHERITORS_SELECTOR
13:55:41.204 | INFO  | AnalyticsConfigureUtil    | PathSelectorType: INHERITORS_SELECTOR
```

The logger's full name constitutes a tree structure so that the logged events from a child are visible to a parent.

For example, the following `log4j2.xml` configuration in IntelliJ IDEA will produce such a problem:
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

This happens because the `org.utbot` logger is a parent to `org.utbot.intellij`, and all the events from 
`org.utbot.intellij` are also transferred to `org.utbot`.

To modify this behavior, add the `additivity="false"` tag to all loggers manually:
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

Consider this problem when you manually configure log level and appender for a logger. 

For more information,
refer to the [`log4j2` additivity](https://logging.apache.org/log4j/2.x/manual/configuration.html#Additivity) document.

### Logging: auxiliary methods

Find more logging-related methods at `UtRdLogUtil.kt` and `Logging.kt`.

To trace the execution duration,
use the `measureTime` method (see `Logging.kt`) with the corresponding log level scope. 

In the Engine process, the entries from the Instrumented process are logged by `org.utbot.instrumentation.rd.InstrumentedProcessKt.rdLogger`.

## Log levels and performance

For development, the `Debug` level is preferred in most cases.

The `Info` log level is sufficient for release.

In Rd, if you choose the `Trace` level for all loggers or set it as default for the root logger,
this enables logging for all technical _send/receive_ events from protocol.
It may cause ~50 MB of additional entries per generation to appear and _heavily_ pollutes the log. This might be useful 
for troubleshooting interprocess communication but in all other cases prefer the `Debug` level or 
specify the `Trace` level per logger explicitly.

For the `Debug` level, if a log message requires heavy string interpolation, wrap it in lambda, for example:
```kotlin
val someVeryBigDataStructure = VeryBigDataStructure()

logger.debug("data structure representation - $someVeryBigDataStructure") // <---- interpolation
```
Here, even for a message with the `Debug` level, interpolation will always occur because
the message is passed as a parameter, which is evaluated at call site. 
If the `Info` level (or higher) is set for a logger,
the message is built, but not logged, 
resulting in unnecessary work, possibly causing performance issues.

Consider using lambdas:
```kotlin
// message will be created only if debug log level is available
logger.debug { "data structure representation - $someVeryBigDataStructure"}
```

Here, although the logs are sent from one process to another, no performance penalties have been noticed.

To reach higher performance, try to use `bufferedIO` and `immediateFlush` properties in `log4j2.xml`. 
For example, you can make the following changes to the `log4j2.xml` file in `utbot-intellij`:
```xml
<RollingFile ... bufferedIO="true" immediateFlush="false" ... >
```

This will reduce a number of I/O operations and help to use `log4j2` buffer more efficiently.
This may also have a flip side: 
when the process terminates, `log4j2` terminates the logging service before the buffer is flushed, and 
you will lose the last portion of logs.
This behavior is undesirable for testing and debugging, 
but probably acceptable for release.

## Docker and Gradle

To see the logs in Gradle from console, Docker and CI, add the following `build.gradle.kts` file:
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

## Useful links

UnitTestBot Java documentation:
1. [Multiprocess architecture](../RD%20for%20UnitTestBot.md)
2. [Interprocess debugging](InterProcessDebugging.md)
3. [How to use loggers](../../HowToUseLoggers.md)

`log4j2` documentation:
1. [Architecture](https://logging.apache.org/log4j/2.x/manual/architecture.html) — an overall `log4j2` description.
2. [Layouts](https://logging.apache.org/log4j/2.x/manual/layouts.html) — how to format log messages.
   (UnitTestBot Java uses `Pattern layout` everywhere.)
3. [Appenders](https://logging.apache.org/log4j/2.x/manual/appenders.html) —
   a description of various ways to store log entries (and how to configure the storages).
   UnitTestBot Java uses the `Console`, `File` and `RollingFile` appenders.
4. [Configuration](https://logging.apache.org/log4j/2.x/manual/configuration.html) —
   how to use a configuration file, how to check the file, and other useful information.
   It is **highly advised** to read the `Additivity` part.
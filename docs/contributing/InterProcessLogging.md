# Logging of UnitTestBot Java

## Table of content
- [IDE and Engine processes logs](#ide-and-engine-processes-logs)
- [RD logs](#rd-logs)
- [Instrumented process logs](#instrumented-process-logs)
- [Useful & misc](#useful--misc)

## IDE and Engine processes logs

Logging for both IDE and engine processes are based on [`utbot-intellij/log4j2.xml`](../../utbot-intellij/src/main/resources/log4j2.xml).

### IDE process
Log configuration file is used as is, so if you want to configure logs in IDEA part - use it straight. 
If you want to change log configuration in already built plugin -
use `Help > Diagnostic Tools > Debug Log Settings...` to change `log4j2.xml` configuration for plugin.

### Engine process
Things are a bit more complicated here.
[`utbot-intellij/log4j2.xml`](../../utbot-intellij/src/main/resources/log4j2.xml) is copied in 
UtBot temporary directory - `org.utbot.common.FileUtilKt.getUtBotTempDirectory`, 
and then provided to JVM via following CLI switch: 
   ```
   -Dlog4j2.configurationFile=%configuration_file%
   ```

where `%configuration_file%` will be either:
1. Modified copy of [`utbot-intellij/log4j2.xml`](../../utbot-intellij/src/main/resources/log4j2.xml) at UtBot temp directory.
  
    More precisely, there are 2 appenders in configuration file:
    ```xml
        <Appenders>
          <Console name="IdeaAppender" target="SYSTEM_OUT">
              <PatternLayout pattern="%msg%n"/>
          </Console>
          <Console name="EngineProcessAppender" target="SYSTEM_OUT">
              <PatternLayout pattern="%d{HH:mm:ss.SSS} | %-5level | %-25c{1} | %msg%n"/>
          </Console>
      </Appenders>
    ```
    By default `IdeaAppender` is used everywhere in file.
    Idea catches plugin stdout log and wraps with own format, so in IDE log only `%msg` is logged.

    When working as engine process - temporary `log4j2.`xml would be created, in which
    substring `ref="IdeaAppender"` will be replaced with `ref="EngineProcessAppender"`, 
    thus changing all appenders and log pattern, but preserving same categories and log level. 

2. Path from `UtSettings.engineProcessLogConfigFile`. 

    This option allows to provide path to external Log4j2 configuration file instead of [`utbot-intellij/log4j2.xml`](../../utbot-intellij/src/main/resources/log4j2.xml).
    At `~/.utbot/settings.properties` you can set path to custom configuration file,
    which would apply for engine process, for example:
    ```
    engineProcessLogConfigFile=C:\wrk\UTBotJava\engineProcessLog4j2.xml
    ```
    This allows you to configure logs even for already built plugin, 
    you need only to restart IDE.

## RD logs

RD has its own logging system with different interface for logging, 
see `com.jetbrains.rd.util.Logger`. 

Obtain logger via global function `getLogger()` from `rd-core.jar`.
By default, logger writes to `stderr` messages with `Warn` or higher log level,
and stdout for others.

You can set which logger you want RD to use via `com.jetbrains.rd.util.ILoggerFactory` interface. 
To set factory use `Logger.set(Lifetime, ILoggerFactory)` method, 
for example this code overrides RD logs with `KotlinLogging`:

```kotlin
Logger.set(object: ILoggerFactory {
    override fun getLogger(category: String): Logger {
        return KotlinLogging.logger(category)
    }
})
```

There are already 2 factories: 
1. `UtRdConsoleLoggeFactory` - allows to write stdin/stdout in a format compatible with IDEA `logj42` configuration.
2. `UtRdKLoggerFactory` - smart adapter from RD to KotlinLogger loggers.

### Details
Setup logger factory before any RD logs occurred, as you might lose some at RD start when loggers are configured to stdout.
The only way to configure RD logs - programmatically. There are no configuration files and/or services.

Rd logger dynamically reconfigures as new logger factories arrive, see `com.jetbrains.rd.util.SwitchLogger` for 
more details. 

Although RD produce ___A LOT OF LOGS___ - for 2-3 test generation logs 
file will contain ~800mb of text related to logs, nearly all RD logs has `Trace` log level. You `Trace` with care!

## Instrumented process logs

Instrumented process have different logging due to class mocking limitation:
in some cases we want to mock loggers, and for that we would need to mock static function like `getLogger` etc.
In that case if we use that logger in UtBot - we might get incorrect version which in fact is mock.

Instead, you should use hand-made logging based on RD as described in [RD logs section](#rd-logs). 

To configure instrumented process log level - use `UtSettings.instrumentedProcessLogLevel` property, 
for example add in settings.properties:

```kotlin
instrumentedProcessLogLevel=Debug
```

## Useful & misc

### Log4j2

Sometimes your log entries might duplicate when using log4j or similar. 
One of the reason might be *additivity*, read [here](https://logging.apache.org/log4j/2.x/manual/configuration.html#Additivity)
about how to solve it.

Also, log4j2 automatically reconfigures when detects changes in log file. Default check timeout - 30s.

### Output files for processes

In `idea.log` there will be a path to engine process log file:
```
Engine process log file - %path-to-engine-process-log%
```

And similarly in engine process log file will be entry:
```
Instrumented process log file: %path-to-instrumented-process-log%
```
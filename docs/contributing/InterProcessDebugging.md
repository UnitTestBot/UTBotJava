# Interprocess debugging of UnitTestBot Java

### Background

We have split the UnitTestBot machinery into three processes. See [doc about processes](../RD%20for%20UnitTestBot.md).
This approach has improved UnitTestBot capabilities, e.g. provided support for various JVMs and scenarios, but also complicated the debugging flow.

These are UnitTestBot processes (according to the execution order):

* IDE process
* Engine process
* Instrumented process

Usually, main problems happen in the Engine process, but it is not the process we run first.
The most straightforward way to debug the Engine process is the following.

### Enable Debugging

IDE debugging is pretty straightforward - start `runIde` task in `utbot-intellij` project from IDEA with debug.

For engine and instrumented processes you need to enable some options:
1. Open [`UtSettings.kt`](../../utbot-framework-api/src/main/kotlin/org/utbot/framework/UtSettings.kt)
2. There are 2 similar options: `runEngineProcessWithDebug` and `runInstrumentedProcessWithDebug`.
3. Enable for processes you need to debug. It can be done in 2 ways:
   * Can create `~/.utbot/settings.properties` file and write following:
   ```
   runEngineProcessWithDebug=true
   runInstrumentedProcessWithDebug=true
   ```
   After you will need to restart IDEA you want to debug.
   * ***Discouraged***: change in source file, but this will involve moderate project recompilation.
4. Additionally, you can set additional options for JDWP agent if debug is enabled:
   * `engineProcessDebugPort` and `instrumentedProcessDebugPort` - port for debugging. 
   Default values - 5005 for Engine and 5006 for Instrumented processes. 
   * `suspendEngineProcessExecutionInDebugMode` and `suspendInstrumentedProcessExecutionInDebugMode` - whether JDWP agent should
   suspend process until debugger is connected. 

   More formally, if debug is enabled following switch is added to engine process JVM at start by default:
   
   ```
   -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,quiet=y,address=5005"
   ```
   These options regulate values for parts `suspend` and `address`, for example with following in `~/.utbot/settings.properties`:
   ```
   runEngineProcessWithDebug=true
   engineProcessDebugPort=12345
   suspendEngineProcessExecutionInDebugMode=false
   ```
   result switch will be:
   ```
   -agentlib:jdwp=transport=dt_socket,server=n,suspend=y,quiet=y,address=12345"
   ```
   See `org.utbot.intellij.plugin.process.EngineProcess.Companion.getDebugArgument`
5. For information about logs - see [this](InterProcessLogging.md). 

### Run configurations for debugging the Engine process

There are 3 basic run configurations:
1. `Run IDE` - run plugin in IDEA
2. `Utility configuration/Listen for Instrumented Process` - listen on 5006 port if instrumented process is available for debug
3. `Utility configuration/Listen for Engine Process` - listen on 5005 port if engine process is available for debug

On top of them, there are 3 compound run configurations for debugging:
1. `Debug Engine Process` and `Debug Instrumented Process` - combo for debug IDE and selected process
3. `Debug All` - debug all 3 processes.

For debug configurations to work you need to provide required properties in `~/.utbot/settings.properties`. 
If you either change port and/or suspend mode - do review utility configuration to change default values as well. 

### How to debug

Let's see through example of how to debug IDE to engine process communication.

1. In your current IntelliJ IDEA with source, use breakpoints to define where the program needs to be stopped. For example, set the breakpoints at `EngineProcess.generate`
   and somewhere in `watchdog.wrapActiveCall(generate)`.
2. Select `Debug Engine Process` configuration, add required parameters to `~/.utbot/settings.properties` and start debug.
3. Generate tests with UnitTestBot in the debug IDE instance.
4. The debug IDE instance will stop generation (if you have not changed the debug parameters). If you take no action, test generation will be cancelled by timeout.
5. When the Engine process started (build processes have finished, and the progress bar says: _"Generate tests: read 
   classes"_), there will be 
6. Wait for the program to be suspended upon reaching the first breakpoint in Engine proces.
7. If symbolic execution is not turned on - часть магии может нахуй не случиться

### Interprocess call mapping

Now you are standing on a breakpoint in the IDE process, for example, the process stopped on:

    `EngineProcess.generate()`

If you would go along execution, it reaches the next line (you are still in the IDE process):

    `engineModel.generate.startBlocking(params)`

It seems that the test generation itself should occur in the Engine process and there should be an entry point in the Engine process.
How can we find it? 

Standing on the breakpoint `engineModel.generate.startBlocking(params)`, you may right-click in IDE on `EngineProcessModel.generate` and **Go to Declaration or 
Usage**. This would navigate to the definition of `RdCall` (which is responsible for cross-process communication) in file `EngineProcesModel.Generated.kt`.

Now **Find Usages** for `EngineProcessModel.generate` and see the point where `RdCall` is passed to the next method:

    watchdog.wrapActiveCall(generate)

This is the point where `RdCall` is called in the Engine process.

Actually you could have skipped the previous step and used **Find Usages** right away, but it is useful to know where `RdCall` is defined.

If you are interested in the trailing lambda of `watchdog.wrapActiveCall(generate)`, set the breakpoint here.
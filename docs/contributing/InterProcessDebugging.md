# Interprocess debugging of UnitTestBot Java

### Background

We have split the UnitTestBot machinery into three processes. See the [document on UnitTestBot multiprocess
architecture](../RD%20for%20UnitTestBot.md).
This approach has improved UnitTestBot capabilities, e.g., provided support for various JVMs and scenarios but also
complicated the debugging flow.

These are UnitTestBot processes (according to the execution order):

* _IDE process_
* _Engine process_
* _Instrumented process_

Usually, the main problems happen in the _Engine process_, but it is not the process we run first.
See how to debug UnitTestBot processes effectively.

### Enable debugging

Debugging the _IDE process_ is pretty straightforward: start the debugger session (**Shift+F9**) for the `runIde`
Gradle task in `utbot-intellij` project from your IntelliJ IDEA.

To debug the _Engine process_ and the _Instrumented process_, you need to enable the debugging options:
1. Open [`UtSettings.kt`](../../utbot-framework-api/src/main/kotlin/org/utbot/framework/UtSettings.kt).
2. There are two similar options: `runEngineProcessWithDebug` and `runInstrumentedProcessWithDebug` — enable the
   relevant one(s). There are two ways to do this:
   * You can create the `~/.utbot/settings.properties` file and write the following:

   ```
   runEngineProcessWithDebug=true
   runInstrumentedProcessWithDebug=true
   ```
   Then restart the IntelliJ IDEA instance you want to debug.

   * **Discouraged**: you can change the options in the source file, but this will involve moderate project
     recompilation.
3. You can set additional options for the Java Debug Wire Protocol (JDWP) agent if debugging is enabled:
   * `engineProcessDebugPort` and `instrumentedProcessDebugPort` are the ports for debugging.

     Default values:
      - 5005 for the _Engine process_
      - 5006 for the _Instrumented process_

   * `suspendEngineProcessExecutionInDebugMode` and `suspendInstrumentedProcessExecutionInDebugMode` define whether
     the JDWP agent should suspend the process until the debugger is connected.

   More formally, if debugging is enabled, the following switch is added to the _Engine process_ JVM at the start by
   default:
   ```
   "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,quiet=y,address=5005"
   ```

   These options set `suspend` and `address` values. For example, with the following options in `~/.utbot/settings.properties`:
   ```
   runEngineProcessWithDebug=true
   engineProcessDebugPort=12345
   suspendEngineProcessExecutionInDebugMode=false
   ```
   the resulting switch will be:
   ```
   "-agentlib:jdwp=transport=dt_socket,server=n,suspend=n,quiet=y,address=12345"
   ```
   See `org.utbot.intellij.plugin.process.EngineProcess.Companion.debugArgument` for switch implementation.
4. For information about logs, refer to the [Interprocess logging](InterProcessLogging.md) guide.

### Run configurations for debugging the Engine process

There are three basic run configurations:
1. `Run IDE` configuration allows running the plugin in IntelliJ IDEA.
2. `Utility Configurations/Listen for Instrumented Process` configuration allows listening to port 5006 to check if 
   the _Instrumented process_ is available for debugging.
3. `Utility Configurations/Listen for Engine Process` configuration allows listening to port 5005 to check if the _Engine process_ is available for debugging.

On top of them, there are three compound run configurations for debugging:
1. `Debug Engine Process` and `Debug Instrumented Process` — a combination for debugging the _IDE process_ and
   the selected process.
3. `Debug All` — a combination for debugging all three processes.

To make debug configurations work properly, you need to set the required properties in `~/.utbot/settings.properties`. If you change the _port number_ and/or the _suspend mode_, do change these default values in the corresponding Utility Configuration.

### How to debug

Let's walk through an example illustrating how to debug the "_IDE process_ → _Engine process_" communication.

1. In your current IntelliJ IDEA with source code, use breakpoints to define where the program needs to be stopped. For example, set the breakpoints at `EngineProcess.generate` and somewhere in `watchdog.wrapActiveCall(generate)`.
2. Select the `Debug Engine Process` configuration, add the required parameters to `~/.utbot/settings.properties` and 
   start the debugger session. 
3. Generate tests with UnitTestBot in the debug IDE instance. Make sure symbolic execution is turned on, otherwise some processes do not even start. 
4. The debug IDE instance will stop generation (if you have not changed the debug parameters). If you take no action, test generation will be canceled by timeout. 
5. When the _Engine process_ has started (build processes have finished, and the progress bar says: _"Generate 
   tests: read classes"_), there will be another debug window — "Listen for Engine Process", — which automatically 
   connects and starts debugging. 
6. Wait for the program to be suspended upon reaching the first breakpoint in the _Engine process_.

### Interprocess call mapping

Now you are standing on a breakpoint in the _IDE process_, for example, the process stopped on:

    EngineProcess.generate()

If you go along the execution, it reaches the next line (you are still in the _IDE process_):

    engineModel.generate.startBlocking(params)

It seems that test generation itself should occur in the _Engine process_ and there should be an entry point in the _Engine process_.
How can we find it?

Standing on the breakpoint at `engineModel.generate.startBlocking(params)`, right-click on
`EngineProcessModel.generate` and **Go to** > **Declaration or Usages**. This navigates to the `RdCall` definition (which is
responsible for cross-process communication) in the `EngineProcesModel.Generated.kt` file.

Now **Find Usages** for `EngineProcessModel.generate` and see the point where `RdCall` is passed to the next method:

    watchdog.wrapActiveCall(generate)

This is the point where `RdCall` is called in the _Engine process_.

You could have skipped the previous step and used **Find Usages** right away, but it is useful to know
where `RdCall` is defined.

If you are interested in the trailing lambda of `watchdog.wrapActiveCall(generate)`, set the breakpoint here.
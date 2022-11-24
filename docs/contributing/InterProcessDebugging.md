# Interprocess debugging of UnitTestBot Java

### Background

We have split the UnitTestBot machinery into three processes. This approach has improved UnitTestBot capabilities, e.g. 
provided support for various JVMs and scenarios, but also complicated the debugging flow.

These are UnitTestBot processes (according to the execution order):

* IDE process
* Engine process
* Concrete execution process

Usually, main problems happen in the Engine process, but it is not the process we run first.
The most straightforward way to debug the Engine process is the following.

### Enable debugging for the Engine process

1. Open `org/utbot/framework/UtSettings.kt`.
2. Set `runIdeaProcessWithDebug` property to _true_. This enables `EngineProcess.debugArgument`.
3. Find `EngineProcess.debugArgument` at `org/utbot/intellij/plugin/process/EngineProcess` and check the parameters of the debug run:

    `"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,quiet=y,address=5005"`

* The `suspend` mode is enabled. Modify it in the case of some tricky timeouts in your scenario.
* The port that will be used for debugging (`address`) is set to `5005`. Modify it if the port is already in use on your system.

### Create a new run configuration for debugging the Engine process

In addition to the `runIde` Gradle task that is supposed to run a new IDE instance, we should create another run 
configuration.

1. In your IntelliJ IDEA go to **Ru**n > **Edit configurations…**.
2. In the **Run/Debug Configuration** dialog, click **`+`** on the toolbar.
3. In the **Run/Debug Configuration Templates** dialog that opens, select a **Remote JVM Debug** configuration type.
4. Check that **Port** has the same number as the `address` parameter from the `EngineProcess.debugArgument` mentioned above.
5. Give the new run configuration a meaningful name and save the run configuration.

### How to debug

1. In your current IntelliJ IDEA, use breakpoints to define where the program needs to be stopped. For example, set the breakpoints at

    `EngineProcess.createTestGenerator()`<br>
    `engineModel().createTestGenerator.startSuspending()`

2. Start the debugger session (**Shift+F9**) for the `runIde` Gradle task (wait for the debug IDE instance to open).
3. Generate tests with UnitTestBot in the debug IDE instance. Make sure symbolic execution is turned on.
4. The debug IDE instance will stop generation (if you have not changed the debug parameters). If you take no action, test generation will be cancelled by timeout.
5. When the Engine process started (build processes have finished, and the progress bar says: _"Generate tests: read 
   classes"_), start the debugger session (**Shift+F9**) for your newly created Remote JVM Debug run configuration.
6. Wait for the program to be suspended upon reaching the first breakpoint.

### Interprocess call mapping

Now you are standing on a breakpoint in the IDE process, for example, the process stopped on:

    `EngineProcess.createTestGenerator()`

If you resume the process it reaches the next breakpoint (you are still in the IDE process):

    `engineModel().createTestGenerator.startSuspending()`

It seems that the test generation itself should occur in the Engine process and there should be an outbound point of the IDE process. How can we find it? An how can we reach the inbound point of the Engine process?

Standing on the breakpoint` engineModel().createTestGenerator.startSuspending()`, you may **Go to Declaration or 
Usage** and navigate to the definition of `RdCall` (which is responsible for cross-process communication) in `EngineProcessModel.createTestGenerator`.

Now **Find Usages** for `EngineProcessModel.createTestGenerator` and see the point where `RdCall` is passed to the next method:

    synchronizer.measureExecutionForTermination()

This is the point where `RdCall` is called in the Engine process.

Actually you could have skipped the previous step and used **Find Usages** right away, but it is useful to know where `RdCall` is defined.

If you are interested in the trailing lambda of `synchronizer.measureExecutionForTermination()`, set the breakpoint here.

#### Architectural notice

We must place the outbound point of the IDE process and the inbound point of the Engine process as close as possible. 
They may be two lambda-parameters of the same function. In this case we hope that the developer will not spend time on straying around.


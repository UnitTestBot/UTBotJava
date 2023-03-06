# Multiprocess architecture

## Overview

UnitTestBot consists of three processes: 
1. `IDE process` — the process where the plugin part executes. We also call it the _plugin process_ or the
   _IntelliJ IDEA process_.
2. `Engine process` — the process where the test generation engine executes.
3. `Instrumented process` — the process where concrete execution takes place.

These processes are built on top of the [Reactive distributed communication framework (Rd)](https://github.com/JetBrains/rd) developed by JetBrains. Rd plays a crucial role in UnitTestBot machinery, so we briefly 
describe this library here.

To gain an insight into Rd, one should grasp these Rd concepts:
1. Lifetime
2. Rd entities
3. `Rdgen`

## Lifetime concept

Imagine an object holding resources that should be released upon the object's death. In Java, 
`Closeable` and `AutoCloseable` interfaces are introduced to help release resources that the object is holding. 
Support for `try`-with-resources in Java and `Closeable.use` in Kotlin are also implemented to assure that the 
resources are closed after the execution of the given block.

Though, releasing resources upon the object's death is still problematic:
1. An object's lifetime can be more complicated than the scope of `Closeable.use`.
2. If `Closeable` is a function parameter, should we close it?
3. Multithreading and concurrency may lead to more complex situations.
4. Considering all these issues, how should we correctly close the objects that depend on some other object's 
   lifetime? How can we perform this task in a fast and easy way?
 
So, Rd introduces the concept of `Lifetime`.

### `Lifetime`

_Note:_ the described relationships and behavior refer to the JVM-related part of Rd.

`Lifetime` is an abstract class, with `LifetimeDefinition` as its inheritor. `LifetimeDefinition` 
has only one difference from its parent: `LifetimeDefinition` can be terminated.
Each `Lifetime` variable is an instance of `LifetimeDefinition` (we later call it "`Lifetime` instance"). You 
can register callbacks in this `Lifetime` instance — all of them will be executed upon the termination.

Though all `Lifetime` objects are instances of `LifetimeDefinition`, there are conventions for using them:
1. Do not cast `Lifetime` to `LifetimeDefinion` unless you are the one who created `LifetimeDefinition`.
2. If you introduce `LifetimeDefinition` somewhere, you should attach it to another `Lifetime` or provide 
   the code that terminates it.

A `Lifetime` instance has these useful methods:
- `onTermination` executes _lambda_/_closeable_ when the `Lifetime` instance is terminated. If an instance has been 
  already terminated, it executes _lambda_/_closeable_ instantly. Termination proceeds on a thread that has invoked 
  `LifetimeDefinition.terminate`. Callbacks are executed in the **reversed order**, which is _LIFO_: the last added 
  callback is executed first.
- `onTerminationIfAlive` is the same as `onTermination`, but the callback is executed only if the `Lifetime` 
  instance is `Alive`.
- `executeIfAlive` executes _lambda_ if the `Lifetime` instance is `Alive`. This method guarantees that the `Lifetime` 
  instance is alive (i.e. will not be terminated) during the whole time of _lambda_ execution.
- `createdNested` creates the _child_ `LifetimeDefinition` instance: it can be terminated if the _parent_ 
  instance is terminated as well; or it can be terminated separately, while the parent instance stays alive.
- `usingNested` is the same as the `createNested` method but behaves like the `Closeable.use` pattern.

See also:
- `Lifetime.Eternal` is a global `Lifetime` instance that is never terminated.
- `Lifetime.Terminated` is a global `Lifetime` instance that has been already terminated.
- `status` — find more details in the 
[LifetimeStatus.kt](https://github.com/JetBrains/rd/blob/9b806ccc770515f6288c778581c54607420c16a7/rd-kt/rd-core/src/main/kotlin/com/jetbrains/rd/util/lifetime/LifetimeStatus.kt) class from the Rd repository. There are three 
  convenient methods: `IsAlive`, `IsNotAlive`, `IsTerminated`.

### `LifetimeDefinition`

`LifetimeDefinition` instances have the `terminate` method that terminates a `Lifetime` instance and invokes all 
the registered callbacks. If multiple concurrent terminations occur, the method may sometimes return before 
executing all the callbacks because some other thread executes them.

## Rd entities

Rd is a lightweight reactive one-to-one RPC protocol, which is cross-language as well as cross-platform. It can 
work on the same or different machines via the Internet.

These are some Rd entities:
- `Protocol` encapsulates the logic of all Rd communications. All the entities should be bound to `Protocol` before 
  being used. `Protocol` contains `IScheduler`, which executes a _runnable_ instance on a different thread.
- `RdSignal` is an entity allowing one to **fire and forget**. You can add a callback for every received message 
  via the `advise(lifetime, callback)` method. There are two interfaces: `ISink` that only allows advising for 
  messages and `ISignal` that can also `fire` events.  There is also a `Signal` class with the same behavior 
  but without remote communication.

**Important:** if you `advise` and `fire` from the same process, your callback receives _not only_ 
messages from the other process, but also the ones you `fire`.

- `RdProperty` is a stateful property. You can get the current value and advise the callback — an advised 
  callback is executed on a current value and every change.
- `RdCall` is the remote procedure call.

There are `RdSet`, `RdMap`, and other entities.

An `async` property allows you to `fire` entities from any thread. Otherwise, you would need to do it from 
the `Protocol.scheduler` thread: all Rd entities should be bound to the `Protocol` from the `scheduler` thread, or you 
would get an exception.

## `Rdgen`

`Rdgen` generates custom classes and requests that can be bound to protocol and advised. There is a special model DSL 
for it.

### Model DSL

Examples:
1. [Korifey](https://github.com/korifey/rd_example/blob/main/src/main/kotlin/org/korifey/rd_example/model/Root.kt) — 
   a simple one.
2. [Rider Unity plugin](https://github.com/JetBrains/resharper-unity/tree/net223/rider/protocol/src/main/kotlin/model) — a complicated one.

First, you need to define a `Root` object: only one instance of each `Root` can be assigned to `Protocol`.

There is a `Root` extension — `Ext(YourRoot)` — where you can define custom types and model entities. You can assign 
multiple `Root` extensions to the `Protocol`. To generate the auxiliary structures, define them as direct fields.

DSL:
- `structdef` is a structure with fields that cannot be bound to `Protocol` but can be serialized. This structure 
  can be `openstruct`, i.e. open for inheritance, and `basestruct`, i.e. abstract. Only `field` can be a member.
- `classdef` is a class that can be bound to a model. It can have `property`, `signal`, `call`, etc.
  as members. It is possible to inherit: the class can be `openclass`, `baseclass`.
- `interfacedef` is provided to define interfaces. Use `method` to create a signature.

You can use `extends` and `implements` to implement inheritance.

_Note:_ `Rdgen` can generate models for C# and C++. Their structs and classes have different behavior.

Rd entities — only in bindable models (`Ext`, `classdef`):
- `property`
- `signal`
- `source`
- `sink`
- `array` and `immutablelist`

Useful properties in DSL entities:
- `async` — the same as `async` in Rd entities
- `docs` — provides KDoc/Javadoc documentation comments for the generated entity

### Gradle

[Example](https://github.com/korifey/rd_example/blob/main/build.gradle)

`RdGenExtension` configures `Rdgen`. The properties are:
- `sources` — the folders with DSL `.kt` files. If there are no `sources`, scan classpath for the inheritors of `Root` 
  and `Ext`.
- `hashfile` — a folder to store the `.rdgen` hash file for incremental generation.
- `packages` — Java package names to search in toplevels, delimited by `,`. Example: `com.jetbrains.rd.model.nova,com,
  org`.

Configure model generation with the `RdGenExtension.generator` method:
- `root` — for which root this generator is declared.
- `namespace` — which namespace should be used in the generated source. In Kotlin, it configures the generated package 
  name.
- `directory` — where to put the generated files.
- `transform` — can be `symmetric`, `asis`, and `reversed`. It allows configuring of different model interfaces for 
  various client-server scenarios. _Note:_ in 99% of cases you should use `symmetric`. If you need another option, consult with someone.
- `language` — can be `kotlin`, `cpp` or `csharp`.

## UnitTestBot project

The `utbot-rd` Gradle project contains model sources in `rdgenModels`. You can find them at 
[`utbot-rd/src/main/rdgen/org/utbot/rd/models`](../utbot-rd/src/main/rdgen/org/utbot/rd/models).

### IDE process

An _IDE process_ uses bundled JetBrains JDK. Code in `utbot-intellij` _**must**_ be compatible will all JDKs and plugin 
SDKs, used by our officially supported Intellij IDEA versions.
See `sinceBuild` and `untilBuild` in [`utbot-intellij/build.gradle.kts`](../utbot-intellij/build.gradle.kts). 

The _IDE process_ starts the _Engine process_. The _IDE process_ keeps the `UtSettings` instance in memory and gets updates for it from Intellij IDEA. The other processes "ask" the _IDE process_ about settings via Rd RPC.

### Engine process

`TestCaseGenerator` and `UtBotSymbolicEngine` run here, in the _Engine process_. The process classpath contains all 
the plugin JAR files (it uses the plugin classpath). 

The _Engine process_ _**must**_ run on the JDK that is used in the project under analysis. Otherwise, there will be 
numerous problems with code analysis, `soot`, _Reflection_, and the divergence of the generated code Java API will occur.

Currently, it is prohibited to run more than **one** generation process simultaneously (the limitation is related to 
the characteristics of the native libraries). The process logging mechanism relies on 
that fact, so UnitTestBot processes can exclusively write to a log file.

The starting point in the _IDE process_ is the 
[`EngineProcess`](../utbot-intellij/src/main/kotlin/org/utbot/intellij/plugin/process/EngineProcess.kt) class. 
The _Engine process_ start file is 
[`EngineProcessMain`](../utbot-framework/src/main/kotlin/org/utbot/framework/process/EngineProcessMain.kt).
The _Engine process_ starts the _Instrumented process_.  

### Instrumented process

The starting points in the _Engine process_ are the 
[`InstrumentedProcess`](../utbot-instrumentation/src/main/kotlin/org/utbot/instrumentation/rd/InstrumentedProcess.kt)
and the [`ConcreteExecutor`](../utbot-instrumentation/src/main/kotlin/org/utbot/instrumentation/ConcreteExecutor.kt) 
classes. The first one encapsulates the state, while the second one implements the request logic for concrete execution.

The _Instrumented process_ runs on the same JDK as the _Engine process_ to prevent deviation from the _Engine process_. 
Sometimes the _Instrumented process_ may unexpectedly die due to concrete execution.

### Useful info

1. If you need to use Rd, add the following dependencies:
    ```
    implementation group: 'com.jetbrains.rd', name: 'rd-framework', version: rdVersion
    
    implementation group: 'com.jetbrains.rd', name: 'rd-core', version: rdVersion
    ```
2. There are useful classes in `utbot-rd` to work with Rd and processes:
	- `LifetimedProcess` binds a `Lifetime` instance to a process. If the process dies, the `Lifetime` instance 
	  terminates, and vice versa. You can terminate the `Lifetime` instance manually — this will destroy the process.
	- `ProcessWithRdServer` starts the Rd server and waits for the connection. 
    - `ClientProtocolBuilder` — you can use it in a client process to correctly connect to `ProcessWithRdServer`.
3. How `ProcessWithRdServer` communication works:
	- Choose a free port.
	- Create a client process and pass the port as an argument.
	- Both processes create protocols, bind the model and setup callbacks.
	- A server process cannot send messages until the _child_ creates a protocol (otherwise, messages are lost), so 
	  the client process has to signal that it is ready.
	- The client process creates a special file in the `temp` directory, which is observed by a _parent_ process.
	- When the parent process spots the file, it deletes this file and sends a special message to the client process 
	  confirming communication success. 
	- Only when the answer of the client process reaches the server, the processes are ready.
4. How to write custom RPC commands:
	- Add a new `call` in a model, for example, in `EngineProcessModel`.
	- Re-generate models: there are special Gradle tasks for this in the `utbot-rd/build.gradle` file.
	- Add a callback for the new `call` in the corresponding start files, for example, in `EngineProcessMain.kt`.
	- **Important**: do not add [`Rdgen`](https://mvnrepository.com/artifact/com.jetbrains.rd/rd-gen) as 
	  an implementation dependency — it breaks some JAR files as it contains `kotlin-compiler-embeddable`.
5. Logging & debugging:
	- [Interprocess logging](contributing/InterProcessLogging.md)
    - [Interprocess debugging](./contributing/InterProcessDebugging.md)
6. Custom protocol marshaling types: do not spend time on it until `UtModels` get simpler, e.g. compatible with 
   `kotlinx.serialization`.


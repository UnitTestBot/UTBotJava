# Multi-process architecture

## Table of content
- [Overview](#overview)
- [Lifetimes](#lifetimes)
	- [Lifetime](#lifetime)
	- [LifetimeDefinition](#lifetimedefinition)
- [Rd](#rd)
- [Rdgen](#rdgen)
  - [Model DSL](#model-dsl)
  - [Gradle](#gradle)
- [UtBot project](#utbot-project)
  - [IDEA process](#idea-process)
  - [Engine process](#engine-process)
  - [Instrumented process](#instrumented-process)
  - [Commons](#useful)

## Overview
UtBot consists of 3 different processes: 
1. `IDEA process` - the one where plugin part executes. Also can be called `plugin process`, `IDE process`. 
2. `Engine process` - process where unit test generation engine executes.
3. `InstrumentedProces` - process where concrete execution takes place.

These processes are built on top of [JetBrains.RD](https://github.com/JetBrains/rd). It is crucial to understand 
this library, so it's better describing it first(as there are no documentation about it in repo;)).

RD is mostly about 3 components:

1. Lifetimes
2. Rd entities
3. Rdgen

Let's dive in each of them:

## Lifetimes

Imagine an object having some resources that should be freed
when object dies. For this purpose Java introduced interfaces
```Closeable\AutoCloseable```. Also, 2 helper functions were introduced: try-with-resources and Kotlin's ```Closeable.use```. There are several problems:
1. Object's lifetime can be more complicated that ```Closeable.use``` scope.
2. If function parameter is ```Closeable``` - should you close it?
3. Multiple closes.
4. Concurrent closes.
5. If you have several objects that depends on another object's lifetime - how to correctly close all of them with respect to issues 1-4? How to make it simple, convenient and fast?

And so Lifetime was introduced.
### Lifetime:
```Lifetime``` is a class, where you can register callbacks and which can be terminated once, thus executing all registered callbacks.

```Lifetime``` is an abstract class, it's inheritor - ```LifetimeDefinition```. The only difference - ```LifetimeDefinition``` can be terminated. Though all ```Lifetime``` are instances of ```LifetimeDefinition```, there are some conventions:
1. Do not cast ```Lifetime``` to ```LifetimeDefinion``` unless you are the one who created ```LifetimeDefinition```.
2. If you introduce somewhere ```LifetimeDefinition``` - either attach it to another ```Lifetime``` or provide code that terminates it.

Useful ```Lifetime``` methods:

- ```onTermination``` - Executes lambda/closeable when lifetime terminates. If already terminated - executes instantly. Termination will proceed on thread that called ```LifetimeDefinition.terminate()```. Callbacks will be executed in ***reversed order***, that is LIFO - last added callback will be executed first.
- ```onTerminationIfAlive``` - same as ```OnTermination```, but callback will not be executed if lifetime is not alive.
- ```executeIfAlive``` - executes lambda if lifetime is alive. This method guarantees that lifetime will not be terminated until lambda completes.
- ```createdNested``` - creates child LifetimeDefinition that will be terminated if parent does.
- ```usingNested``` - same as ```createNested```, but like ```Closeable.use``` pattern.
- ```Eternal``` - lifetime that never terminates.
- ```Terminated``` - lifetime that already terminated.
- ```status``` - see [LifetimeStatus.kt](https://github.com/JetBrains/rd/blob/9b806ccc770515f6288c778581c54607420c16a7/rd-kt/rd-core/src/main/kotlin/com/jetbrains/rd/util/lifetime/LifetimeStatus.kt) in RD repo. There are 3 convenient method: ```IsAlive, IsNotAlive, IsTerminated```.

### LifetimeDefinition:
- ```terminate``` - terminates ```Lifetime``` and calls all callbacks. Sometimes if multiple concurrent terminations occurred - method will return before executing all callbacks because some other thread is doing this.

## Rd
Rd is a cross-language, cross-platform, light-weight, reactive, one-to-one rpc protocol. Can work either on the same or different machines via internet.

Useful entities:
- ```Protocol``` - encapsulation of all logic regarding rd communication. All entities should be bound to protocol before using. Contains ```IScheduler``` which executes runnables on different thread.
- ```RdSignal``` - entity for ___fire and forget___. You can add callback for every received message via ```advise(lifetime, callback)``` method. There are 2 interfaces - ```ISink``` which allows only to advise for messages, and ```ISignal``` which can also ```fire``` events.  Also, there is just ```Signal``` class with same behaviour, but without remote communication.

  ___Important___: if you advise and fire from the same process - your callback will receive ___not only___ messages from another process, but also the ones you fire.
- ```RdProperty``` - stateful property. You can get current value, you can advise  - advised callback will be executed on current value and on every change.
- ```RdCall``` - remote procedure call.

Also there are ```RdSet```, ```RdMap``` and many other.

There is ```async``` property that allows you to ```fire``` entities from any thread. Otherwise you would need to do it from ```Protocol.scheduler``` thread. All rd entities should be at first

## Rdgen
Generates custom classes and requests which can be bound to protocol and advised. There is special model DSL for it.
### Model DSL
Eexample:
1. [Korifey](https://github.com/korifey/rd_example/blob/main/src/main/kotlin/org/korifey/rd_example/model/Root.kt) - quite simple
2. [Rider Unity plugin](https://github.com/JetBrains/resharper-unity/tree/net223/rider/protocol/src/main/kotlin/model) - complicated one

First you need to define ```Root``` object - only one instance of each root can be assigned to protocol.

Then there is root extension ```Ext(YourRoot)``` where you can define your own types and model entities. You can assign multiple extensions of root for the protocol. Auxillary structures can be defined as direct fields - they will be also generated.

DSL:
- ```structdef``` - structure with fields that cannot be bound to protocol, but can be serialized. Can be open for inheritace - ```openstruct```, can be abstract -  ```basestruct```. Can have only ```field``` as member.
- ```classdef``` - class that can be bould to model. Can also have ```property```, ```signal```, ```call``` e.t.c. as members. It is possible to do inheritance - ```openclass```, ```baseclass```.
- ```interfacedef``` - to define interfaces. Use ```method``` to create signature.

  You can use ```extends``` and ```implements``` to work with inheritance.

  ___N.B.___ - rdgen can generate models also for C# and C++. Their structs and classes have a little different behaviour.
- Rd entities - only in bindable models(```Ext```, ```classdef```):
	- ```property```
	- ```signal```
	- ```source```
	- ```sink```
	- ```array``` and ```immutablelist```
- Useful properties in dsl entities:
	- async - as ```async``` in RD entities
	- docs - provides kdoc/javadoc for generated entity
### Gradle
[Example](https://github.com/korifey/rd_example/blob/main/build.gradle)

```RdGenExtension``` configurates Rdgen, useful properties:
- ```sources``` - folders with dsl .kt files. If not present, scan classpath for inheritors of ```Root``` and ```Ext```.
- ```hashfile``` - folder to store hash file ```.rdgen``` for incremental generation.
- ```packages``` - java package names to search toplevels, delimited by ','. Example: ```com.jetbrains.rd.model.nova,com,org```.
- Configuring model generation with method ```RdGenExtension.generator```:
	- root - for which root you are declaring this generator
	- namespace - which namespace should be used in generated source. In kotlin it configures generated packaged name.
	- directory - where to put generated files.
	- transform - can be ```symmetric```, ```asis``` and ```reversed```. This allows to configure model interface differently for client-server scenarios.

	  P.S. This is legacy from distant past, in 99% of time you should use ```symmetric```. In 1% chance - consult with somebody if you really need another option.
	- language - can be ```kotlin```, ```cpp``` and ```csharp```.

## UtBot project

There is another gradle project ```utbot-rd``` which contains model sources in ```rdgenModels```.
Look at [```utbot-rd/src/main/rdgen/org/utbot/rd/models```](../utbot-rd/src/main/rdgen/org/utbot/rd/models)

### IDEA process
Uses bundled JetBrains JDK. Code in `utbot-intellij` ___must___ be compatible will all JDKs and plugin SDKs, which are used by our officially supported IntellijIDEA versions.
See [`utbot-intellij/build.gradle.kts`](../utbot-intellij/build.gradle.kts), parts `sinceBuild` and `untilBuild`. 

Starts `Engine process`. Maintains `UtSettings` instance in memory and updates it from IDEA. 
Other processes ask this process for settings via RD RPC.

### Engine process

`TestCaseGenerator` and `UtBotSymbolicEngine` runs here. Process classpath contains all plugin jars(more precisely - it uses plugin classpath). 

___Must___ run on JDK, which uses project we analyze. Otherwise there will be numerous problems with code analysis, soot, reflection and 
devirgention of generated code Java API. 

Currently, it is prohibited to run more than 1 generation process simultaneously(something with native libs). 
However, logging for processes relies on that fact, so they can exclusively write to log file.

IDEA starting point - class [`EngineProcess`](../utbot-intellij/src/main/kotlin/org/utbot/intellij/plugin/process/EngineProcess.kt). 
Process start file - [`EngineProcessMain`](../utbot-framework/src/main/kotlin/org/utbot/framework/process/EngineProcessMain.kt).
Starts `Instrumented process`.  

### Instrumented process

Start points at `Engine process`: classes [`InstrumentedProcess`](../utbot-instrumentation/src/main/kotlin/org/utbot/instrumentation/rd/InstrumentedProcess.kt) and [`ConcreteExecutor`](../utbot-instrumentation/src/main/kotlin/org/utbot/instrumentation/ConcreteExecutor.kt). 
First one is state encapsulation, second is used to implement request logic for concrete execution.

Runs on the same JDK as `Engine process` to erase deviation from `Engine process`. 
Sometimes might unexpectedly die due concrete execution.


### Useful

1. if you need to use rd somewhere - add following dependencies:
    ```
    implementation group: 'com.jetbrains.rd', name: 'rd-framework', version: rdVersion
    
    implementation group: 'com.jetbrains.rd', name: 'rd-core', version: rdVersion
    ```
2. There are some useful classes in `utbot-rd` to work with processes & rd:
	- ```LifetimedProcess``` - binds ```Lifetime``` to process. If process dies - lifetime terminates and vice versa. You can terminate lifetime manually - this will destroy process.
	- ```ProcessWithRdServer``` - also starts Rd server and waits for connection. 
    - `ClientProtocolBuilder` - use in client process to correctly connect to `ProcessWithRdServer`.
3. How ```ProcessWithRdServer``` communication works:
	- Choose free port
	- Create client process, pass port as argument
	- Both processes create protocols, bind model and setup callbacks
	- Server process cannot send messages before child creates protocol, otherwise messages will be lost. So client process needs to signal that he is ready.
	- Client process creates special file in temp dir, that is observed by parent process.
	- When parent process spots file - he deletes it, and then sends special message for client process confirming communication succeed. 
	- Only after client process answer reaches server - then processes are ready.
4. How to write custom RPC commands
	- Add new ```call``` in some model, for example in ```EngineProcessModel```.
	- Regenerate models: there are special gradle tasks for it in `utbot-rd/build.gradle` file.
	- Add callback for new ```call``` in corresponding start files, for example in `EngineProcessMain.kt`.
	- ___Important___ - do not add [`Rdgen`](https://mvnrepository.com/artifact/com.jetbrains.rd/rd-gen) as implementation dependency, it breaks some `.jar`s as it contains `kotlin-compiler-embeddable`.
5. Logs & Debug
	- Logs - [inter process logging](./contributing/InterProcessLogging.md)
   	- Debug - [inter process debugging](./contributing/InterProcessDebugging.md)
6. Custom protocol marshalling types
   Do not spend time on it until UtModels would get simpler, for example Kotlinx.serialization compatible.


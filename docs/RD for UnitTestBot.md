# RD
New child process communication involves 3 different things:
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

```Lifetime``` is an abstract class, it's inheritor - ```LifetimeDefinition```. The only difference - only ```LifetimeDefinition``` can be terminated. Though all ```Lifetime``` are instances of ```LifetimeDefinition```, there are some conventions:
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

There is another gradle project ```utbot-rd``` which contains model sources in ```rdgenModels``` sources. Look for ```org.utbot.rd.models.ProtocolRoot```.

Usefull:
1. if you need to use rd somewhere - add following dependencies:
    ```
    implementation group: 'com.jetbrains.rd', name: 'rd-framework', version: 'actual.version'
    
    implementation group: 'com.jetbrains.rd', name: 'rd-core', version: 'actual.version'
    ```
2. There are some usefull classes to work with processes & rd:
	- ```LifetimedProcess``` - binds ```Lifetime``` to process. If process dies - lifetime terminates and vice versa. You can terminate lifetime manually - this will destroy process.
	- ```ProcessWithRdServer``` - also starts Rd server and waits for connection.
	- ```UtInstrumentationProcess``` - encapsulates logic for preparing child process for executing arbitary commands. Exposes ```protocolModel``` for communicating with child process.
	- ```ConcreteExecutor``` is convenient wrapper for executing commands and managing resources.
3. How child communication works:
	- Choosing free port
	- Creating child process, passing port as argument
	- Both processes create protocols and bind model
	- Child process setups all callbacks
	- Parent process cannot send messages before child creates protocol, otherwise messages will be lost. So child process needs to signal that he is ready.
	- Child proces creates special file in temp dir, that is observed by parent process.
	- When parent process spots file - he deletes it, and then sends special message for preparing child proccess instrumentation
	- Only then process is ready for executing commands
4. How to write custom commands for child process
	- Add new ```call``` in ```ProtocolModel```
	- Regenerate models
	- Add callback for new ```call``` in ```ChildProcess.kt```
	- Use ```ConcreteExecutor.withProcess``` method
	- ___Important___ - do not add `Rdgen` as implementation dependency, it breaks some `.jar`s as it contains `kotlin-compiler-embeddable`.
5. Logs

   There is ```UtRdLogger``` where you can configure level via ```log4j2.xml```.

6. Custom protocol marshalling types

   Do not spend time on it until:
	- Cyclic dependencies removed from UtModels
	- Kotlinx.serialization is used


# Android Studio support

## Installing AS

> Install latest AS <https://developer.android.com/studio/install>

### Installing Lombok plugin

> Use the first advice from the following link
>
> <https://stackoverflow.com/questions/70900954/cannot-find-lombok-plugin-in-android-studio-2021-1-1-build-of-2022>

## Prerequisites

> Install and setup gradle version 7.2+ (version 7.4 tested)
>
> Use JDK 11 for Gradle in\
> `File -> Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JVM`
>
> \
> If you want to use JDK 8, you can:
> 1. Generate tests with JDK 8
> 2. Switch to JDK 11 and compile tests
> 3. Switch back to JDK 8 and run tests
> 
> The reason for it is the Android Gradle Plugin, which requires Java 11 to build anything.

## Running in AS

> For now, running Utbot is supported only for Kotlin libraries. You can
> create one like this:
>
> <https://proandroiddev.com/create-an-android-library-aar-79d2338678ba>
> 
> To run generated tests, you must create separate JUnit configuration.\
> ("Green arrows" will not work, since they launch Android Emulator.)
>
## Debug Intellij code

> At first, when entering Intellij code, you might be presented with
> decompiled code.
>
> If you try to debug it, you might soon find out that it does not match
> sources.
>
> (TO BE TESTED)
>
> To fix this, you are able to connect alternative Intellij sources
> using jar-files from AS.
>
> File -> Project Structure -> Libraries -> +
>
> After that, you might want to enable "Choose sources switch":
>
> <https://intellij-support.jetbrains.com/hc/en-us/community/posts/206822215-what-does-Choose-Sources-do-and-how-can-I-undo-what-it-does->

## Crucial differences from Intellij IDEA

### Host Android SDK

> Android Studio uses **host Android SDK** to build project, which is
> basically **a stub(mock) version of real Android SDK**, that is
> supposed to be found on a real device.
>
> It means that, for instance, the constructor of java.lang.Object in
> that SDK throws Exception explicitly, saying "Stub!".
>
> The main idea is that user is not supposed to run anything on host
> machine, they must use real device or emulator.
>
> That leads to the **inability to analyze Android SDK**, thus we have
> to take real java from Gradle JDK, for example.

### KtClass tree in View-based modules

> UtBot Plugin window won't even show up if you try to analyze code from
> the visual components inside AS. That is because insead of PsiClass
> tree we find KtClass tree.
>
> TODO: There is something to be done about this...

## Troubleshooting

### Maven can't install some dependencies
> 1. Proxy might have been installed automatically
     > Solution: remove gradle-wrapper file
> 2. Mockito can't be found
     > Solution: specify version explicitly
### No target device found
> File -> Settings -> Build, Execution, Deployment -> Testing -> Run android tests with Gradle(or smth like that)

### Android Gradle plugin requires Java 11 to run. You are currently using Java 1.8

> Solution: Use JUNIT (manually create run config for it), not Gradle!
> 
> Also, turn off 'build before run' for tests, if you use Java 11 in your project.
> 
> TODO: how to create config
>
> (!) In Generation Window, you have to set correct source root(src/test/java).

### Test events were not received

> Solution: Use JUNIT (manually create run config for it), not Gradle!
> 
> TODO: how to create config
>
> (!) In Generation Window, you have to set correct source root(src/test/java).

### Can't run tests because Android project complains about 'tools.jar'

> The project is messed up, copy sources and make a clean one.
>
> TODO: a better solution?
>
### java: Cannot run program AppData/Local/Android/Sdk/bin/java

> The project is messed up, copy sources and make a clean one.
>
> TODO: a better solution?

### \[possible\] org.jetbrains.android not found

> Use latest Kotlin in UTBotJava/utbot-intellij/build.gradle:
>
> > intellij.plugins = [..., 'org.jetbrains.kotlin',...]

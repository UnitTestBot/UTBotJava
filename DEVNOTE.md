# UnitTestBot developer guide

---

When you have the forked repository on your local machine, you are almost ready to build your own version of UnitTestBot.

💡 Before you start coding, please check the [system requirements](https://github.com/UnitTestBot/UTBotJava/wiki/Check-system-requirements) and find instructions on
configuring development environment.

💡 Get to know the [code style](https://github.com/saveourtool/diktat/blob/master/info/guide/diktat-coding-convention.md) we are used to.

## How to build UnitTestBot with your improvements

The project structure is mostly straightforward and the modules are self-explanatory, e.g.:

* ```utbot-framework``` — everything related to UnitTestBot engine (including tests);
* ```utbot-intellij``` — IDE plugin implementation;
* ```utbot-sample``` — a framework with examples to demonstrate engine capacity.

Learn UnitTestBot from inside and implement changes. To verify your improvements open Gradle tool window in IntelliJ IDEA:

* to _run/debug plugin_ in IntelliJ IDEA choose and run the task: **utbot → utbot-intellij → intellij → runIde**;
* to _compile plugin_ choose and run the task: **utbot → utbot-intellij → intellij → buildPlugin**. The resulting ZIP 
  file is located at ```utbot-intellij/build/distributions```.
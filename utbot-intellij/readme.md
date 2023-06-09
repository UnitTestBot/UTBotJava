UnitTestBot Intellij Plugin
===========================

To run/debug plugin in IDEA:

* run IDEA in sandbox: `gradle runIde`
* open your test project in IDEA
* configure JDK, wait while indexing finishes
* use plugin actions in proper menu

To compile plugin: 
* run `gradle buildPlugin`
* find zipped plugin in build/distributions

## UnitTestBot Intellij Plugin UI Tests

* Comment `exclude("/org/utbot/**")` in utbot-intellij/build.gradle.kts
* run IDEA in sandbox with IntelliJ Robot server plugin installed: `gradle runIdeForUiTests`
* run **All** the tests in utbot-intellij/src/test/kotlin/org/utbot/tests

Note: projects are created first and only on new projects tests are executed. 
That is done for independency of each autotest run.



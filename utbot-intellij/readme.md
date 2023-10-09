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

* comment `exclude("/org/utbot/**")` in utbot-intellij/build.gradle.kts
* correct DEFAULT_PROJECT_DIRECTORY in RunInfo.kt if needed (it is your local directory in which test projects will be created locally)
* run IDEA in sandbox with IntelliJ Robot server plugin installed: `gradle runIdeForUiTests`
* wait till debug IDEA is started
* check it is above other windows and maximized
* check keyboard language is EN
* do NOT lock screen
* run **All** the tests in utbot-intellij/src/test/kotlin/org/utbot/tests

Note: projects are created first and only on new projects tests are executed. 
That is done for independency of each autotest run.



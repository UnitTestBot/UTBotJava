## Utbot gradle plugin

Utbot Gradle Plugin is a gradle plugin for generating tests and creating SARIF-reports.

The `generateTestsAndSarifReport` gradle task generates tests and SARIF-reports for all classes in your project (or only for classes specified in the configuration).
In addition, it creates one big SARIF-report containing the results from all the processed files.   


### How to use

Please, check for the available versions [here](https://plugins.gradle.org/plugin/org.utbot.gradle.plugin). 

- Apply the plugin:

  __Groovy:__
  ```Groovy
  plugins {
      id "org.utbot.gradle.plugin" version "..."
  }
  ```
  __Kotlin DSL:__
  ```Kotlin
  plugins {
      id("org.utbot.gradle.plugin") version "..."
  }
  ```

- Run gradle task `utbot/generateTestsAndSarifReport` to create a report.


### How to configure

For example, the following configuration may be used:

__Groovy:__
  ```Groovy
  sarifReport {
      targetClasses = ['com.abc.Main', 'com.qwerty.Util']
      projectRoot = 'C:/.../SomeDirectory'
      generatedTestsRelativeRoot = 'build/generated/test'
      sarifReportsRelativeRoot = 'build/generated/sarif'
      markGeneratedTestsDirectoryAsTestSourcesRoot = true
      testPrivateMethods = false
      projectType = 'purejvm'
      testFramework = 'junit5'
      mockFramework = 'mockito'
      generationTimeout = 60000L
      codegenLanguage = 'java'
      mockStrategy = 'other-packages'
      staticsMocking = 'mock-statics'
      forceStaticMocking = 'force'
      classesToMockAlways = ['org.slf4j.Logger', 'java.util.Random']
  }
  ```
__Kotlin DSL:__
  ```Kotlin
  configure<SarifGradleExtension> {
      targetClasses.set(listOf("com.abc.Main", "com.qwerty.Util"))
      projectRoot.set("C:/.../SomeDirectory")
      generatedTestsRelativeRoot.set("build/generated/test")
      sarifReportsRelativeRoot.set("build/generated/sarif")
      markGeneratedTestsDirectoryAsTestSourcesRoot.set(true)
      testPrivateMethods.set(false)
      projectType.set("purejvm")
      testFramework.set("junit5")
      mockFramework.set("mockito")
      generationTimeout.set(60000L)
      codegenLanguage.set("java")
      mockStrategy.set("other-packages")
      staticsMocking.set("mock-statics")
      forceStaticMocking.set("force")
      classesToMockAlways.set(listOf("org.slf4j.Logger", "java.util.Random"))
  }
  ```

Also, you can configure the task using `-P<parameterName>=<value>` syntax.

For example, 
```Kotlin
generateTestsAndSarifReport
    -PtargetClasses='[com.abc.Main, com.qwerty.Util]'
    -PprojectRoot='C:/.../SomeDirectory'
    -PgeneratedTestsRelativeRoot='build/generated/test'
    -PsarifReportsRelativeRoot='build/generated/sarif'
    -PtestPrivateMethods='false'
    -PtestProjectType=purejvm
    -PtestFramework=junit5
    -PmockFramework=mockito
    -PgenerationTimeout=60000
    -PcodegenLanguage=java
    -PmockStrategy='other-packages'
    -PstaticsMocking='mock-statics'
    -PforceStaticMocking=force
    -PclassesToMockAlways='[org.slf4j.Logger, java.util.Random]'
```

**Note:** All configuration fields have default values, so there is no need to configure the plugin if you don't want to.

**Description of fields:**
- `targetClasses` &ndash; 
  - Classes for which the SARIF-report will be created.
    Uses all source classes from the user project if this list is empty.
  - By default, an empty list is used.

- `projectRoot` &ndash;
  - **Absolute** path to the root of the relative paths in the SARIF-report.
  - By default, the root of your project is used.

- `generatedTestsRelativeRoot` &ndash;
  - **Relative** path (against module root) to the root of the generated tests.
  - By default, `'build/generated/test'` is used.

- `sarifReportsRelativeRoot` &ndash;
  - **Relative** path (against module root) to the root of the SARIF reports.
  - By default, `'build/generated/sarif'` is used.

- `markGeneratedTestsDirectoryAsTestSourcesRoot` &ndash;
  - Mark the directory with generated tests as `test sources root` or not.
  - By default, `true` is used.

- `testPrivateMethods`&ndash;
  - Generate tests for private methods or not.
  - By default, `false` is used.

- `projectType` &ndash;
  - The type of project being analyzed.
  - Can be one of:
    - `'purejvm'` _(by default)_
    - `'spring'`
    - `'python'`
    - `'javascript'`

- `testFramework` &ndash;
  - The name of the test framework to be used.
  - Can be one of:
    - `'junit4'`
    - `'junit5'` _(by default)_
    - `'testng'`

- `mockFramework` &ndash;
  - The name of the mock framework to be used.
  - Can be one of:
    - `'mockito'` _(by default)_

- `generationTimeout` &ndash;
    - Time budget for generating tests for one class (in milliseconds).
    - By default, 60 seconds is used.

- `codegenLanguage` &ndash;
  - The language of the generated tests.
  - Can be one of: 
    - `'java'` _(by default)_
    - `'kotlin'`

- `mockStrategy` &ndash;
  - The mock strategy to be used.
  - Can be one of:
    - `'no-mocks'` &ndash; do not use mock frameworks at all
    - `'other-packages'` &ndash; mock all classes outside the current package except system ones _(by default)_
    - `'other-classes'` &ndash; mock all classes outside the class under test except system ones

- `staticsMocking` &ndash;
  - Use static methods mocking or not.
  - Can be one of:
    - `'do-not-mock-statics'`
    - `'mock-statics'` _(by default)_

- `forceStaticMocking` &ndash;
  - Forces mocking static methods and constructors for `classesToMockAlways` classes or not.
  - Can be one of:
    - `'force'` _(by default)_
    - `'do-not-force'`
  - **Note:** We force static mocking independently on this setting for some classes (e.g. `java.util.Random`).

- `classesToMockAlways` &ndash;
  - Classes to force mocking theirs static methods and constructors.
  - By default, some internal classes are used.


### How to test

If you want to change the source code of the plugin or even the whole utbot-project,
you need to do the following:

- Publish plugin to the local maven repository:  
  `utbot-gradle/publishing/publishToMavenLocal`

- Add to your build file:

  __Groovy:__
  ```Groovy
  buildscript {
      repositories {
          mavenLocal()
      }
      dependencies {
          classpath "org.utbot:utbot-gradle:1.0-SNAPSHOT"
      }
  }
  ```
  __Kotlin DSL:__
  ```Kotlin
  buildscript {
      repositories {
          mavenLocal()
      }
      dependencies {
          classpath("org.utbot:utbot-gradle:1.0-SNAPSHOT")
      }
  }
  ```

- Apply the plugin:  

  __Groovy:__
  ```Groovy
  apply plugin: 'org.utbot.gradle.plugin'
  ```
  __Kotlin DSL:__
  ```Kotlin
  apply(plugin = "org.utbot.gradle.plugin")
  ```

### How to configure the log level

To change the log level run the `generateTestsAndSarifReport` task with the appropriate flag.

For example, `generateTestsAndSarifReport --debug`

Note that the internal gradle log information will also be shown.

Also note that the standard way to configure the log level (using the `log4j2.xml`) does not work from gradle.

[Read more about gradle log levels](https://docs.gradle.org/current/userguide/logging.html)

### Publishing

1. Read the [documentation](https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html) about plugin publishing
2. Sign in to our [account](https://plugins.gradle.org/u/utbot) to get API keys (if you don't have a password, please contact [Nikita Stroganov](https://github.com/IdeaSeeker))
3. Run `utbot-gradle/plugin portal/publishPlugins` gradle task

You can check the published artifacts in the [remote repository](https://plugins.gradle.org/m2/org/utbot/utbot-gradle/).

Please note that the maximum archive size for publishing on the Gradle Plugin Portal is ~60Mb.

### Requirements

UTBot gradle plugin requires Gradle 7.4.2+

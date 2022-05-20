## Utbot gradle plugin

Utbot Gradle Plugin is a gradle plugin for generating tests and creating SARIF-reports.

The `createSarifReport` gradle task generates tests and SARIF-reports for all classes in your project (or only for classes specified in the configuration).
In addition, it creates one big SARIF-report containing the results from all the processed files.   


### How to use

- Add our nexus repository to your build file:
  
  <details>
  <summary>Groovy</summary>
  <pre>
  buildscript {
      repositories {
          maven {
              url "http://[your-ip]:[your-port]/repository/utbot-uber/"
              allowInsecureProtocol true
          }
      }
  }
  </pre>
  </details>
  
  <details>
  <summary>Kotlin DSL</summary>
  <pre>
  buildscript {
      repositories {
          maven {
              url = uri("http://[your-ip]:[your-port]/repository/utbot-uber/")
              isAllowInsecureProtocol = true
          }
      }
  }
  </pre>
  </details>

- Then apply the plugin:
  
  <details>
  <summary>Groovy</summary>
  <pre>
  apply plugin: 'org.utbot.sarif'
  </pre>
  </details>
  
  <details>
  <summary>Kotlin DSL</summary>
  <pre>
  apply(plugin = "org.utbot.sarif")
  </pre>
  </details>

- Run gradle task `utbot/createSarifReport` to create a report.


### How to configure

For example, the following configuration may be used:

<details>
<summary>Groovy</summary>
<pre>
sarifReport {
    targetClasses = ['com.abc.Main', 'com.qwerty.Util']
    projectRoot = 'C:/.../SomeDirectory'
    generatedTestsRelativeRoot = 'build/generated/test'
    sarifReportsRelativeRoot = 'build/generated/sarif'
    markGeneratedTestsDirectoryAsTestSourcesRoot = true
    generationTimeout = 60000L
    testFramework = 'junit5'
    mockFramework = 'mockito'
    codegenLanguage = 'java'
    mockStrategy = 'package-based'
    staticsMocking = 'mock-statics'
    forceStaticMocking = 'force'
    classesToMockAlways = ['org.slf4j.Logger', 'java.util.Random']
}
</pre>
</details>


<details>
<summary>Kotlin DSL</summary>
<pre>
configure&lt;SarifGradleExtension&gt; {
    targetClasses.set(listOf("com.abc.Main", "com.qwerty.Util"))
    projectRoot.set("C:/.../SomeDirectory")
    generatedTestsRelativeRoot.set("build/generated/test")
    sarifReportsRelativeRoot.set("build/generated/sarif")
    markGeneratedTestsDirectoryAsTestSourcesRoot.set(true)
    generationTimeout.set(60000L)
    testFramework.set("junit5")
    mockFramework.set("mockito")
    codegenLanguage.set("java")
    mockStrategy.set("package-based")
    staticsMocking.set("mock-statics")
    forceStaticMocking.set("force")
    classesToMockAlways.set(listOf("org.slf4j.Logger", "java.util.Random"))
}
</pre>
</details>

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

- `generationTimeout` &ndash;
  - Time budget for generating tests for one class (in milliseconds).
  - By default, 60 seconds is used.

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

- `codegenLanguage` &ndash;
  - The language of the generated tests.
  - Can be one of: 
    - `'java'` _(by default)_
    - `'kotlin'`

- `mockStrategy` &ndash;
  - The mock strategy to be used.
  - Can be one of:
    - `'do-not-mock'` &ndash; do not use mock frameworks at all
    - `'package-based'` &ndash; mock all classes outside the current package except system ones _(by default)_
    - `'all-except-cut'` &ndash; mock all classes outside the class under test except system ones

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
- Publish the modified project to the local maven repository
- Correctly specify the dependencies in the build file (in your project)

There are two ways to do it.

- **The first way**
    - Run `publishing/publishToMavenLocal` (**utbot root** gradle task)
  
    - Add to your build file:
      
      <details>
      <summary>Groovy</summary>
      <pre>
      buildscript {
          repositories {
              mavenLocal()
              maven {
                  url "http://[your-ip]:[your-port]/repository/utbot-uber/"
                  allowInsecureProtocol true
              }
              mavenCentral()
          }
      &nbsp;
          dependencies {
              classpath group: 'org.utbot', name: 'utbot-gradle', version: '1.0-SNAPSHOT'
          }
      }
      </pre>
      </details>
      
      <details>
      <summary>Kotlin DSL</summary>
      <pre>
      buildscript {
          repositories {
              mavenLocal()
              maven {
                  url = uri("http://[your-ip]:[your-port]/repository/utbot-uber/")
                  isAllowInsecureProtocol = true
              }
              mavenCentral()
          }
      &nbsp;
          dependencies {
              classpath("org.utbot:utbot-gradle:1.0-SNAPSHOT")
          }
      }
      </pre>
      </details>

- **The second way** (faster, but more difficult)
    - Run `publishing/publishToMavenLocal` (**utbot-gradle** gradle task)
    - Add to your `build.gradle`:

      <details>
      <summary>Groovy</summary>
      <pre>
      buildscript {
          repositories {
              mavenLocal()
              maven {
                  url "http://[your-ip]:[your-port]/repository/utbot-uber/"
                  allowInsecureProtocol true
              }
              mavenCentral()
          }
      &nbsp;
          dependencies {
              classpath group: 'org.utbot', name: 'utbot-gradle', version: '1.0-SNAPSHOT'
              classpath files('C:/..[your-path]../UTBotJava/utbot-framework/build/libs/utbot-framework-1.0-SNAPSHOT.jar')
              classpath files('C:/..[your-path]../UTBotJava/utbot-framework-api/build/libs/utbot-framework-api-1.0-SNAPSHOT.jar')
              classpath files('C:/..[your-path]../UTBotJava/utbot-instrumentation/build/libs/utbot-instrumentation-1.0-SNAPSHOT.jar')
          }
      }
      </pre>
      </details>
      
      <details>
      <summary>Kotlin DSL</summary>
      <pre>
      buildscript {
          repositories {
              mavenLocal()
              maven {
                  url = uri("http://[your-ip]:[your-port]/repository/utbot-uber/")
                  isAllowInsecureProtocol = true
              }
              mavenCentral()
          }
      &nbsp;
          dependencies {
              classpath("org.utbot:utbot-gradle:1.0-SNAPSHOT")
              classpath(files("C:/..[your-path]../UTBotJava/utbot-framework/build/libs/utbot-framework-1.0-SNAPSHOT.jar"))
              classpath(files("C:/..[your-path]../UTBotJava/utbot-framework-api/build/libs/utbot-framework-api-1.0-SNAPSHOT.jar"))
              classpath(files("C:/..[your-path]../UTBotJava/utbot-instrumentation/build/libs/utbot-instrumentation-1.0-SNAPSHOT.jar"))
          }
      }
      </pre>
      </details>

### How to configure the log level

To change the log level run the `createSarifReport` task with the appropriate flag.

For example, `createSarifReport --debug`

Note that the internal gradle log information will also be shown.

Also note that the standard way to configure the log level (using the `log4j2.xml`) does not work from gradle.

[Read more about gradle log levels](https://docs.gradle.org/current/userguide/logging.html)
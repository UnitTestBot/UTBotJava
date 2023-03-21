## Utbot maven plugin

Utbot Maven Plugin is a maven plugin for generating tests and creating SARIF-reports.

The `generateTestsAndSarifReport` maven task generates tests and SARIF-reports for all classes in your project (or only for classes specified in the configuration).
In addition, it creates one big SARIF-report containing the results from all the processed files.


### How to use

_TODO: The plugin has not been published yet._

- Apply the plugin:
  ```XML
  <build>
      <plugins>
          <plugin>
              <groupId>org.utbot</groupId>
              <artifactId>utbot-maven</artifactId>
              <version>1.0-SNAPSHOT</version>
          </plugin>
      </plugins>
  </build>
  ```

- Run maven task `utbot:generateTestsAndSarifReport` to create a report.


### How to configure

For example, the following configuration may be used:

```XML
<configuration>
    <targetClasses>
        <param>com.abc.Main</param>
        <param>com.qwerty.Util</param>
    </targetClasses>
    <projectRoot>C:/.../SomeDirectory</projectRoot>
    <generatedTestsRelativeRoot>target/generated/test</generatedTestsRelativeRoot>
    <sarifReportsRelativeRoot>target/generated/sarif</sarifReportsRelativeRoot>
    <markGeneratedTestsDirectoryAsTestSourcesRoot>true</markGeneratedTestsDirectoryAsTestSourcesRoot>
    <testPrivateMethods>false</testPrivateMethods>
    <projectType>purejvm</projectType>
    <testFramework>junit5</testFramework>
    <mockFramework>mockito</mockFramework>
    <generationTimeout>60000L</generationTimeout>
    <codegenLanguage>java</codegenLanguage>
    <mockStrategy>other-packages</mockStrategy>
    <staticsMocking>mock-statics</staticsMocking>
    <forceStaticMocking>force</forceStaticMocking>
    <classesToMockAlways>
        <param>org.slf4j.Logger</param>
        <param>java.util.Random</param>
    </classesToMockAlways>
</configuration>
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
    - By default, `'target/generated/test'` is used.

- `sarifReportsRelativeRoot` &ndash;
    - **Relative** path (against module root) to the root of the SARIF reports.
    - By default, `'target/generated/sarif'` is used.

- `markGeneratedTestsDirectoryAsTestSourcesRoot` &ndash;
    - _TODO: It has not been supported yet._
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
  `utbot-maven/publishing/publishToMavenLocal`
- Add the plugin to your project (see the section __How to use__).

### How to configure the log level

To change the log level run the `generateTestsAndSarifReport` task with the appropriate flag.

For example, `mvn utbot:generateTestsAndSarifReport --debug`

Note that the internal maven log information will also be shown.

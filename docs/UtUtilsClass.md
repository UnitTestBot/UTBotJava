# UtUtils class

## What are the utility methods

_Utility methods_ implement common, often re-used operations which are helpful for accomplishing tasks in many different classes. In UnitTestBot _utility methods_ include those related to creating instances, checking deep equals, working with arrays, using lambdas and so on — miscellaneous methods necessary for generating tests.

## Why to create UtUtils class

Previously UnitTestBot generated _utility methods_ for each test class when they were needed — and only those which were necessary for the given class. They were declared right in the generated test class, occupying space. Generating multiple test classes often resulted in duplicating _utility methods_ and consuming even more space.

For now UnitTestBot provides a special `UtUtils` class containing all _utility methods_ if at least one test class needs some of them. This class is generated once and the specific methods are imported from it if necessary. No need for _utility methods_ — no `UtUtils` class is generated.

## What does it look like

Here is an example of a comment inherent to every `UtUtils` class:

<img width="494" alt="ututils" src="https://user-images.githubusercontent.com/64418523/196719780-2603f141-e922-40fc-9a0a-533aaacc5c49.png">

As one can see, the comment mentions two characteristics of the `UtUtils` class:

1. _Version_

When the UnitTestBot plugin is upgraded and some new _utility methods_ become necessary for test generation, the existing `UtUtils` class is upgraded as well and gets a new version number. If the previously generated `UtUtils` class works well for the current test generation with the upgraded plugin, the version stays the same.

Whenever the contributors change the _utility methods_ necessary for test generation, they must update the version of `UtUtils` class here:

`org.utbot.framework.codegen.model.constructor.builtin.UtilClassFileMethodProvider.UTIL_CLASS_VERSION`

_2. Mockito support_

UnitTestBot uses Mockito framework to implement mocking. When test generation implies mocking, the deepEquals() _utility method_ should be configured — it should have a check: whether the compared object is a mock or not. That is why the `UtUtils` class for the tests with mocking differs from the one without mocking support.

If you have previously generated tests with mocking, the next `UtUtils` class will support mocking as well — even if it is upgraded or there is no need for mocking during current generation, so that existing tests can still rely on the proper methods from `UtUtils` class.

## Where to find it

`UtUtils` class is usually located in the chosen Test sources root near the generated test classes.

## How to test

If you want to test `UtUtils` class generation using UnitTestBot project itself, try out the next conditions and check the output:

|     | Pre-condition                                                                       | Condition                                                                                                                                                               | Class under test                                                                          | Output                                                                                                                                                         |
|:----|-------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1   | No `UtUtils` class                                                                  | Generate a test class, that does not need utility methods                                                                                                               | `IntExamples.java`                                                                        | No `UtUtils` class is produced.                                                                                                                                |
| 2   | No `UtUtils` class                                                                  | Generate a test class, that (a) needs a utility method, (b) does not use mocking                                                                                        | `QueueUsages.java`                                                                        | Regular `UtUtils` class (without mocking support) is produced.                                                                                                 |
| 3   | Regular `UtUtils` class (without mocking support) exists                            | Generate a test class, that (a) needs a utility method, (b) uses mocking                                                                                                | `CommonMocksExample.java`                                                                 | `UtUtils` class with mocking support is produced.                                                                                                              |
| 4   | `UtUtils` class with mocking support exists                                         | Generate a test class, that (a) needs a utility method, (b) does not use mocking                                                                                        | `QueueUsages.java`                                                                        | `UtUtils` class with mocking support from the previous generation stays the same.                                                                              |
| 5   | `UtUtils` class with mocking support exists                                         | Generate another test class, that (a) needs a utility method, (b) does not use mocking + create and choose another **Test sources root**: `utbot-sample/src/test/java2` | `AnonymousClassesExample.java`                                                            | No additional `UtUtils` class is generated in `utbot-sample/src/test/java2`. `UtUtils` class with mocking support from the previous generation stays the same. |
| 6   | No `UtUtils` class (delete previously generated test classes and `UtUtils` classes) | Generate a test class, that (a) does not need utility methods (b) uses mocking                                                                                          | `MockRandomExamples.java` and choose the method `randomAsParameter()` for test generation | No `UtUtils` class is produced.                                                                                                                                |

## How to improve

UnitTestBot does not currently support generating tests for classes from multiple modules simultaneously. If this option was possible, we would probably have to generate a separate `UtUtils` class for each module. Perhaps we could find a special location for a `UtUtils` class reachable from every module.

For now, you can generate separate `UtUtils` classes for different modules only if you manually choose the different **Test sources roots** when generating tests.
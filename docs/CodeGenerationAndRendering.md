# Code generation and rendering

Code generation and rendering are a part of the test generation process in UnitTestBot (find the overall picture in the 
[UnitTestBot architecture 
overview](https://github.com/UnitTestBot/UTBotJava/blob/main/docs/OverallArchitecture.md)). 
UnitTestBot gets the synthetic representation of generated test cases from the fuzzer or the symbolic engine.
This representation (or model) is implemented in the `UtExecution` class.

The `codegen` module generates the real test code based on this `UtExecution` model and renders it in a 
human-readable form in accordance with the requested configuration (considering programming language, testing 
framework, mocking and parameterization options).

The `codegen` module
- converts `UtExecution` test information into an Abstract Syntax Tree (AST) representation using `CodeGenerator`,
- renders this AST according to the requested programming language and other configurations using `renderer`.

## Example

Consider the following method under test:

```java
package pack;

public class Example {

    public int maxIfNotEquals(int a, int b) throws IllegalArgumentException {
        if (a == b) throw new IllegalArgumentException("a == b");
        if (a > b) return a; else return b;
    }
}
```

The standard UnitTestBot-generated tests for this method (without test summaries and clustering into regions) 
look like this:

```java
package pack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ExampleStandardTest {

    @Test
    @DisplayName("maxIfNotEquals: a == b : False -> a > b")
    public void testMaxIfNotEquals_AGreaterThanB() {
        Example example = new Example();

        int actual = example.maxIfNotEquals(1, 0);

        assertEquals(1, actual);
    }
    
    @Test
    @DisplayName("maxIfNotEquals: a == b -> ThrowIllegalArgumentException")
    public void testMaxIfNotEquals_AEqualsB() {
        Example example = new Example();

        assertThrows(IllegalArgumentException.class, () -> example.maxIfNotEquals(-255, -255));
    }
    
    @Test
    @DisplayName("maxIfNotEquals: a < 0, b > 0 -> return 1")
    public void testMaxIfNotEqualsReturnsOne() {
        Example example = new Example();

        int actual = example.maxIfNotEquals(-1, 1);

        assertEquals(1, actual);
    }
}
```

Here is an example of the parameterized tests for this method. We also implement the data provider method — the 
argument source.

```java
package pack;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public final class ExampleParameterizedTest {
    
    @ParameterizedTest
    @MethodSource("pack.ExampleTest#provideDataForMaxIfNotEquals")
    public void parameterizedTestsForMaxIfNotEquals(Example example, int a, int b, Integer expectedResult, Class expectedError) {
        try {
            int actual = example.maxIfNotEquals(a, b);

            assertEquals(expectedResult, actual);
        } catch (Throwable throwable) {
            assertTrue(expectedError.isInstance(throwable));
        }
    }
    
    public static ArrayList provideDataForMaxIfNotEquals() {
        ArrayList argList = new ArrayList();

        {
            Example example = new Example();

            Object[] testCaseObjects = new Object[5];
            testCaseObjects[0] = example;
            testCaseObjects[1] = 1;
            testCaseObjects[2] = 0;
            testCaseObjects[3] = 1;
            testCaseObjects[4] = null;
            argList.add(arguments(testCaseObjects));
        }
        {
            Example example = new Example();

            Object[] testCaseObjects = new Object[5];
            testCaseObjects[0] = example;
            testCaseObjects[1] = -255;
            testCaseObjects[2] = -128;
            testCaseObjects[3] = -128;
            testCaseObjects[4] = null;
            argList.add(arguments(testCaseObjects));
        }
        {
            Example example = new Example();

            Object[] testCaseObjects = new Object[5];
            testCaseObjects[0] = example;
            testCaseObjects[1] = -255;
            testCaseObjects[2] = -255;
            testCaseObjects[3] = null;
            testCaseObjects[4] = IllegalArgumentException.class;
            argList.add(arguments(testCaseObjects));
        }

        return argList;
    }
}
```

## Configurations

UnitTestBot renders code in accordance with the chosen programming language, testing framework, 
mocking and parameterization options.

Supported languages for code generation are:
- Java
- Kotlin (experimental) — we have significant problems with the support for nullability and generics
- Python and JavaScript — in active development

Supported testing frameworks are:
- JUnit 4
- JUnit 5
- TestNG (only for the projects with JDK 11 or later)

Supported mocking options are:
- No mocking
- Mocking with Mockito framework
- Mocking static methods with Mockito

Parameterized tests can be generated in Java only. Parameterization is not supported with the mocks enabled or 
with JUnit 4 chosen as the testing framework.

## Entry points

The `codegen` module gets calls from various UnitTestBot components. The most common scenario is to call `codegen` 
from integration tests as well as from the `utbot-intellij` project and its `CodeGenerationController` class. The 
`utbot-online` and `utbot-cli` projects call `codegen` as well. 

The `codegen` entry points are:
- `CodeGenerator.generateAsString()`
- `CodeGenerator.generateAsStringWithTestReport()`

The latter gets `UtExecution` information received from the symbolic engine or the fuzzer and converts it into the 
`codegen`-related data units, each called `CgMethodTestSet`. As a result of further processing, the test code is 
generated as a string with a test generation report (see [Reports](#Reports) for details).

Previously, `CgMethodTestSet` has been considerably different from `UtMethodTestSet` as it has been using 
`ExecutableId` instead of the legacy `UtMethod` (has been removed recently).
For now, `CgMethodTestSet` contains utility functions required for code generation, mostly for parameterized tests.

## Abstract Syntax Tree (AST)

The `codegen` module converts `UtExecution` information to the AST representation.
We create one AST per one source class (and one resulting test class). We use our own AST implementation.

We generate a `UtUtils` class containing a set of utility functions, when they are necessary for a given test class. 
If the `UtUtils` class has not been created previously, its AST representation is generated as well. To learn more 
about the `UtUtils` class and how it is generated, refer to the 
[design doc](https://github.com/UnitTestBot/UTBotJava/blob/main/docs/UtUtilsClass.md).

All the AST elements are `CgElement` inheritors. 
`CgClassFile` is the top level element — it contains `CgClass` with the required imports. 

The class has the body (`CgClassBody`) as well as minor properties declared: documentation comments, annotations, 
superclasses, interfaces, etc.
 
The class body is a set of `CgRegion` elements, having the `header` and the corresponding `content`, which is mostly 
the set of `CgMethod` elements.

The further AST levels are created similarly. The AST leaves are `CgLiteral`, `CgVariable`, 
`CgLogicalOr`, `CgEmptyLine`, etc.

## Test method

The below-mentioned functionality is implemented in `CgMethodConstructor`.

To create a test method:
* store the initial values of the static fields and perform the seven steps for creating test method body mentioned later;
* if the static field values undergo changes, perform these seven steps in the `try` block and recover these values in the `finally` block accordingly.

To create test method body:
1. substitute static fields with local variables
2. set up instrumentation (get mocking information from `UtExecution`)
3. create a variable for the current instance
4. create variables for method-under-test arguments
5. record an actual result by calling method under test
6. generate result assertions
7. for successful tests, generate field state assertions

_Note:_ generating assertions has pitfalls. In primitive cases, like comparing two integers, we can use the standard 
assertions of a selected test framework. To compare two objects of an arbitrary type, we need a 
custom implementation of equality assertion, e.g. using `deepEquals()`. The `deepEquals()` method compares object 
structures field by field. The method is recursive: if the current field is not of the primitive type, we call 
`deepEquals()` for this field. The maximum recursion depth is limited.

For the parameterized tests
- we do not support mocking, so we do not set up the initial environment;
- we do not generate field state assertions.

`UtExecution` usually represents a single test scenario, and one `UtExecution` instance is used to create a single
test method. Parameterized tests are supposed to cover several test scenarios, so several `UtExecution` instances 
are used for generating test methods.

## Generic execution

Parameterization often helps to reveal similarities between test scenarios. The combined outcome is sometimes more 
expressive. To represent these similarities, we construct generic executions.

Generic execution is a synthetic execution, formed by a group of real executions, that have
- the same type of result,
- the same modified static fields.

Otherwise, we create several generic executions and several parameterized tests. The logic of splitting executions 
into the test sets is implemented in the `CgMethodTestSet` class.

From the group of `UtExecution` elements, we take the first successful execution with the non-nullable result. See 
`CgMethodConstructor.chooseGenericExecution()` for more details.

## Renderer

We have a general approach for rendering the code of test classes. `UtUtils` class is rendered differently: we 
hardcode the required method implementations for the specific code generation language.

All the renderers implement `CgVisitor` interface. It has a separate `visit()` method for each supported 
`CgElement` item.

There are three renderers:
- `CgAbstractRenderer` for elements that are similar in Kotlin and Java
- `CgJavaRenderer` for Java-specific elements
- `CgKotlinRenderer` for Kotlin-specific elements

Each renderer method visits the current `CgElement`. It means that all the required instructions are printed properly.
If an element contains the other element, the first one delegates rendering to its _child_.

`CgVisitor` refers us to the _Visitor_ design pattern. Delegating means asking the _child_ element to 
accept the renderer and manage it. Then we go through the list of `CgElement` types to find the first 
matching `visit()` method.

_Note:_ the order of `CgElement` items listed in `CgElement.accept()` is important.

Rendering may be terminated if the generated code is too long (e.g. due to test generation bugs).

## Reports

While constructing the test class, we create test generation reports. It contains basic statistical information: the 
number of generated tests, the number of successful tests, etc. It also may contain information on potential problems 
like trying to use mocks when mocking framework is not installed.

The report is an HTML string with clickable links.

_Note:_ no test generation reports are created for parameterized tests.

## Services

Services help the `codegen` module to produce human-readable test code.

### Name generator

With this service, we create names for variables and methods. It assures avoiding duplicates in names, 
resolving conflicts with keywords, etc. It also adds suffixes if we process a mock or a static item.

Name generator is called directly from `CgStatementConstructor`.

_Note:_ if you need a new variable, you should better use this service (e.g. the `newVar()` method) instead of calling 
the `CgVariable` constructor manually.

### Framework and language services

Framework services help the `codegen` module to generate constructions (e.g. assertions) according to a given 
testing or mocking framework. 
Language services provide the `codegen` module with language-specific information on test class extensions, 
prohibited keywords, etc.

See the `Domain` file for more framework- and language-specific implementations.

### CgFieldStateManager

`CgFieldStateManager` stores the initial and the final environment states for the given method under test. 
These states are used for generating assertions. Usually, the environment state consists of three parts:
* current instance state,
* argument state, 
* static field state.

All the state-related variables are marked as `INITIAL` or `FINAL`.

### CgCallableAccessManager

This service helps to validate access. For example, if the current argument 
list is valid for the method under test, `CgCallableAccessManager` checks if one can call this method with these 
arguments without using _Reflection_.

`CgCallableAccessManager` analyzes callables as well as fields for accessibility.

## CgContext

`CgContext` contains context information for code generation. The `codegen` module uses one 
context per one test class. `CgContext` also stores information about the scope for the inner context elements: e.g. when 
they should be instantiated and cleared. For example, the context of the nested class is the part of the owner class context.

`CgContext` is the so-called "God object" and should be split into independent storages and 
helpers. This task seems to be difficult and is postponed for now.
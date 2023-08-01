# Taint analysis

## Introduction to the technique

Taint analysis allows you to track the propagation of unverified external data through the program.
If this kind of data reaches critical code sections, it may lead to vulnerabilities, including SQL injections, 
cross-site scripting (XSS) and others.
Attackers can use these vulnerabilities to disrupt correct system operation, get confidential data, or perform other unauthorized operations.
Taint analysis helps to find these mistakes at the compilation stage.

The key idea of the approach is that any variable an external user can change stores a potential security
threat. If the variable is used in some expression, then the value of this expression also becomes suspicious.
The algorithm tracks situations when the variables marked as suspicious are used in dangerous
command execution, for example, in direct queries to a database or an operating system.

Taint analysis requires a configuration, where you assign one of the following
roles to each method in a program.

- Taint source — a source of unverified data.
  For example, it can be a method for reading user input, or a method for getting a parameter of an incoming HTTP
  request.
  The Taint source execution result is marked. The method semantics determines the mark to be applied
  to the variable.
  The name of the mark can be completely arbitrary, since it is chosen by the one who writes the
  configuration.
  For example, according to configuration, the `getPassword()` method marks its return value with a "sensitive-data" 
  mark.
- Taint pass — a function that marks the return value taking into account the marks in its arguments.
  Depending on the implementation, marks can be applied not only to method results but also to a `this` object, and to 
  input parameters. For example, you can configure the `concat(String a, String b)` concatenation method 
  to mark its result with all the marks from `a` and `b`.
- Taint cleaner — a function that removes a given set of marks from the passed arguments.
  Most often, this is a kind of validation method that verifies that the user has entered data in the expected
  format.
  For example, the `validateEmail(String email)` method removes the "XSS" mark upon successful check completion,
  because no unverified data in the `email` object that can now lead to cross-site
  scripting vulnerability.
- Taint sink — a receiver, a critical section of an application.
  It can be a method that accesses a database or a file system directly, or perform other potentially dangerous
  operations.
  For the Taint sink, you can set a list of marks. Variables with specified marks should not leak into this taint sink.
  For example, if a value marked as "sensitive-data" is passed to a logging function, which prints its arguments 
  directly to the console, then this is a developer mistake, since data leaks.

The taint analysis algorithm scans the data flow graph, trying to detect a route between a method from a set of Taint
sources and a method from Taint sinks. The UnitTesBot taint analysis feature is implemented inside the symbolic engine
to avoid a large number of false positives.

**Example**

Consider an example of a simple function with an SQL injection vulnerability inside:
if an attacker enters the string `"name'); DROP TABLE Employees; --"` into the variable name, then it will be possible
to delete the `Employees` table with the data in it.

```java
class Example {
    void example(Connection connection) {
        Scanner sc = new Scanner(System.in);
        String name = sc.nextLine();
        Statement stmt = connection.createStatement();
        String query = "INSERT INTO Employees(name) VALUES('"
                .concat(name)
                .concat("')");
        stmt.executeUpdate(query);
    }
}
```

For taint analysis, you have to set the configuration.

- Taint source is a `java.util.Scanner.nextLine` method that adds a "user-input" mark to the returned value.
- Taint pass is a `java.lang.String.concat` method that passes the "user-input" marks through itself received
  either from the first argument or from the object on which this method is called (`this`).
- Taint sink is a `java.sql.Statement.executeUpdate` method that checks variables marked as "user-input".

Any correct implementation of the taint analysis algorithm should detect a mistake in this code: the variable with
the "user-input" mark is passed to `executeUpdate` (the sink).

Note that the algorithm is not responsible for detecting specific data that an attacker could
enter to harm the program. It only discovers the route connecting the source and the sink.

## UnitTestBot implementation

No unified configuration format is provided for taint analysis in the world, and all static analyzers describe their
own way of configuration. Thus, we provide a custom configuration scheme to describe the rules: sources, passes, 
cleaners, and sinks.

### Configuration: general structure

The general structure of the configuration format (based on YAML) is presented below.

```yaml
sources:
  - <rule-1>
  - <rule-2>
  - <...>
passes:
  - <rule>
  - <...>
cleaners:
  - <rule>
  - <...>
sinks:
  - <rule>
  - <...>
```

That is, these are just lists of rules related to a specific type.

Each rule has a set of characteristics.

- The unique identifier of the method that this rule describes.
  It consists of the method's full name, including the package name and the class name,
  as well as the signature of the method — a set of argument types (the `signature` key in the YAML file).
- Some `conditions` that must be met during execution for the rule to work.
- A set of `marks` that the rule uses.
- A set of specific mark management actions that occur when the rule is triggered (`add-to`, `get-from`, 
  `remove-from`, or `check`, depending on the semantics of the rule).

For example, the rule for the taint source can look like this.

```yaml
com.abc.ClassName.methodName:
  signature: [ <int>, _, <java.lang.Object> ]
  conditions:
    arg1:
      not: [ -1, 1 ]
  add-to: [ this, arg2, return ]
  marks: user-input
```

This rule is defined for a method named `methodName` from the `ClassName` class located in the `com.abc` package.
The method takes exactly 3 arguments: the first one has the `int` type, the second can be anything,
and the last one has the `java.lang.Object` type.
The `signature` key may not be specified, then any `methodName` overload is appropriate.

The rule is triggered when the first argument (`arg1`) is not equal to either -1 or 1 as specified by the `conditions` 
key (the list is interpreted as logical OR).
This parameter is optional, if it is absent, no conditions are checked.

The described source adds a "user-input" mark to the variables corresponding to `this`, `arg2` and `return`:
to the `ClassName` class object on which `methodName` is called, to the second argument of the function
and to the return value. Moreover, the `add-to` and `marks` keys can contain both a list, and a single value.

The other rule types have the same syntax as the source, except for the `add-to` key.

- Taint pass transfers marks from one set of objects to another, so two keys are defined for it:
  `get-from` and `add-to`, respectively. The marks specified in `marks` are added on `add-to` if there is a mark in 
  `get-from`.
- Taint cleaner removes marks from objects, so its key is called `remove-from`.
- Taint sink checks for the marks in variables, which locates under the `check` key.

### Configuration: details

Fully qualified method names can be written in one line or using nested structure: the package name is specified first,
then the class name appears, and finally, there is the method name itself.

```yaml
- com.abc.def.Example.example: ...
```

or

```yaml
- com:
    - abc.def:
        - Example.example: ...
```

Note that regular expressions in names are not supported yet.

The `add-to`, `get-from`, `remove-from`, and `check` fields specify the objects (or entities) to be marked.
You can specify only one value here or a whole list.

Possible values are:

- `this`
- `arg1`
- `arg2`
- ...
- `return`

The user can define arbitrary mark names or specify an empty list (`[]`) for all possible marks.

```yaml
passes:
  - java.lang.String.concat:
      get-from: this
      add-to: return
      marks: [ user-input, sensitive-data, my-super-mark ]
```

or

```yaml
passes:
  - java.lang.String.concat:
      get-from: this
      add-to: return
      marks: [ ] # all possible marks
```

To check the conformity to `conditions`, you can set:

- the specific values of method arguments
- their runtime types

Values can be set for the following types: `boolean`, `int`, `float` or `string` (and `null` value for all nullable
types).

The full type name must be specified in the angle brackets `<>`.

The `conditions` field specifies runtime conditions for arguments (`arg1`, `arg2`, ...).
Conditions can also be specified for `this` and `return` if it makes sense.
For sinks, checking conditions for a return value makes no sense, so this functionality is not supported.

```yaml
conditions:
  this: <java.lang.String> # this should be java.lang.String
  arg1: "test" # the first argument should be equal to "test"
  arg2: 227 # int
  arg3: 227.001 # float
  arg4: null # null
  return: true # return value should be equal to `true`
```

Values and types can be negated using the `not` key, as well as combined using lists (`or` semantics).

Nesting is allowed.

```yaml
conditions:
  this: [ "in", "out" ] # this should be equal to either "in" or "out"
  arg1: [ <int>, <float> ] # arg1 should be int or float
  arg2: { not: 0 } # arg2 should not be equal to 0
  arg3:
    not: [ 1, 2, 3, 5, 8 ] # should not be equal to any of the listed numbers
  arg4: [ "", { not: <java.lang.String> } ] # should be an empty string or not a string at all
```

If several rules are suitable for one method call, they are all applied.

**Overall example**

```yaml
sources:
  - com:
      - abc:
          - method1:
              signature: [ _, _, <java.lang.Object> ]
              add-to: return
              marks: xss
          - method1:
              signature: [ <int>, _, <java.lang.String> ]
              add-to: [ this, return ]
              marks: sql-injection
      - bca.method2:
          conditions:
            this:
              not: "in"
          add-to: return
          marks: [ sensitive-data, xss ]

passes:
  - com.abc.method2:
      get-from: [ this, arg1, arg3 ]
      add-to: return
      marks: sensitive-data
  - org.example.method3:
      conditions:
        arg1: { not: "" }
      get-from: arg1
      add-to: [ this, return ]
      marks: sql-injection

cleaners:
  - com.example.pack.method7:
      conditions:
        return: true
      remove-from: this
      marks: [ sensitive-data, sql-injection ]

sinks:
  - org.example:
      - log:
          check: arg1
          marks: sensitive-data
      - method17:
          check: [ arg1, arg3 ]
          marks: [ sql-injection, xss ]
```

**Usage examples**

`java.lang.System.getenv` is a source of the “environment” mark. There are two overloads of this method: with one string
parameter and no parameters at all. We want to describe only the first overload:

  ```yaml
  sources:
    - java.lang.System.getenv:
        signature: [ <java.lang.String> ]
        add-to: return
        marks: environment
  ```

`java.lang.String.concat` is a pass-through only if `this` is marked and not equal to `""`, or if `arg1` is marked and
not equal to `""`:

  ```yaml
  passes:
    - java.lang.String:
        - concat:
            conditions:
              this: { not: "" }
            get-from: this
            add-to: return
            marks: sensitive-data
        - concat:
            conditions:
              arg1: { not: "" }
            get-from: arg1
            add-to: return
            marks: sensitive-data
  ```

If you want to define a `+` operator for strings as taint pass, you should write the following rules:

```yaml
passes:
  - java.lang.StringBuilder.append:
      get-from: arg1
      add-to: this
      marks: [ ]
  - java.lang.StringBuilder.toString:
      get-from: this
      add-to: return
      marks: [ ]
```

`java.lang.String.isEmpty` is a cleaner only if it returns `true`:

  ```yaml
  cleaners:
    - java.lang.String.isEmpty:
        conditions:
          return: true
        remove-from: this
        marks: [ sql-injection, xss ]
  ```

Suppose that the `org.example.util.unsafe` method is a sink for the “environment” mark if the second argument is
an `Integer` and equal to zero:

  ```yaml
  sinks:
    - org.example.util.unsafe:
        signature: [ _, <java.lang.Integer> ]
        conditions:
          arg2: 0
        check: arg2
        marks: environment
  ```

The configuration above checks the type at compile time, but sometimes we want to check the type at runtime:

  ```yaml
  sinks:
    - org.example.util.unsafe:
        conditions:
          arg2:
            not: [ { not: 0 }, { not: <java.lang.Integer> } ]
        check: arg2
        marks: environment
  ```

Perhaps explicit `and` for `conditions` will be added in the future.

### Algorithm implementation details

The main idea of the implemented approach is that each symbolic variable is associated with a taint vector — a 64-bit
value, where each `i` bit is responsible for the presence of a mark with the number `i` in this object.
After that, during the symbolic execution, these mappings are maintained and updated in accordance with
the classical taint analysis algorithm.

The implementation mostly affects the `Traverser` and `Memory` classes, as well as the new `TaintMarkRegistry`
and `TaintMarkManager` classes. The diagram below shows a high-level architecture of the taint module (in the actual 
code, the implementation is a bit different, but to understand the idea, the diagram is greatly simplified).

<img src="https://github.com/UnitTestBot/UTBotJava/assets/54814796/0a199fc8-ef6c-4fc5-830d-91ed556a8e3f" height="250">

The `TaintMarkRegistry` class stores a mapping between the mark name and its ordinal number from 0 to 63.
The number of marks is limited to 64. However, firstly, this is enough for almost any reasonable example,
and secondly, the decision was made due to performance issues — operations with the `Long` data type are
performed much faster than if a bit array was used.

The `TaintModel` component (data classes at `org.utbot.taint.model`) is responsible for providing access
to the configuration. In particular, it defines a way to convert conditions (the value of the `conditions` key
in a YAML document) into logical expressions over symbolic variables.

`Memory` stores the values of the taint bit-vectors for symbolic variables. Only simple methods were implemented there
(functions to update vectors and get them at the address of a symbolic object).
All the complex logic of adding and removing marks, based on taint analysis theory,
was written in a separate `TaintMarkManager` class. In other words, this class wraps low-level memory work into
domain-friendly operations.

The information about the marked variables is updated during the `Traverser` work. Before each `invoke()`
instruction corresponding to the method call in the user code, a special `Traverser.
processTaintSink` handler is called, and after the `invoke` instruction, the `Traverser.processTaintSource`, 
`Traverser.processTaintPass` and `Traverser.processTaintCleaner` handlers are called. This order is set because all the
rules, except tose for the sinks, need the result of the function. At the same time, the fact of transferring 
the tainted data occurs when launching the sink function, therefore, you can report the vulnerability 
found even before it is executed.

The listed rule handlers get the configuration and perform the taint analysis semantics. The `processTaintSink` method
requests information from the `TaintMarkManager` about the marks already set and adds constraints to the SMT 
solver: the satisfiability corresponds to the defect detection. The other handlers modify the symbolic 
`Memory` through the `TaintMarkManager`, adding and removing marks from the selected symbolic variables.

### Code generator modification

UnitTestBot produces unit tests (and the SARIF reports). `CodeGenerator` is launched on each
found test case, and generates the test (as Java code). Moreover, the test, which leads to throwing an unhandled exception, 
should not pass. Taint analysis errors are not real from the language perspective, since they 
are not real exceptions. However, we still have to highlight such tests as failed. The code generator was modified
so that an artificial error was added at the end of each test to ensure a fail (the same strategy
was used in the integer overflow detection).

```java
fail("'java.lang.String' marked 'user-input' was passed into 'Example.example' method");
```

The solution allows us to automatically integrate with the SARIF reports and to visualize the results
in the IntelliJ IDEA _Problems_ tool window. The found test case is treated as a real exception,
and all the necessary logic has already been written for them.

**Example**

Consider the code below.

```java
public class User {

    String getLogin() { /* some logic */ }
  
    String getPassword() { /* some logic */ }
  
    String userInfo(String login, String password) {
        return login + "#" + password;
    }
  
    void printUserInfo() {
        var login = getLogin();
        var password = getPassword();
        var info = userInfo(login, password);
        System.out.println(info);
    }
}
```

The `getPassword` method returns sensitive data that should never leak out of the application, but the programmer prints
them to the `stdout`, which is a serious mistake. First, we write the corresponding configuration and save it to the 
`./.idea/utbot-taint-config.yaml` file for the analyzer to read from.

```yaml
sources:
  - User.getPassword:
      add-to: return
      marks: sensitive-data

passes:
  - User.userInfo:
      get-from: [ arg1, arg2 ]
      add-to: return
      marks: [ ] # all

sinks:
  - java.io.PrintStream.println:
      check: arg1
      marks: sensitive-data
```

Then we enable taint analysis in settings and run the UnitTestBot plugin in IntelliJ IDEA.

<img src="https://github.com/UnitTestBot/UTBotJava/assets/54814796/64f12291-5596-4c6e-a1cd-6ad4aab03e47" height="250">

Generated code:

```java
public final class UserTest {
  // some omitted code

  ///region SYMBOLIC EXECUTION: TAINT ANALYSIS for method printUserInfo()
  
  @Test
  @DisplayName("printUserInfo: System.out.println(info) : True -> DetectTaintAnalysisError")
  public void testPrintUserInfo_PrintStreamPrintln_1() {
    User user = new User();

    user.printUserInfo();
    fail("'java.lang.String' marked 'sensitive-data' was passed into 'PrintStream.println' method");
  }

  ///endregion
}
```

We can see the detected problem in the _Problems_ tool window:

<img src="https://github.com/UnitTestBot/UTBotJava/assets/54814796/59cacc9c-7329-4a3f-b496-5a0307c96d06" height="250">

**A brief explanation**

Upon executing the `getPassword` method, the symbol corresponding to the password variable
is marked as "sensitive-data" (a zero bit is set to 1 in its taint vector). Upon calling `userInfo`, the `info`
variable is also marked, since `userInfo` is a taint pass that adds the marks from
both of its arguments to the return value. Before printing `info` to the console, the `processTaintSink` handler function adds a constraint
to the SMT solver, so that its satisfiability corresponds to throwing an artificial error. The logical 
formula for
this path is satisfiable, so the analyzer reports an error detected, which we eventually observe.

### Unit tests

Taint analysis unit tests are located at the `./utbot-framework-test/src/test/kotlin/org/utbot/examples/taint/`
directory.

Test use examples are located at `utbot-sample/src/main/java/org/utbot/examples/taint`. Each example has its own
configuration file stored at `utbot-sample/src/main/resources/taint`.

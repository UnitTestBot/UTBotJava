# Taint analysis

## Introduction to the technique

Taint analysis is a method that allows you to track the spread of unverified external data on the program. Getting such
data into critical sections of the code can lead to various vulnerabilities, including SQL injection, cross-site
scripting
(XSS) and many others. Attackers can use these vulnerabilities to disrupt the correct operation of the system, obtain
confidential data or conduct other unauthorized operations. Taint analysis helps to find the described mistakes at the
compilation stage.

The key idea of the approach is that any variable that can be changed by an external user stores a potential security
threat.
If this variable is used in some expression, then the value of this expression also becomes suspicious.
Further, the algorithm tracks and notifies of situations when any of these marked variables are used in dangerous
command execution,
for example, direct queries to the database or the computer's operating system.

For its work, taint analysis requires a configuration in which program methods can be assigned one of the following
roles.

- Taint source — a source of unverified data.
  For example, it can be a method for reading user input or a method for obtaining a parameter of an incoming HTTP
  request.
  The Taint source execution result is called marked. The semantics of the method determines which mark will be applied
  to the variable.
  Moreover, the name of the mark can be completely arbitrary, since it is chosen by the one who writes the
  configuration.
  For example, it can be set that the `getPassword()` method marks its return value with a "sensitive-data" mark.
- Taint pass — a function that marks the return value taking into account the marks in its arguments.
  Depending on the implementation, marks can be applied not only to the result of the method but also to the object
  `this`, and on the input parameters. For example, it can be set that the concatenation
  method `concat strings(String a, String b)`
  marks its result with all marks from `a` and from `b`.
- Taint cleaner — a function that removes a given set of marks from the passed arguments.
  Most often, this is some kind of validation method that verifies that the user has entered data in the expected
  format.
  For example, the `validate Email(String email)` method removes the XSS mark upon successful completion of the checks,
  because now there is no unverified data in the `email` that can lead to vulnerability of cross-site
  scripting.
- Taint sink — a receiver, some critical section of the application.
  It can be a method of direct access to the database or file system, as well as any other potentially dangerous
  operation.
  For the Taint sink, you can set a list of marks. Variables with specified marks should not leak into this taint sink.
  For example, if a value marked "sensitive-data" is passed to the logging function, which prints its arguments directly
  to the console, then this is a developer mistake, since data leaks.

The taint analysis algorithm scans the data flow graph, trying to detect a route between a method from a set of Taint
sources
and a method from Taint sinks. The UnitTesBot taint analysis is implemented inside the symbolic engine
to avoid a large number of false positives.

### Example

Consider an example of a simple function in which there is an SQL injection vulnerability:
if an attacker enters the string `"name'); DROP TABLE Employees; --"` into the variable name, then he will be able
to delete the Employees table along with all the data that was stored in it.

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

For taint analysis, you must set the configuration.

- Taint source is a `java.util.Scanner.nextLine` method that adds a "user—input" mark to the returned value.
- Taint pass is a `java.lang.String.concat` method that passes the "user—input" marks through itself received
  either from the first argument or from the object on which this method is called (`this`).
- Taint sink is a `java.sql.Statement.executeUpdate` method that checks variables marked "user-input".

Any correct implementation of the taint analysis algorithm should detect a mistake in this code: the variable with
the mark "user-input" is passed to the `executeUpdate` (sink).

It is important to note that the duties of the algorithm do not include detecting specific data that an attacker could
enter to harm the program. It is only necessary to discover the route connecting the source and the sink.

## UnitTestBot implementation

There is no unified configuration format for taint analysis in the world and all static analyzers describe their
own way of configuration. Thus, we are going to do the same thing: to come up with a configuration scheme
where it would be possible to describe the rules: sources, passes, cleaners and sinks.

### Configuration

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

Each rule has a certain set of characteristics.

- The unique identifier of the method that this rule describes.
  It consists of the full name of the method, including the package name and class name,
  as well as the signature of the method — a set of types of its arguments (the `signature` key in YAML).
- Some `conditions` that must be met during execution for the rule to work.
- A set of `marks` that the rule uses.
- A set of specific mark management actions that occur when a rule is triggered (`add-to`, `get-from`, `remove-from`, or
  `check`, depending on the semantics of the rule).

Thus, one rule, for example, taint source, may look like

```yaml
com.abc.ClassName.methodName:
  signature: [ <int>, _, <java.lang.Object> ]
  conditions:
    arg1:
      not: [ -1, 1 ]
  add-to: [ this, arg2, return ]
  marks: user-input
```

This rule is defined for a method named `methodName` from the `ClassName` class, which is in the `com.abc` package.
The method takes exactly 3 arguments, the first of which has the `int` type, the second can be anything,
and the last has the `java.lang.Object` type.
The `signature` key may not be specified, then any `methodName` overload is considered appropriate.

Triggering occurs only when the first argument (`arg1`) is not equal to either -1 or 1,
which is specified by the `conditions` key (the list is interpreted as a logical OR).
This parameter is optional, if it is not present, no conditions will be checked.

The described source adds a "user-input" mark on the variables corresponding to `this`, `arg2` and `return`.
In other words, to the class object `ClassName` on which methodName is called, the second argument of the function
and the return value. Moreover, the `add-to` and `marks` keys can contain both a list and a single value — this is done
for more convenient use.

The other types of rules have the same syntax as the source, except for the `add-to` key.

- Taint pass transfers marks from one set of objects to another, so two keys are defined for it:
  `get-from` and `add-to`, respectively. The mark specified in `marks` are added on `add-to` if there is one in `get-from`.
- Taint cleaner removes marks from objects, so its key is called `remove-from`.
- Taint sink checks for the presence of some marks in variables, which locates under the `check` key.

### Configuration details

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

---

The `add-to`, `get-from`, `remove-from`, and `check` fields specify the objects (or entities) to be marked.
You can specify only one value here or a whole list.

Possible values are:

- `this`
- `arg1`
- `arg2`
- ...
- `return`

---

The user can define arbitrary mark names or specify an empty list (`[]`) if he means all possible marks.

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

---

To check the conformity to `conditions`, you can set:

- the specific values of method arguments
- their runtime types.

Values can be set for the following types: `boolean`, `int`, `float` or `string` (and also `null` value for all nullable
types).

The full type name must be specified in the angle brackets `<>`.

The `conditions` field specifies runtime conditions for arguments (`arg1`, `arg2`, ...).
Conditions can also be specified for `this` and `return` if it makes sense.
For sinks there is no sense to check some conditions for return value, so such functionality is not supported.

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

---

If several rules are suitable for one method call, they will all be applied in some kind of order.

---

### Overall example

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

### Usage examples

`java.lang.System.getenv` is a source of the "environment" mark. There are two overloads of this method: with one string
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

If you want to define `+` operator for strings as taint pass, you should write the following rules:

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

Suppose that the `org.example.util.unsafe` method is a sink for the "environment" mark if the second argument is
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

The configuration above checks the type at compile-time, but sometimes we want to check the type at runtime:

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
value, each bit `i` of which is responsible for the presence of a mark with the number `i` in this object.
After that, during the symbolic execution, these mappings are maintained and updated in accordance with
the classical taint analysis algorithm.

The implementation mostly affect the `Traverser` and `Memory` classes. Also, the new classes `TaintMarkRegistry`
and `TaintMarkManager`. The diagram below shows a high-level architecture of the taint module (actually, in the code
it was written a little differently, but to understand the idea, the diagram is greatly simplified).

<img src="https://github.com/UnitTestBot/UTBotJava/assets/54814796/0a199fc8-ef6c-4fc5-830d-91ed556a8e3f" height="250">

The `TaintMarkRegistry` class stores a mapping between the mark name and its ordinal number from 0 to 63.
The number of marks is limited to 64. However, firstly, this is enough for almost any reasonable example,
and secondly, the decision was made due to performance issues — operations with the `Long` data type are
performed much faster than if a bit array was used.

The TaintModel component (data classes at `org.utbot.taint.model`) is responsible for providing access
to the configuration. In particular, it defines a way to convert conditions (the value of the `conditions` key
in a YAML document) into logical expressions over symbolic variables.

`Memory` stores the values of the taint bit-vectors for symbolic variables. Only simple methods were implemented there
(functions to update vectors and get them at the address of a symbolic object).
All the complex logic of adding and removing marks, based on taint analysis theory,
was written in a separate class `TaintMarkManager`. In other words, this class wraps low-level memory work into
domain-friendly operations.

The information about the marked variables is updated during the `Traverser` work. Before each of the `invoke()`
instruction, which corresponds to the launch of some method in the user code, a special `Traverser.processTaintSink`
handler is called, and after the `invoke` instruction, the `Traverser.processTaintSource`, `Traverser.processTaintPass`
and `Traverser.processTaintCleaner` handlers are called. This order is because all rules, except sinks,
need the result of the function. At the same time, the transfer fact of tainted data occurs at the time of launching
the sink function, therefore, you can report the vulnerability found even before it is executed.

The listed rule handlers get the configuration and perform the taint analysis semantics. The `processTaintSink` method
requests information from the `TaintMarkManager` about the marks already set and adds constraints to the SMT solver,
the satisfiability of which corresponds to the detection of a defect. The other handlers modify the symbolic `Memory`
through the `TaintMarkManager`, adding and removing marks from the selected symbolic variables.

### Code generator modification

The result of the UnitTestBot (in addition to the SARIF report) are unit tests. `CodeGenerator` is launched on each
found test case, and generates the test (as Java code). Moreover, the test which leads to throwing an unhandled exception
should not pass. The taint analysis errors are not real from the point of view of the language, since they are not
real exceptions. However, it is still needed to highlight such tests as failed, so the code generator was modified
in such a way that an artificial error was added at the end of each test, which would ensure a fall (the same strategy
was used in the integer overflow detection).

```java
fail("'java.lang.String' marked 'user-input' was passed into 'Example.example' method");
```

The solution allows to automatically get integration with SARIF reports and visualization of results
in the Problems view tab in IntelliJ IDEA. The found test case is treated as a real exception,
and all the necessary logic has already been written for them.

### Example

Consider the code below

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
them to the `stdout`, which is a serious mistake. First, we will write a configuration that expresses the thought said,
and save it to the file `./.idea/utbot-taint-config.yaml`, from where the analyzer can read it.

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

Then we enable taint analysis in settings and run the UnitTestBot using the IntelliJ IDEA plugin.

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

Also, we can see the detected problem on the Problems tab:

<img src="https://github.com/UnitTestBot/UTBotJava/assets/54814796/59cacc9c-7329-4a3f-b496-5a0307c96d06" height="250">

**A brief explanation**: After executing the `getPassword` method, the symbol corresponding to the password variable
is marked as "sensitive-data" (a zero bit is set to 1 in its taint vector). After calling `userInfo`, the `info`
variable is also marked, since `userInfo` is a taint pass that adds to the return value all the marks collected from
both of its arguments. Before printing `info` to the console, the `processTaintSink` handler function adds a constraint
to the SMT solver, the satisfiability of which corresponds to throwing our artificial error. The logical formula for
this path is satisfiable, so the analyzer reports an error detected, which we eventually observe.

### Unit tests

Taint analysis unit tests are at the `./utbot-framework-test/src/test/kotlin/org/utbot/examples/taint/`
directory. Tests use examples at the `utbot-sample/src/main/java/org/utbot/examples/taint`. Each example has its own
configuration file stored at the `utbot-sample/src/main/resources/taint`.

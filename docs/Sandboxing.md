# Sandboxing tests with Java Security Manager

## What is sandboxing?

Sandboxing is a security technique to find unsafe code fragments and prevent them from being executed.

What do we mean by "unsafe code" in Java? The most common forbidden actions are:

* working with files (read, write, create, delete),
* connecting to [sockets](https://github.com/UnitTestBot/UTBotJava/issues/792),
* invoking `System.exit()`,
* accessing system properties or JVM properties,
* using reflection.

## Why do we need sandboxing for test generation?

During test generation, UnitTestBot executes the source code with the concrete values. All the fuzzer runs require 
concrete execution and some of the symbolic execution processes invoke it as well. If the source code contains 
potentially unsafe operations, executing them with the concrete values may lead to fatal errors. It is safer to catch 
these operations and break the concrete execution process with `AccessControlException` thrown.

## What do the sandboxed tests look like?

When the source code fragments are suspicious and the corresponding test generation processes are interrupted, the tests with the `@Disabled` annotation and a stack trace appear in the output:

    public void testPropertyWithBlankString() {
        SecurityCheck securityCheck = new SecurityCheck();
        
        /* This test fails because method [com.company.security.SecurityCheck.property] produces [java.security.AccessControlException: access denied ("java.util.PropertyPermission" "   " "read")]
            java.security.AccessControlContext.checkPermission(AccessControlContext.java:472)
            java.security.AccessController.checkPermission(AccessController.java:886)
            java.lang.SecurityManager.checkPermission(SecurityManager.java:549)
            java.lang.SecurityManager.checkPropertyAccess(SecurityManager.java:1294)
            java.lang.System.getProperty(System.java:719)
            com.company.security.SecurityCheck.property(SecurityCheck.java:32) */
    }

## How does UnitTestBot sandbox code execution?

UnitTestBot for Java/Kotlin uses [Java Security Manager](https://docs.oracle.com/javase/tutorial/essential/environment/security.html) for sandboxing. In general, the Security Manager allows applications to implement a security policy. It determines whether an operation is potentially safe or not and interrupts the execution if needed.

In UnitTestBot the **secure mode is enabled by default**: only a small subset of runtime permissions necessary for 
test generation are given (e.g. fields reflection is permitted by default). To extend the list of permissions learn 
[How to handle sandboxing](#How-to-handle-sandboxing).

Java Security Manager monitors [all the code](https://github.com/UnitTestBot/UTBotJava/issues/791) for the risk of performing forbidden operations, including code in _class constructors, private methods, static blocks, [threads](https://github.com/UnitTestBot/UTBotJava/issues/895)_, and combinations of all of the above.

## How to handle sandboxing

You can **add permissions** by creating and editing the `~\.utbot\sandbox.policy` file. Find more about [Policy File and Syntax](https://docs.oracle.com/javase/7/docs/technotes/guides/security/PolicyFiles.html#Examples) and refer to the [Full list of permissions](https://docs.oracle.com/javase/1.5.0/docs/guide/security/spec/security-spec.doc3.html) to choose the proper approach.

If the permission was added but somehow [not recognized](https://github.com/UnitTestBot/UTBotJava/issues/796), the UnitTestBot plugin will fail to start and generate no tests.

If you are sure you want the code to be executed as is (**including the unsafe operations!**) you can **turn sandboxing off**:

* You can add `AllPermission`  to `~\.utbot\sandbox.policy`. Be careful!
* Alternatively, you can add `useSandbox=false` to `~\.utbot\settings.properties`. Create this file manually if you don't have one. Find [more information](https://github.com/UnitTestBot/UTBotJava/pull/857) on how to manage sandboxing to test the UnitTestBot plugin itself.

It is reasonable to regard the `@Disabled` tests just as supplemental information about the source code, not as the tests for actual usage.

## How to improve sandboxing

For now there are several unsolved problems related to sandboxing in UnitTestBot:

1. We need to replace Java Security Manager with our own tool.

   [Java Security Manager is deprecated since JDK 17](https://openjdk.org/jeps/411) and is subject to removal in some 
 future version. It is still present in JDK 19 but with limited functionality. E.g., in Java 18, a Java application or library is prevented from dynamically installing a Security Manager unless the end user has explicitly opted to allow it. Obviously, we cannot rely upon the deprecated tool and need to create our own one.


2. We need to provide a unified and readable description for disabled tests.

   UnitTestBot supports three testing frameworks and their annotations are slightly different:

JUnit 4: `@Ignore("<comment>")`

JUnit 5: `@Disabled("<comment>")`

TestNG: `@Ignore` as an alternative to `@Test(enabled=false)`

* How should we unify these annotations?
* How should we show info in Javadoc comments?
* Do we need to print a stack trace?
  

3. We need to add emulation for restricted operations (a kind of mocks)

   Emulating unsafe operations will allow UnitTestBot to generate useful tests even for the sandboxed code and run them instead of disabling.

4. We need to provide a user with the sandboxing settings.

   The UnitTestBot plugin UI provides no information about configuring the behavior of Security Manager. Information on [How to 
  handle 
sandboxing](#How-to-handle-sandboxing) is available only on GitHub.

* Should we add Sandboxing (or Security Manager) settings to plugin UI? E.g.: **File operations: Forbidden / Allowed / Emulated in sandbox**.

* Should we add a hyperlink to a piece of related documentation or to the `~\.utbot\sandbox.policy` file?

## How to test sandboxing

See the [short manual testing scenario](https://github.com/UnitTestBot/UTBotJava/pull/625) and the [full manual testing checklist](https://github.com/UnitTestBot/UTBotJava/issues/790).

## Related links

Initial feature request: [Add SecurityManager support to block suspicious code #622](https://github.com/UnitTestBot/UTBotJava/issues/622)

Pull request: [Add SecurityManager support to block suspicious code #622 #625](https://github.com/UnitTestBot/UTBotJava/pull/625)

Improvement request: [Improve sandbox-relative description in generated tests #782](https://github.com/UnitTestBot/UTBotJava/issues/782)
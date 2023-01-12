---
name: Bug report
about: Create a report to help us improve
title: ''
labels: 'ctg-bug'
assignees: ''

---

**Description**

_Example:_ There are hundreds of error messages in the UTBot log. Looks like the problem is in the concrete executor.

**To Reproduce**

Steps to reproduce the behavior:

_Example:_

1. Run the 'X' project in IntelliJ Idea
2. Use plugin to generate tests
3. Open the generated test

**Expected behavior**

_Example:_ Tests are supposed to be generated.

**Actual behavior**

_Example:_ An error test is generated with information about errors in the concrete executor.

**Visual proofs (screenshots, logs, images)**

_Example:_
~~~
public void testFail_errors() {
// Couldn't generate some tests. List of errors:
//
// 684 occurrences of:
// Concrete execution failed
}
~~~

**Environment**

_Substitute this text with an information that can help us to recreate the environment. For instance, specify Java version, test framework with a version (JUnit, TestNG...), Mockito configuration (packages, classes, statics), were the test parametrized or not and so on._

**Additional context**

Add any other context about the problem here.

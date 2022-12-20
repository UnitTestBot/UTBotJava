# UnitTestBot decomposition

This document is a part of UnitTestBot roadmap for the nearest future. We plan to decompose the whole UnitTestBot mechanism into the following standalone systems.

## Fuzzing platform

Entry points:
* `org.utbot.fuzzing.Fuzzing`
* `fuzz` extension function

Exit point:
overridden `Fuzzing#run` method

Probable fields of use (without significant implementation changes):
1.	Test generation
2.	Taint analysis
3.	Finding security vulnerabilities
4.	Static analysis validation
5.	Automatic input minimization
6.	Specific input-output search

## Symbolic engine platform

Probable fields of use (without significant implementation changes):
1.	Test generation
2.	Taint analysis
3.	Type inference

A more abstract interface can be extracted. For instance, we can use the interface to solve type constraints for Python or other languages.
Currently, there are two levels of abstraction:
1.	Java-oriented abstraction that is intended to mimic heap and stack memory.
2.	More low-level and less Java-coupled API to store constraints for Z3 solver.

There is a room for improvement, namely we can extract more high-level abstraction, which can be extended for different languages.

Entry points:
* `org.utbot.engine.Memory`
* `org.utbot.engine.state.LocalVariableMemory`
* `org.utbot.engine.SymbolicValue`

Exit point:
`org.utbot.engine.Resolver` → `UtModel`

Another level of abstraction is `UtExpression`. Basically, it is a thin abstraction over Z3 solver.

## Program synthesis system

An implementation that allows `UtAssembleModel` to keep information about object creation in a human-readable format. Otherwise, the object state should be initiated with _Reflection_ or sufficient constructor call. The synthesizing process is built upon the UnitTestBot symbolic execution memory model and is supposed to preserve construction information during the analysis process.

Entry and exit point:
`org.utbot.framework.synthesis.Synthesizer`

## Program analysis system

We use an outdated approach with the [Soot](https://github.com/soot-oss/soot) framework. It is not worth being extracted as a separate service. A good substitution is the [JacoDB](https://github.com/UnitTestBot/jacodb) library. Currently, this library provides an API to work with Java code, send queries, provide custom indexes, and so on.

## Code generation system

The current domain of code generation is specific for generating tests, though it could be reused for other purposes. Currently, the engine can be used to generate tests for different test frameworks. One can use the code generator to generate test templates inside the IntelliJ-based IDEs.

Entry and exit point:
`org.utbot.framework.codegen.CodeGenerator#generateAsStringWithTestReport`

## SARIF report visualizer

UnitTestBot represents the result of analysis using SARIF — the format that is widely used in the GitHub community. SARIF allows users to easily represent the results in the built-in GitHub viewer. Additionally, we provide our own SARIF report visualizer for IntelliJ IDEA.

Entry and exit point:
`org.utbot.gradle.plugin.GenerateTestsAndSarifReportTask`
# Summarization module

## Overview

UnitTestBot minimizes the number of tests so that they are necessary and sufficient, but sometimes there are still a lot of them. Tests may look very similar to each other, and it may be hard for a user to distinguish between them. To ease test case comprehension, UnitTestBot generates summaries, or human-readable test descriptions. Summaries also facilitate navigation: they structure the whole collection of generated tests by clustering them into groups.

Summarization module generates detailed meta-information:
- test method names
- testing framework annotations (including `@DisplayName`)
- Javadoc comments for tests
- cluster comments for groups of tests (_regions_)

Javadoc comments can be rendered in two styles: as plain text or in a special format enriched with the [custom Javadoc tags](https://github.com/UnitTestBot/UTBotJava/blob/main/docs/summaries/CustomJavadocTags.md).

If the summarization process fails due to an error or insufficient information, then the test method receives a unique name and no meta-information.

The whole summarization subsystem is located in the `utbot-summary` module.

## Implementation

At the last stage of test generation process, the `UtMethodTestSet.summarize` method is called.
As input, this method receives the set of `UtExecution` models with empty `testMethodName`, `displayName`, and `summary` fields. It fills these fields with the corresponding meta-information, groups the received `UtExecution` models into clusters and generates cluster names.

Currently, there are three main `UtExecution` implementations:
* `UtSymbolicExecution`,
* `UtFailedExecution`,
* `UtFuzzedExecution`.

To construct meta-information for the `UtFuzzedExecution` models, the summarization module uses method parameters with their values and types as well as the return value type. To generate summaries for each `UtSymbolicExecution`, it uses the symbolic code analysis results.

Let's describe this process in detail for `UtSymbolicExecution` and `UtFuzzedExecution`.

### Constructing meta-information for `UtSymbolicExecution`

1. **Producing _Jimple statements_.**
   For each method under test (or MUT), the symbolic execution engine generates `UtMethodTestSet` consisting of `UtExecution` models, i.e. a test suite consisting of unit tests. A unit test (or `UtExecution`) in this suite is a set of execution steps that traverses a particular path in the MUT. An execution `Step` contains info on a statement, the depth of execution step and an execution decision.
* A statement (`stmt`) is a Jimple statement, provided with the [Soot](https://github.com/soot-oss/soot) framework. A Jimple statement is a simplified representation of the Java program that is based on the three-address code. The symbolic engine accepts Java bytecode and transforms it to the Jimple statements for the analytical traversal of execution paths.
* The depth of execution step (`depth`) depicts an execution depth of the statement in a call graph where the MUT is a root.
* An execution decision (`decision`) is a number indicating the execution direction inside the control flow graph. If there are two edges coming out of the execution statement in the control flow graph, a decision number shows what edge is chosen to be executed next.

2. **_Tagging_.**
   For each pair of `UtMethodTestSet` and its source code file, the summarization module identifies unique execution steps, recursions, iteration cycles, skipped iterations, etc. These code constructs are marked with tags or meta-tags, which represent the execution paths in a structural view. The summarization module uses these tags directly to create meta-information, or summaries.

At this moment, the summarization module is able to assign the following tags:
- Uniqueness of a statement:
   - _Unique_: no other execution path in the cluster contains this step, so only one execution triggers this statement in its cluster.
   - _Common_: all the paths execute these statements.
   - _Partly Common_: only some executions in a cluster contain this step.
- The decision in the CFG (branching): _Right_, _Left_, _Return_
- The number of statement executions in a given test
- Dealing with loops: _starting/ending an iteration_, _invoking the recursion_, etc.

We use our own implementation of the [DBSCAN](https://en.wikipedia.org/wiki/DBSCAN) clustering algorithm with the non-euclidean distance measure based on the Minimum Edit Distance to identify _unique_, _common_ and _partly common_ execution steps. Firstly, we manually divided execution paths into groups:
- successfully executed paths (only this group is clustered into different regions with DBSCAN)
- paths with expected exceptions
- paths with unexpected exceptions
- other groups with errors and exceptions based on the given `UtResult`

3. **Building _sentences_.**
   _Sentences_ are the blocks for the resulting summaries.
   To build the _sentence_, the summarization module
- parses the source file (containing the MUT) using [JavaParser](https://javaparser.org/) to get AST representations;
- maps the AST representations to Jimple statements (so each statement is mapped to AST node);
- builds the _sentence_ blocks (to provide custom Javadoc tags or plain-text mode);
- builds the _final sentence_ (valid for plain-text mode only);
- generates the `@DisplayName` annotation and test method names using the following rule: find the last _unique_ statement in each path (preferably, the condition statement) that has been executed once (being satisfied or unsatisfied); then the AST node of this statement is used for naming the execution;
- builds the cluster names based on the _common_ execution paths.

### Constructing meta-information for `UtFuzzedExecution`

For `UtFuzzedExecution`, meta-information is also available as test method names, `@DisplayName` annotations, Javadoc comments, and cluster comments.

The difference is that clustering tests for `UtFuzzedExecution` is based on `UtResult`. No subgroups are generated for the successfully completed tests.

The algorithm for generating meta-information is described in the `ModelBasedNameSuggester` class, which is the registration point for `SingleModelNameSuggester` interface. This interface is implemented in `PrimitiveModelNameSuggester` and `ArrayModelNameSuggester`.

Depending on the received `UtExecutionResult` type, `ModelBasedNameSuggester` produces the basic part of the method name or the `@DisplayName` annotation. `UtFuzzedExecution` provides `FuzzedMethodDescription` and `FuzzedValue` that supplement the generated basic part for test name with information about the types, names and values of the MUT parameters.

_Note:_ test method names and `@DisplayName` annotations are generated if only the number of MUT parameters is no more than three, otherwise they are not generated.


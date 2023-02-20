# UnitTestBot Go

UnitTestBot Go automatically generates ready-to-use unit tests for Go programs.

With UnitTestBot Go, you can find bugs in your code and fixate the desired program behavior with less effort.

The project is under development,
so feel free to [contribute](https://github.com/UnitTestBot/UTBotJava/blob/main/utbot-go/docs/DEVELOPERS_GUIDE.md).

## Features and details

UnitTestBot Go now implements the _basic fuzzing technique_.
It generates input values with respect to parameter types,
inserts these values into the user functions, and executes the resulting test cases.

### Supported types for function parameters

At the moment, UnitTestBot Go is able to generate values for _primitive types_, _arrays_ and _structs_.

For _floating point types_, UnitTestBot Go supports working with _infinity_ and _NaN_.

### Supported types for the returned results

For the returned function results,
UnitTestBot Go supports the `error` type in addition to all the types supported for the function parameters.

It also captures `panic` cases correctly.

Check the examples of [supported functions](https://github.com/UnitTestBot/UTBotJava/blob/main/utbot-go/go-samples/simple/samples.go).

### Keeping tests near the source code

[Testing code typically
lives in the same package as the code it tests](https://gobyexample.com/testing).
By default, UnitTestBot Go generates tests into the `[name of source file]_go_ut_test.go` file located in the same
directory and Go package as the corresponding source file.

If you need to change the location for the generated tests,
use the `generateGo` CLI command and set the generated test output mode to
`StdOut` (`-p, --print-test` flag).
Then, with bash primitives, redirect the output to an arbitrary file.

### Requirements to source code

To simplify handling dependencies and generating tests, UnitTestBot Go requires the code under test _to
be a part of a Go project_ consisting of a module and packages.

To create a simple project, refer to the [starter tutorial](https://go.dev/doc/tutorial/getting-started) if necessary.

For larger projects, try the [Create a Go module](https://go.dev/doc/tutorial/create-module) 
and [Call your code from another module](https://go.dev/doc/tutorial/call-module-code) tutorials.

To create a new module rooted in the current directory, use the `go mod init` command.

To add missing module requirements necessary to build the current module’s packages and dependencies,
use the `go mod tidy` command. For editing and formatting `go.mod` files, use the `go mod edit` command.

In the future, we plan to make UnitTestBot Go working with arbitrary code as input and generate the simplest
Go projects automatically.

## Installation and usage

### IntelliJ IDEA plugin

#### Requirements

* IntelliJ IDEA (Ultimate Edition) — versions from 2022.2 to 2022.2.4
* Go SDK 1.18 or later
* Compatible [Go plugin](https://plugins.jetbrains.com/plugin/9568-go) for IntelliJ IDEA
* Properly configured `go.mod` file for the code under test
* `github.com/stretchr/testify/assert` Go module installed (IntelliJ IDEA automatically offers to install it as soon as the tests are generated)

#### Installation

To install the UnitTestBot Go plugin in IntelliJ IDEA, refer to [UnitTestBot user guide](https://github.com/UnitTestBot/UTBotJava/wiki/Install-or-update-plugin). 

#### Usage

1. In your IntelliJ IDEA, go to **File** > **Settings** > **Tools** > **UnitTestBot** and enable **Experimental languages support**.
2. Open a `.go` file and press **Alt+Shift+U**.
3. In the **Generate Tests with UnitTestBot** window, you can configure the settings.

### CLI

#### Requirements

* Java SDK 11 or later
* Go SDK 1.18 or later
* Properly configured `go.mod` file for the code under test
* GCC as well as `github.com/stretchr/testify/assert` and `golang.org/x/tools@v0.4.0` Go modules installed

#### Installation

To install the UnitTestBot Go CLI application, go to GitHub, scroll through the release notes and click **Assets**.
Download the zip-archive named like **utbot-cli-VERSION**.
Extract the JAR file from the archive.

#### Usage

Run the extracted JAR file using a command line: `generateGo` and `runGo` actions are supported for now.
To find info about the options for these actions,
insert the necessary JAR file name instead of `utbot-cli-2022.8-beta.jar` in the example and run the following commands:

```bash
java -jar utbot-cli-2022.8-beta.jar generateGo --help
```
or
```bash
java -jar utbot-cli-2022.8-beta.jar runGo --help
```

`generateGo` options:

* `-s, --source TEXT`, _required_: specifies a Go source file to generate tests for.
* `-f, --function TEXT`, _required_: specifies a function name to generate tests for. Can be used multiple times to select multiple
  functions.
* `-go, --go-path TEXT`, _required_: specifies a path to a Go executable. For example, `/usr/local/go/bin/go`.
* `-et, --each-execution-timeout INT`: specifies a timeout in milliseconds for each target function execution.
  The default timeout is 1,000 ms.
* `-at, --all-execution-timeout INT`: specifies a timeout in milliseconds for all target function executions.
  The default timeout is 60,000 ms.
* `-p, --print-test`: specifies whether a test should be printed out to `StdOut`. Disabled by default.
* `-w, --overwrite`: specifies whether to overwrite the output test file if it already exists. Disabled by default.
* `-h, --help`: shows a help message and exits.

`runGo` options:

* `-p, --package TEXT`, _required_: specifies a Go package to run tests for.
* `-go, --go-path TEXT`, _required_: specifies a path to a Go executable. For example, `/usr/local/go/bin/go`.
* `-v, --verbose`: specifies whether an output should be verbose. Disabled by default.
* `-j, --json`: specifies whether an output should be in JSON format. Disabled by default.
* `-o, --output TEXT`: specifies an output file for a test run report. Prints to `StdOut` by default.
* `-cov-mode, --coverage-mode [html|func|json]`: specifies whether a test coverage report should be generated and defines the report format.
  Coverage report generation is disabled by default.
* `-cov-out, --coverage-output TEXT`: specifies the output file for a test coverage report. Required if `[--coverage-mode]` is
  set.

## Contributing to UnitTestBot Go

To take part in project development or learn more about UnitTestBot Go, check
out the [Developer guide](docs/DEVELOPER_GUIDE.md) and our [future plans](docs/FUTURE_PLANS.md).
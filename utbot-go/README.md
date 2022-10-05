# UTBot Go

## About project

UTBot Go _**automatically generates unit tests for Go programs**_. Generated tests:

* provide _high code coverage_ and, as a result, its reliability;
* fixate the current behavior of the code as _regression tests_.

The core principles of UTBot Go are _**ease of use**_ and _**maximizing code coverage**_.

***

_The project is currently under development._

## Features

At the moment, only the _basic fuzzing technique_ is supported: namely, the execution of functions on predefined values,
depending on the type of parameter.

At the moment, functions are supported, the parameters of which have _any primitive types_, namely:

* `bool`
* `int`, `int8`, `int16`, `int32`, `int64`
* `uint`, `uint8`, `uint16`, `uint32`, `uint64`
* `byte`, `rune`, `string`
* `float64`, `float32`
* `complex128`, `complex64`
* `uintptr`

For floating point types, _correct work with infinities and NaNs_ is also supported.

Function result types are supported the same as for parameters, but with _support for types that implement `error`_.

In addition, UTBot Go correctly captures not only errors returned by functions, but also _`panic` cases_.

Examples of supported functions can be found [here](samples).

## Install and use easily

### IntelliJ IDEA plugin

<ins>_Requirements:_</ins>

* `IntelliJ IDEA (Ultimate Edition)`, compatible with version `2022.1`;
* installed `Go SDK` version compatible with `1.19` or `1.18`;
* installed in IntelliJ IDEA [Go plugin](https://plugins.jetbrains.com/plugin/9568-go), compatible with the IDE
  version (it is for this that the `Ultimate` edition of the IDE is needed);
* properly configured Go module for source code file (i.e. for file to generate tests for): corresponding `go.mod` file
  must exist.

Most likely, if you are already developing Go project in IntelliJ IDEA, then you have already met all the requirements.

<ins>_To install the UTBot Go plugin in IntelliJ IDEA:_</ins>

* just find the latest version of [UnitTestBot](https://plugins.jetbrains.com/plugin/19445-unittestbot) in the plugin
  market;
* or download zip archive with `utbot-intellij JAR`
  from [here](https://github.com/UnitTestBot/UTBotJava/actions/runs/2926264476) and install it in IntelliJ IDEA as
  follows from plugins section (yes, you need to select the entire downloaded zip archive, it does not need to be
  unpacked).
  ![](docs/images/install-intellij-plugin-from-disk.png)

Finally, you can <ins>_start using UTBot Go_</ins>: open any `.go` file in the IDE and press `alt + u, alt + t`. After
that, a window will appear in which you can configure the test generation settings and start running it in a couple
of clicks.

[//]: # (See some example screenshots:)

[//]: # ()

[//]: # (* opened `.go` source code file)

[//]: # (* test generation configuration window)

[//]: # (* generated file with tests)

### CLI application

<ins>_Requirements:_</ins>

* installed `Java SDK` version `11` or higher;
* installed `Go SDK` version compatible with `1.19` or `1.18`;
* properly configured Go module for source code file (i.e. for file to generate tests for): corresponding `go.mod` file
  must exist.

<ins>_To install the UTBot Go CLI application:_</ins> download zip archive containing `utbot-cli JAR`
from [here](https://github.com/UnitTestBot/UTBotJava/actions/runs/2926264476), then extract its content (JAR file) to a
convenient location.

Finally, you can <ins>_start using UTBot Go_</ins> by running the extracted JAR on the command line. For example, to
find out about all flags of UTBot Go CLI application, run the command as follows (`utbot-cli-2022.8-beta.jar` here is
the path to the extracted JAR).

```bash
java -jar utbot-cli-2022.8-beta.jar generateGo --help
```

<ins>_UTBot Go CLI application options:_</ins>

* `-s, --source TEXT`, _required_: specifies Go source file to generate tests for.
* `-f, --function TEXT`: specifies function name to generate tests for. Can be used multiple times to select multiple
  functions at the same time. If no functions are specified, all functions contained in the source file are selected.
* `-g, --go-path TEXT`, _required_: specifies path to Go executable. For example, it could be `/usr/local/go/bin/go` for
  some systems.
* `-p, --print-test`: specifies whether a test should be printed out to StdOut.
* `-w, --overwrite`: specifies whether to overwrite the output test file if it already exists.
* `-h, --help`: show help message and exit.

## Contribute to UTBot Go

If you want to _take part in the development_ of the project or _learn more_ about how it works, check
out [DEVELOPERS_GUIDE.md](docs/DEVELOPERS_GUIDE.md).

For the current list of tasks, check out [FUTURE_PLANS.md](docs/FUTURE_PLANS.md).

Your help and interest is greatly appreciated!

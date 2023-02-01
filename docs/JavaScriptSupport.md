# UnitTestBot JavaScript plugin setup

## How to start using UnitTestBot JavaScript

1. [Install](https://www.jetbrains.com/idea/download/) the latest version of IntelliJ IDEA Ultimate.
2. [Install](https://plugins.jetbrains.com/plugin/19445-unittestbot) the latest version of UnitTestBot plugin.
3. [Install](https://nodejs.org/en/download/) Node.js 10.0.0 or later. Add Node.js to environment variables for better experience.
4. In your IntelliJ IDEA, go to **File** > **Settings** > **Tools** > **UnitTestBot** and enable **Experimental languages support**.
5. Go to **File** > **Settings** > **Languages & Frameworks**, choose **Node.js** and check if the path to Node.js executable file is specified.
6. In a JavaScript file, press **Alt+Shift+U** to open the generation dialog.

## Troubleshooting: _npm_ cannot install requirements

1. The system prohibits installation

Solution: run _cmd_ via `sudo` or with administrator access, run `npm install -g <missing requirement>`.

2. Node.js is missing, or _npm_ is not installed

Solution: install Node.js with default configuration from the official website.

# JavaScript Command Line Interface usage

## Build

JAR file can be built in [GitHub Actions](https://github.com/UnitTestBot/UTBotJava/actions/workflows/publish-plugin-and-cli-from-branch.yml) with the `publish-plugin-and-cli-from-branch` script.

## Requirements

* [Install](https://nodejs.org/en/download/) Node.js 10.0.0 or later
* [Install](https://www.oracle.com/java/technologies/downloads/) Java 11 or later
* Install _nyc_ 15.1.0 or later: `> npm install -g nyc`
* Install Mocha 10.0.0 or later: `> npm install -g mocha`

## Basic usage

### Generate tests: `generate_js`

    java -jar utbot-cli.jar generate_js --source="dir/file_with_sources.js" --output="dir/generated_tests.js"

  This will generate tests for top-level functions from `file_with_sources.js`.

#### Options

- `-s, --source <path>`

  _(required)_ Source code file for test generation.
- `-c, --class <classname>`

  Specifies the class to generate tests for.
  If not specified, tests for top-level functions or a single class are generated.

- `-o, --output <dir/filename>`

  File for generated tests.
- `-p, --print-test`

  Specifies whether a test should be printed out to `StdOut` (default = false).
- `-t, --timeout <seconds>`

  Timeout for a single test case to generate: in seconds (default = 15).
- `--coverage-mode <BASIC/FAST>`

  Specifies the coverage mode for test generation (used for coverage-based optimization). For now, the fast mode cannot deal with exceeding timeouts, but works faster (default = FAST). Do not use the fast mode if you guess there might be infinite loops in your code.
- `--path-to-node <path>`

  Sets a path to Node.js executable (default = "node").
- `--path-to-nyc <path>`

  Sets a path to _nyc_ executable (default = "nyc").
- `--path-to-npm <path>`

  Sets a path to _npm_ executable (default = "npm").

### Run generated tests: `run_js`

    java -jar utbot-cli.jar run_js --fileOrDir="generated_tests.js"

  This will run generated tests from a file or directory.

#### Options

- `-f, --fileOrDir`

  _(required)_ File or directory with tests.
- `-o, --output`

  Specifies the output TXT file for a test framework result (if empty, prints the result to `StdOut`).

- `-t, --test-framework <name>`

  Test framework to use for test running (default = "Mocha").

### Generate a coverage report: `coverage_js`

    java -jar utbot-cli.jar coverage_js --source=dir/generated_tests.js

  This will generate a coverage report for generated tests and print it to `StdOut`.

#### Options

- `-s, --source <file>`

  _(required)_ File with tests to generate a report for.

- `-o, --output`

  Specifies the output JSON file for a coverage report (if empty, prints the report to `StdOut`).
- `--path-to-nyc <path>`

  Sets a path to _nyc_ executable (default = "nyc").

## Build

.jar file can be built in GitHub Actions with script publish-plugin-and-cli-from-branch.

## Requirements

* NodeJs 10.0.0 or higher (available to download https://nodejs.org/en/download/)
* Java 11 or higher (available to download https://www.oracle.com/java/technologies/downloads/)
* Nyc 15.1.0 or higher (`> npm install -g nyc`)
* Mocha 10.0.0 or higher (`> npm install -g mocha`)

## Basic usage

Generate tests:

    java -jar utbot-cli.jar generate_js --source="dir/file_with_sources.js" --output="dir/generated_tests.js"

This will generate tests for top-level functions from `file_with_sources.js`.

Run generated tests:

    java -jar utbot-cli.jar run_js --fileOrDir="generated_tests.js"

This will run generated tests from file or directory.

Generate coverage report:

    java -jar utbot-cli.jar coverage_js --source=dir/generated_tests.js

This will generate coverage report from generated tests and print in `StdOut`

## `generate_js` options

- `-s, --source <path>`

  (required) Source code file for a test generation.
- `-c, --class <classname>`

  If not specified, tests for top-level functions or single class are generated, otherwise for the specified class.

- `-o, --output <dir/filename>`

  File for generated tests.
- `-p, --print-test`

  Specifies whether test should be printed out to `StdOut` (default = false)
- `-t, --timeout <seconds>`

  Timeout for a single test case to generate in seconds (default = 15)
- `--coverage-mode <BASIC/FAST>`

  Specifies the coverage mode for test generation. Fast mode can't find timeouts, but works faster (default = FAST)
- `--path-to-node <path>`

  Sets path to Node.js executable (default = "node")
- `--path-to-nyc <path>`

  Sets path to nyc executable (default = "nyc")
- `--path-to-npm <path>`

  Sets path to npm executable (default = "npm")

## `run_js` options

- `-f, --fileOrDir`

  (required) File or directory with tests.
- `-o, --output`

  Specifies output of .txt file for test framework result (If empty prints to `StdOut`)

- `-t, --test-framework <name>`

  Test framework of tests to run. (default = "Mocha")

## `coverage_js` options

- `-s, --source <file>`

  (required) File with tests to generate a report.

- `-o, --output`

  Specifies output .json file for generated tests (If empty prints .json to `StdOut`)
- `--path-to-nyc <path>`

  Sets path to nyc executable (default = "nyc")

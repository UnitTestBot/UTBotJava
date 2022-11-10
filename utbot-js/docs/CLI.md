## Build

.jar file can be built in GitHub Actions with script publish-plugin-and-cli-from-branch.

## Requirements

* NodeJs 10.0.0 or higher (available to download https://nodejs.org/en/download/)
* Java 11 or higher (available to download https://www.oracle.com/java/technologies/downloads/)

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

  If not specified tests for top-level functions are generated, otherwise for the specified class.

- `-o, --output <dir/filename>`

  File for generated tests.
- `-p, --print-test`

  Specifies whether test should be printed out to `StdOut` (default = false)
- `-t, --timeout <seconds>`

  Timeout for Node.js to run scripts in seconds (default = 5)

## `run_js` options

- `-f, --fileOrDir`

  (required) File or directory with tests.
- `-o, --output`

  Specifies output of .txt file for test framework result (If empty prints to `StdOut`)

- `-t, --test-framework [mocha]`

  Test framework of tests to run.

## `coverage_js` options

- `-s, --source <file>`

  (required) File with tests to generate a report.

- `-o, --output`

  Specifies output .json file for generated tests (If empty prints .json to `StdOut`)

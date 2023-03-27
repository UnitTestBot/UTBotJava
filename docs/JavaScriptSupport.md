# UnitTestBot JavaScript

[UnitTestBot](https://www.utbot.org/) is the tool for automated unit test generation available as an IntelliJ IDEA plugin, or a command-line interface.

Now UnitTestBot provides fuzzing-based support for JavaScript.

## IntelliJ IDEA plugin

### Requirements

1. IntelliJ IDEA Ultimate — for compatibility, see [UnitTestBot on JetBrains Marketplace](https://plugins.jetbrains.com/plugin/19445-unittestbot/versions).
2. UnitTestBot plugin: please refer to [UnitTestBot user guide](https://github.com/UnitTestBot/UTBotJava/wiki/Install-or-update-plugin).
3. [Node.js 10.0.0 or later](https://nodejs.org/en/download/) (we recommend that you add Node.js to environment variables)
 
_Note:_ when _npm_ cannot install requirements, try troubleshooting.
1. If the system prohibits installation: run _cmd_ via `sudo` or with administrator access, run `npm install -g <missing requirement>`.
2. If Node.js is missing, or _npm_ is not installed: install Node.js with default configuration from the official website.

### How to use

1. In your IntelliJ IDEA, go to **File** > **Settings** > **Tools**, choose **UnitTestBot** and enable **Experimental languages support**.
   
    **(!) NOTE:** be sure to enable this option for **_each_** project.

2. Go to **File** > **Settings** > **Languages & Frameworks**, choose **Node.js** and check if the path to Node.js executable file is specified.
3. In a JavaScript file, press **Alt+Shift+U** to open the generation dialog.

## Command-line interface (CLI)

### Build

JAR file can be built in [GitHub Actions](https://github.com/UnitTestBot/UTBotJava/actions/workflows/publish-plugin-and-cli-from-branch.yml) with the `publish-plugin-and-cli-from-branch` script.

### Requirements

* [Node.js 10.0.0 or later](https://nodejs.org/en/download/)
* [Java 11 or later](https://www.oracle.com/java/technologies/downloads/)
* _nyc_ 15.1.0 or later: `> npm install -g nyc`
* Mocha 10.0.0 or later: `> npm install -g mocha`

_Note:_ for each new project, _npm_ needs internet connection to install the required packages.

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

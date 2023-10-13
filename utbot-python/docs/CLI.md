## Build

.jar file can be built in Github Actions with script `publish-plugin-and-cli-from-branch`.

## Requirements

 - Required Java version: 11.

 - Prefered Python version: 3.8+.

    Make sure that your Python has `pip` installed (this is usually the case). [Read more about pip installation](https://pip.pypa.io/en/stable/installation/).

    Before running utbot install pip requirements (or use `--install-requirements` flag in `generate_python` command):

        python -m pip install utbot_executor utbot_mypy_runner

## Basic usage

Generate tests:

    java -jar utbot-cli.jar generate_python dir/file_with_sources.py -p <PYTHON_PATH> -o generated_tests.py -s dir

This will generate tests for top-level functions from `file_with_sources.py`.

Run generated tests:

    java -jar utbot-cli.jar run_python generated_tests.py -p <PYTHON_PATH>

### `generate_python` options
  
- `-s, --sys-path <dir1>,<dir2>`              

  (required) Directories to add to `sys.path`. One of directories must contain the file with the methods under test.
  
  `sys.path` is a list of strings that specifies the search path for modules. It must include paths for all user modules that are used in imports.

- `-p, --python-path <path>`           

  (required) Path to Python interpreter.
  
- `-o, --output <filename>`                

  (required) File for generated tests.

- `--coverage <filename>`                  
  
  File to write coverage report.
  
- `-c, --class <class>`
  
  Specify top-level (ordinary, not nested) class under test. Without this option tests will be generated for top-level functions.
  
- `-m, --methods <method1>,<method2>`

  Specify methods under test.

- `--install-requirements`           

  Install Python requirements if missing.
  
- `--do-not-minimize`                
  
  Turn off minimization of the number of generated tests.

- `--do-not-check-requirements`
  
  Turn off Python requirements check (to speed up).
  
- `-t, --timeout INT`                

  Specify the maximum time in milliseconds to spend on generating tests (60000 by default).
  
- `--timeout-for-run INT`            

  Specify the maximum time in milliseconds to spend on one function run (2000 by default).

- `--test-framework [pytest|Unittest]`

  Test framework to be used.

- `--do-not-generate-regression-suite`

  Do not generate regression test suite.

- `--runtime-exception-behaviour [PASS|FAIL]`

  Runtime exception behaviour (assert exceptions or not).

- `--coverage`

  File to save coverage report.
  
### `run_python` options

- `-p, --python-path <path>`
  
  (required) Path to Python interpreter.

- `--test-framework [pytest|Unittest]`
  
  Test framework of tests to run.

- `-o, --output <filename>`

  Specify file for report.

## Problems

- Unittest can not run tests from parent directories 

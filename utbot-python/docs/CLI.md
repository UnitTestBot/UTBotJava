## Requirements

Prefered Python version: 3.8 or 3.9.

Before running utbot install pip requirements (or use `--install-requirements` flag in `generate_python` command):

    python -m pip install mypy==0.971 astor typeshed-client coverage

## Basic usage

Generate tests:

    java -jar utbot-cli.jar generate_python dir/file_with_sources.py -p <PYTHON_PATH> -o generated_tests.py -s dir

This will generate tests for top-level functions from `file_with_sources.py`.

Run generated tests:

    java -jar utbot-cli.jar run_python generated_tests.py -p <PYTHON_PATH>

## `generate_python` options
  
- `-s, --sys-path <dir1>,<dir2>`              

  Directories to add to sys.path (required). One of directories must contain file with methods under test.

- `-p, --python-path <path>`           

  Path to Python interpreter (required)
  
- `-o, --output <filename>`                

  File for generated tests (required)

- `--coverage <filename>`                  
  
  File to write coverage report
  
- `-c, --class <class>`
  
  Specify top-level class under test
  
- `-m, --methods <method1>,<method2>`

  Specify methods under test

- `--install-requirements`           

  Install Python requirements if missing
  
- `--do-not-minimize`                
  
  Turn off minimization of number of generated tests

- `--do-not-check-requirements`
  
  Turn off Python requirements check (to speed up)
  
- `--visit-only-specified-source`

  Do not search for classes and imported modules in other Python files from sys.path

- `-t, --timeout INT`                

  Specify the maximum time in milliseconds to spend on generating tests (60000 by default)
  
- `--timeout-for-run INT`            

  Specify the maximum time in milliseconds to spend on one function run (2000 by default)

- `--test-framework [pytest|Unittest]`

  Test framework to be used
  
## `run_python` options

- `-p, --python-path <path>`
  
  Path to Python interpreter (required)

- `--test-framework [pytest|Unittest]`
  
  Test framework of tests to run

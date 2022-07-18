# UtBot-Python
__Task__: implement utbot for Python using fuzzing to generate tests.

Subtasks:
* Get list of functions to be tested
* Generate input parameters for this functions
* Compute return values for this parameters
* Render tests

## Getting list of functions

We get list of functions to be tested from Intellij IDEA plugin. Other information we get from source code.

Information about functions:
* Name
* List of parameters
* Source code
* Declaration file
* Type annotations for parameters and return type (optional)

## Input parameters generation

### Problem

If we do not have type annotation, we have to find suitable types for this parameter.

### Solution
Gather information about Python built-in types (by 'built-in types' we mean types that are implemented in C):

* Name
* Methods: name + parameters (+ annotations)
* How to generate instances of this type (default, random, using constants from code)

We can use CPython code and tests for it to gather this.

For user class we need to initialize its fields recursively. Possible problems: getting types of fields, dynamic addition of new fields.

To find suitable types for parameter we can look for them only in given and imported files.

To narrow down the search of suitable types we can gather constraints for function parameters. For that we can analyze AST to see which attributes of parameter are used.

## Run function with generated parameters

After generating parameters for fuzzing we pass them on into the function under test and run it in a separate process. This approach is called concrete execution.

To run the function we need to generate code that imports and calls it and saves result.

## Get return value

We write serialized return value in file. To serialize values of the most used built-in types we can use json module. For other types we will have to do it manually.

## Test generation

First we build AST of test code and then render it.

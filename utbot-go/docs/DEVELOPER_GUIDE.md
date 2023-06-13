# UnitTestBot Go Developer Guide

## Overview

Here are the main stages of UnitTestBot Go test generation pipeline.

```mermaid
flowchart TB
    A["Target selection and configuration (IntelliJ IDEA plugin or CLI)"]:::someclass --> B(Go source code analysis)
    classDef someclass fill:#b810

    B --> C(Code instrumentation)
    C --> D(Generation of arguments)
    D --> E(Executing functions)
    E --> F(Getting results)
    F --> D
    D ---> G(Test file generation)
```

### Target selection and configuration

A user manually selects the target source file and functions to generate the tests for.
Test generation settings are also configured manually.

### Code instrumentation

UnitTestBot Go adds logging about the passage of a given line during the execution 
after each line in functions.

### Go source code analysis

UnitTestBot Go collects the size in bits of an int or uint value and information about the target functions:
* signatures and type information;
* constants in the function bodies.

As a result, UnitTestBot Go gets an internal
representation of the target functions.

### Generation of arguments

At this stage, UnitTestBot Go generates the input values for the target functions.

### Executing target functions

UnitTestBot Go executes the target functions with the generated values as arguments.

### Getting results

The result of target function execution is sent to the fuzzer for analysis:
coverage rate information guides the following value generation process.
Based on remaining time, 
the fuzzer decides whether it should continue or stop generating input values.

### Test code generation

Generating test source code requires support for Go language features (for example, the necessity to cast
constants to the desired type).
Extended support for various Go constructs is a future plan for us. 

## How to test UnitTestBot Go

_**TODO**_

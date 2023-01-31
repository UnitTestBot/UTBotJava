# Taint Analysis

## Configuration

### Basic

- There are 4 sections: sources, passes, cleaners and sinks. Each of them contains a list of methods descriptions (rules). Fully qualified method names can be written in one line, or it can be defined by nested parts — first the package names, then the class name, and finally the method name. Note that regular expressions in names are not supported.
- Each rule can contain a method signature — a list of argument types (in compile-time): `signature: [  <int>, _, <java.lang.String> ]`
  - name of the type is written in `<>`.
  - `_` means any type.
- Also, the rule can contain runtime conditions that must be met for triggering this rule. Here you can set specific values of method arguments or their runtime types. More detailed information about the `conditions` is given below.
- The `signature` and `conditions` fields are optional. If you do not specify them, the rule will be triggered on any call of the method.
- If several rules are suitable for one method call, they will all be applied in some kind of order.

### Sources

- The `add-to` field specifies which objects will be marked. You can specify only one value here or a whole list. Possible values:
    - `this`
    - `arg1`
    - `arg2`
    - ...
    - `return`
- The `marks` field specifies which `marks` to add to objects from `add-to`. You can also specify only one mark here or a whole list.

**Example:**

```yaml
sources:
  - com:
      - abc:
          - method1:
              signature: [ _, _, <java.lang.Object> ]
              add-to: return
              marks: xss
          - method1:
              signature: [ <int>, _, <java.lang.String> ]
              add-to: [ this, return ]
              marks: sql-injection
      - bca.method2:
          add-to: return
          marks: [ sensitive-data, xss ]
  - org.example.method3:
      add-to: [ this, arg1, arg3 ]
      marks: sensitive-data
```

### Passes

- The `get-from` field specifies the sources of `marks`.
- The `add-to` field specifies which objects will be marked, if any element from `get-from` has any mark.
- The `marks` field specifies the actual marks that can passed from one object to another.
- For all the keys listed above, there can be only one value or a whole list of values. Also, it's true for similar keys in the sections below.

**Example:**

```yaml
passes:
  - com.abc.method2:
      get-from: [ this, arg1, arg3 ]
      add-to: [ arg2, return ]
      marks: sensitive-data
```

### Cleaners

- The `remove-from` field specifies which objects to remove `marks` from.
- The `marks` field specifies the marks to be removed.

**Example:**

```yaml
cleaners:
  - com.example.pack.method7:
      remove-from: this
      marks: [ sensitive-data, sql-injection ]
```

### Sinks

- The `check` field specifies which objects will be checked for `marks`.
- When one of the `marks` is found in one of the objects from the `check`, the analysis will report the problem found.

**Example:**

```yaml
sinks:
  - org.example:
      - log:
          check: arg1
          marks: sensitive-data
      - method17:
          check: [ arg1, arg3 ]
          marks: [ sql-injection, xss ]
```

### Conditions

- The `conditions` field specifies runtime conditions for arguments (`arg1`, `arg2`, ...). Conditions can also be specified for `this` and `return` (if it makes sense).
- The rule is triggered if all the specified conditions are met.
- The condition can check a specific value or runtime type.
- Values can be set for the following types: boolean, int, float or string.
- The full name of the type must be specified in triangular brackets `<>`.

**Example:**

```yaml
conditions:
  this: <java.lang.String> # this should be java.lang.String
  arg1: "test" # the first argument should be equal to "test"
  arg2: 227 # int
  arg3: 227.001 # float
  return: true # return value should be equal to `true`
```

- Values and types can be negated using the `not` key, as well as combined using lists (`or` semantics).
- Nesting is allowed.

**Example:**

```yaml
conditions:
  this: [ "in", "out" ] # this should be equal to one of: "in" or "out"
  arg1: [ <int>, <float> ] # arg1 should be int or float
  arg2: { not: 0 } # arg2 should not be equal to 0
  arg3:
    not: [ 1, 2, 3, 5, 8 ] # should not be equal to any of the listed numbers
  arg4: [ "", { not: <java.lang.String> } ] # should be an empty string or not a string at all
```

### Overall example

```yaml
sources:
  - com:
      - abc:
          - method1:
              signature: [ _, _, <java.lang.Object> ]
              add-to: return
              marks: xss
          - method1:
              signature: [ <int>, _, <java.lang.String> ]
              add-to: [ this, return ]
              marks: sql-injection
      - bca.method2:
          conditions:
            this:
              not: "in"
          add-to: return
          marks: [ sensitive-data, xss ]

passes:
  - com.abc.method2:
      get-from: [ this, arg1, arg3 ]
      add-to: return
      marks: sensitive-data
  - org.example.method3:
      conditions:
        arg1: { not: "" }
      get-from: arg1
      add-to: [ this, return ]
      marks: sql-injection

cleaners:
  - com.example.pack.method7:
      conditions:
        return: true
      remove-from: this
      marks: [ sensitive-data, sql-injection ]

sinks:
  - org.example:
      - log:
          check: arg1
          marks: sensitive-data
      - method17:
          check: [ arg1, arg3 ]
          marks: [ sql-injection, xss ]
```
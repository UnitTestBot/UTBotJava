# Taint analysis

[//]: # (TODO)

## Configuration

There are four _rule types_: sources, passes, cleaners and sinks.

The rule of each type contains
* a method name,
* a method description

### Method name

Fully qualified _method names_ can be written in one line.

One can also define a full _method name_ using nested structure: the package name is specified first,
then the class name appears, and finally there is the method name itself.

Note that regular expressions in names are not supported.

### Method description

Method description constitutes the essence of a rule.
Here are method descriptions for the rules of each type: sources, passes, cleaners and sinks.

#### Sources

The `add-to` field specifies the objects to be marked.
You can specify only one value here or a whole list.

Possible values are:
* `this`
* `arg1`
* `arg2`
* ...
* `return`

The `marks` field specifies the `marks` that should be added to the objects from the `add-to` list. You can also specify only one mark here or a whole list.

Example:

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

#### Passes

The `get-from` field specifies the sources of `marks`.

The `add-to` field specifies the objects that will be marked if any element from `get-from` has any mark.

The `marks` field specifies the actual marks that can be passed from one object to another.

For all the keys listed above, there can be only one value or a whole list of values.
This is also true for similar keys in the sections below.

Example:

```yaml
passes:
  - com.abc.method2:
      get-from: [ this, arg1, arg3 ]
      add-to: [ arg2, return ]
      marks: sensitive-data
```

#### Cleaners

The `remove-from` field specifies the objects the `marks` should be removed from.

The `marks` field specifies the marks to be removed.

Example:

```yaml
cleaners:
  - com.example.pack.method7:
      remove-from: this
      marks: [ sensitive-data, sql-injection ]
```

#### Sinks

The `check` field specifies the objects that will be checked for `marks`.

When one of the `marks` is found in one of the objects from the `check`, the analysis will report the problem found.

Example:

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
***

For all the rule types, method descriptions can optionally contain
* a method signature,
* runtime conditions.

**Signature**

Method `signature` (optional) is a list of _argument types_ (at compile time):

`signature: [  <int>, _, <java.lang.String> ]`

Please note that
- the type name is written in `<>`,
- `_` means any type.

**Conditions**

Runtime `conditions` (optional) are conditions that must be met to trigger this rule.

To check the conformity to conditions, you can set:
* the specific values of method arguments 
* or their runtime types.

Values can be set for the following types: `boolean`, `int`, `float` or `string`.

The full type name must be specified in the angle brackets `<>`.

If you do not specify `conditions`, the rule will be triggered by any call of the method.

If several rules are suitable for one method call, they will all be applied in some kind of order.

The `conditions` field specifies runtime conditions for arguments (`arg1`, `arg2`, ...). Conditions can also be specified for `this` and `return` (if it makes sense).

The rule is triggered if all the specified conditions are met.

Example:

```yaml
conditions:
  this: <java.lang.String> # this should be java.lang.String
  arg1: "test" # the first argument should be equal to "test"
  arg2: 227 # int
  arg3: 227.001 # float
  arg4: null # null
  return: true # return value should be equal to `true`
```

Values and types can be negated using the `not` key, as well as combined using lists (`or` semantics).

Nesting is allowed.

Example:

```yaml
conditions:
  this: [ "in", "out" ] # this should be equal to either "in" or "out"
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

## Usage examples

`java.lang.System.getenv` is a source of the "environment" mark. There are two overloads of this method: with one string parameter and no parameters at all. We want to describe only the first overload:

  ```yaml
  sources:
    - java.lang.System.getenv:
        signature: [ <java.lang.String> ]
        add-to: return
        marks: environment
  ```

`java.lang.String.concat` is a pass-through only if `this` is marked and not equal to `""`, or if `arg1` is marked and not equal to `""`:

  ```yaml
  passes:
    - java.lang.String:
        - concat:
            conditions:
              this: { not: "" }
            get-from: this
            add-to: return
            marks: sensitive-data
        - concat:
            conditions:
              arg1: { not: "" }
            get-from: arg1
            add-to: return
            marks: sensitive-data
  ```

`java.lang.String.isEmpty` is a cleaner only if it returns `true`:

  ```yaml
  cleaners:
    - java.lang.String.isEmpty:
        conditions:
          return: true
        remove-from: this
        marks: [ sql-injection, xss ]
  ```

Suppose that the `org.example.util.unsafe` method is a sink for the "environment" mark if the second argument is an `Integer` and equal to zero:

  ```yaml
  sinks:
    - org.example.util.unsafe:
        signature: [ _, <java.lang.Integer> ]
        conditions:
          arg2: 0
        check: arg2
        marks: environment
  ```
  
The configuration above checks the type at compile-time, but sometimes we want to check the type at runtime:

  ```yaml
  sinks:
    - org.example.util.unsafe:
        conditions:
          arg2:
            not: [ { not: 0 }, { not: <java.lang.Integer> } ]
        check: arg2
        marks: environment
  ```
  
Perhaps explicit `and` for `conditions` will be added in the future.

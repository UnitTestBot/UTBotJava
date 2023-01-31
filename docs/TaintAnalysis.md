# Taint Analysis

## Configuration

- Each method description can contain the key `conditions` (optionally).
- Keys like `add-to` or `remove-from` can contain a list of values or only one value. 
  Possible values:
  - this
  - arg1
  - arg2
  - ...
  - return
- The `marks` key can also contain a list of marks or only one mark.
- Fully qualified name of the method may be determined in parts.

**Example:**

```YAML
sources:
  com:
    abc.method1:
      add-to: [this, return] 
      marks: xss
    bca.method2:
      add-to: return
      marks: [sensitive-data, sql-injection]

passes:
  com.abc.method2:
    conditions:
      this: "some string" # value: bool, int, float or string
      arg0:
        not: ""
      arg1: <int> # type
      arg2: [1, 2, 3] # arg2 should be equal to one of: 1, 2 or 3 
      arg3:
        not: [4, 5, 6]
      arg4: [<float>, <java.lang.String>]
      arg5:
        not: [<int>, <boolean>]
      return: false
    get-from: [this, arg1, arg3]
    add-to: [return]
    marks: sensitive-data

cleaners:
  java.lang.String.isEmpty:
    conditions:
      return: true
    remove-from: this
    marks: [sensitive-data, sql-injection]
  com.company.method8:
    remove-from: [arg1, return]
    marks: xss

sinks:
  org.example:
    log:
      check: arg1
      marks: sensitive-data
    sink0:
      check: [arg1, arg3]
      marks: [sql-injection, xss]
```
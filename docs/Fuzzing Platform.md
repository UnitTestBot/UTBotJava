# Fuzzing Platform (FP) Design

**Problem:** fuzzing is a versatile technique for generating values to be used as method arguments. Normally, 
to generate values, one needs information on a method signature, or rather on the parameter types (if a fuzzer is 
able to "understand" them).
The _white-box_ approach also requires AST, and the _grey-box_ approach needs coverage 
information. To generate values that may serve as method arguments, the fuzzer uses generators, mutators, and 
predefined values.

* _Generators_ yield concrete objects created by descriptions. The basic description for creating objects is _type_. 
  Constants, regular expressions, and other structured object specifications (e.g. in HTML) may also be used as 
  descriptions.

* _Mutators_ modify the object in accordance with some logic that usually means random changes. To get better 
  results, mutators obtain feedback (information on coverage and the inner state of the 
  program) during method call.

* _Predefined values_ work well for known problems, e.g. incorrect symbol sequences. To discover potential problems, one can analyze parameter names as well as the specific constructs or method calls inside the method body.

The general API for using the fuzzer looks like this:

```
fuzz(
    params = "number", "string", "object<object, number>: number, string",
    seedGenerator = (type: Type) -> seeds
    details: (constants, providers, etc)
).forEveryGeneratedValues { values: List ->
    feedback = exec(values);
    return feedback
}
```

The fuzzer gets the list of types,
which can be provided in different formats: as a string, an object, or a Class<*> in Java.
The seed generator accepts these types and produces seeds.
The seeds are base objects for value generation and mutations.

It is the fuzzer, which is responsible for choosing, combining and mutating values from the seed set.
The fuzzer API should not provide access to the inner fuzzing logic.
Only general configuration is available.

## Parameters

The general fuzzing process gets the list of parameter descriptions as input and returns the corresponding list of values. The simplest description is the specific object type, for example:

```kotlin
[Int, Bool]
```

In this particular case, the fuzzing process can generate the set of all the pairs having integer as the first value 
and `true` or `false` as the second one.
If values `-3, 0, 10` are generated to be the `Int` values, the set of all the possible combinations has six items:
`(-3, false), (0, false), (10, false), (-3, true), (0, true), (10, true)`.
Depending on the programming language,
one may use interface descriptions or annotations (type hints) instead of defining the specific type.
Fuzzing platform (FP) is not able to create the concrete objects as it does not deal with the specific languages.
It can still convert the descriptions to the known constructs it can work with.

Say, in most of the programming languages, any integer may be represented as a bit array, and the fuzzer can construct and 
modify bit arrays. So, in the general case, the boundary values for the integer are these bit arrays:

* [0, 0, 0, ..., 0] — null
* [1, 0, 0, ..., 0] — minimum value
* [0, 1, 1, ..., 1] — maximum value
* [0, 0, ..., 0, 1] — plus 1
* [1, 1, 1, ..., 1] — minus 1

One can correctly use this representation for unsigned integers as well:

* [0, 0, 0, ..., 0] — null (minimum value)
* [1, 0, 0, ..., 0] — maximum value / 2
* [0, 1, 1, ..., 1] — maximum value / 2 + 1
* [0, 0, ..., 0, 1] — plus 1
* [1, 1, 1, ..., 1] — maximum value

Thus, FP interprets the _Byte_ and _Unsigned Byte_ descriptions in different ways: in the former case,
the maximum value is [0, 1, 1, 1, 1, 1, 1, 1], while in the latter case it is [1, 1, 1, 1, 1, 1, 1, 1].
FP types are described in detail further.

## Refined parameter description

During the fuzzing process, some parameters get the refined description, for example:

```
public boolean isNaN(Number n) {
    if (!(n instanceof Double)) {
        return false;
    }
    return Double.isNaN((Double) n);
}
```

In the above example, let the parameter be `Integer`. Considering the feedback, the fuzzer suggests that nothing but `Double` might increase coverage, so the type may be downcasted to `Double`. This allows for filtering out a priori unfitting values.

## Statically and dynamically generated values
Predefined, or _statically_ generated, values help to define the initial range of values, which could be used as method arguments.

These values allow us to:
* check if it is possible to call the given method with at least some set of values as arguments;
* gather statistics on executing the program;
* refine the parameter description.

_Dynamic_ values are generated in two ways:
* internally, via mutating the existing values, successfully performed as method arguments (i.e. seeds);
* externally, via obtaining feedback that can return not only the statistics on the execution (the paths explored, 
  the time spent, etc.) but also the set of new values to be blended with the values already in use.

Dynamic values should have a higher priority for a sample;
that is why they should be chosen either first or at least more likely than the statically generated ones.
In general, the algorithm that guides the fuzzing process looks like this:

```
# dynamic values are stored with respect to their return priority
dynamic_values = empty_priority_queue()
# static values are generated beforehand
static_values = generate()
# "good" values 
seeded_values = []
# 
filters = []

# the loop runs until coverage reaches 100%
while app.should_fuzz(seeded_values.feedbacks):
    # first we choose all dynamic values
    # if there are no dynamic values, choose the static ones
    value = dynamic_values.take() ?: static_values.take_random()
    # if there is no value or it was filtered out (static values are generated in advance — they can be random and unfitting), try to generate new values via mutating the seeds
    if value is null or filters.is_filtered(value):
        value = mutate(seeded_values.random_value())
    # if there is still no value at this point, it means that there are no appropriate values at all, and the process stops
    if value is null: break

    # run with the given values and obtain feedback
    feedback = yield_run(value)
    # feedback says if it is reasonable to add the current value to the set of seeds
    if feedback is good:
        seeded_values[feedback] += value
    # feedback may also provide fuzzer with the new values
    if feedback has suggested_value:
        dynamic_values += feedback.suggested_values() with high_priority

    # mutate the static value thus allowing fuzzer to alternate static and dynamic values
    if value.is_static_generated:
        dynamic_values += mutate(seeded_values.random_value()) with low_priority
```

## Helping fuzzer via code modification

Sometimes it is reasonable to modify the source code so that it makes applying fuzzer to it easier. This is one of possible approaches: to split the complex _if_-statement into the sequence of simpler _if_-statements. See [Circumventing Fuzzing Roadblocks with Compiler Transformations](https://lafintel.wordpress.com/2016/08/15/circumventing-fuzzing-roadblocks-with-compiler-transformations/) for details.

## Generators

There are two types of generators:
* yielding values of primitive data types: integers, strings, booleans
* yielding values of recursive data types: objects, lists

Sometimes it is necessary not only to create an object but to modify it as well. We can apply fuzzing to 
the fuzzer-generated values that should be modified. For example, you have the `HashMap.java` class, and you need to 
generate 
three 
modifications for it using `put(key, value)`. For this purpose, you may request for applying the fuzzer to six 
parameters `(key, value, key, value, key, value)` and get the necessary modified values.

Primitive type generators allow for yielding:
1. Signed integers of a given size (8, 16, 32, and 64 bits, usually)
2. Unsigned integers of a given size
3. Floating-point numbers with a given size of significand and exponent according to IEEE 754
4. Booleans: _True_ and _False_
5. Characters (in UTF-16 format)
6. Strings (consisting of UTF-16 characters)

The fuzzer should be able to provide out-of-the-box support for these types — be able to create, modify, and process 
them.
To work with multiple languages, it is enough to specify the possible type size and to describe and create 
concrete objects based on the FP-generated values.

The recursive types include two categories:
* Collections (arrays and lists)
* Objects

Collections may be nested and have _n_ dimensions (one, two, three, or more).

Collections may be:
* of a fixed size (e.g., arrays)
* of a variable size (e.g., lists and dictionaries)

Objects may have:
1. Constructors with parameters
2. Modifiable inner fields
3. Modifiable global values (the static ones)
4. Calls for modifying methods

FP should be able to create and describe such objects in the form of a tree. The semantics of actual modifications is under the responsibility of a programming language.


## Typing

FP does not use the concept of _type_ for creating objects. Instead, FP introduces the _task_ concept — it 
encapsulates the description of a type, which should be used to create an object. Generally, this task consists of two 
blocks: the task for initializing values and the list of tasks for modifying the initialized value.

```
Task = [
	Initialization: [T1, T2, T3, ..., TN]
	Modification(Initialization): [
		М1: [T1, T2, ..., TK],
		М2: [T1, T2, ..., TJ],
		МH: [T1, T2, ..., TI],
		]
]
```

Thus, we can group the tasks as follows:

```
1. Trivial task = [
	Initialization: [INT|UNSIGNED.INT|FLOAT.POINT.NUMBER|BOOLEAN|CHAR|STRING]
	Modification(Initialization): []
]


2. Task for creating an array = [
	Initialization: [UNSIGNED.INT]
	Modification(UNSIGNED.INT) = [T] * UNSIGNED.INT
]

or

2. Task for creating an array = [
	Initialization: [UNSIGNED.INT]
	Modification(UNSIGNED.INT) = [[T * UNSIGNED.INT]]
]

where "*" means repeating the type the specified number of times

3. Task for creating an object = [
	Initialization: [Т1, Т2, ... ТN],
	Modification(UNSIGNED.INT) = [
        ...
    ]
]

```

Therefore, each programming language defines how to interpret a certain type and how to infer it. This allows fuzzer 
to store and mutate complex objects without any additional support from the language.

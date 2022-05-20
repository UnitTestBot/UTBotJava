# UTBot Fuzzer

Fuzzer generates method input values to improve method coverage or find unexpected errors. In UTBot next strategies can be used to find those values:

* **Default values** for objects, e.g. 0, 0.0, empty string or null values.
* **Bound values** of primitives types, e.g. Integer.MAX_VALUE, Double.POSITIVE_INFINITY, etc.
* **Method constants and their simple mutations** of primitive types.
* **Simple objects** created via its constructors.

## Design

Fuzzer requires model providers which are a simple functions to create a set of `UtModel` for a given `ClassId`. Fuzzer iterates through these providers and creates models, which are used for generating all possible combinations. Each combination contains values for the method. For example, if a method has `String, double, int` as parameters then fuzzer can create combination `"sometext", Double.POSITIVE_INFINITY, 0`.  

Fuzzer's entry point is:
```kotlin
// org.utbot.fuzzer.FuzzerKt
fun fuzz(method: FuzzedMethodDescription, vararg models: ModelProvider): Sequence<List<UtModel>>
```

Model provider should implement

```kotlin
fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>)
```
where consumer accepts 2 values: index of a parameter for the method and model for this parameter. For every parameter should exist at least one `UtModel`. `ModelProvider.withFallback` can be used to process  those classes which cannot be processed by provider. Several providers can be combined into one by using `ModelProvider.with(anotherModel: ModelProvider)`.

Common way to generate all combinations is:

```kotlin
ObjectModelProvider()
    .with(PrimitiveModelProvider)
    // ...
    .with(NullModelProvider)
    .withFallback { classId ->
        createDefaultModelByClass(classID)
    }
```
or

```kotlin
// org.utbot.fuzzer.FuzzerKt
fun defaultModelProviders(idGenerator: ToIntFunction<ClassId>)
```

## List of builtin Providers

### PrimitiveModelProvider

Creates default values and some corner case values such as Integer.MAX_VALUE, 0.0, Double.NaN, empty string, etc.

### ConstantsModelProvider

Uses information about concrete values from `FuzzedMethodDescription#concreteValues` to generate simple values.
At the moment, only primitive values are supported.

### ObjectModelProvider

Creates models of class that has public constructor with primitives as parameters only. 

### NullModelProvider

Creates `UtNullModel` for every reference class.
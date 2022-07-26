# UTBot Fuzzer

Fuzzer generates method input values to improve method coverage or find unexpected errors. In UTBot next strategies can be used to find values like:

* **Default values** for objects, e.g. 0, 0.0, empty string or null values.
* **Corner case values** of primitives types, e.g. Integer.MAX_VALUE, Double.POSITIVE_INFINITY, etc.
* **Method constants and their simple mutations** of primitive types.
* **Objects** created via its constructors or field mutators.

After values are found fuzzer creates all its possible combinations and runs methods with these combinations.

For example, if a method has two parameters of types `boolean` and `int` the follow values can be found:
```
boolean = [false, true]
int = [0, MAX_VALUE, MIN_VALUE]
```

Now, fuzzer creates `2 * 3 = 6` combinations of them:

```
[false, 0], [false, MAX_VALUE], [false, MIN_VALUE], [true, 0], [true, MAX_VALUE], [true, MIN_VALUE]
```

To find more branches of execution as fast as possible fuzzer also shuffles
combinations and supplies them for the running.

## Design

Fuzzer requires model providers that create a set of `UtModel` for a given `ClassId`. 
Fuzzer iterates through these providers and creates models, which are used for generating combinations later. 
Each combination contains concrete values that can be accepted by the method. 
For example, if a method has signature with `String, double, int` as parameters then fuzzer can create combination `"sometext", Double.POSITIVE_INFINITY, 0`.  

Fuzzer's entry point is:
```kotlin
// org.utbot.fuzzer.FuzzerKt
fun fuzz(method: FuzzedMethodDescription, vararg models: ModelProvider): Sequence<List<FuzzedValue>>
```

`FuzzedMethodDescription` stores comprehensive information about a method:
* signature (parameters and return types)
* name/package/class (optional)
* constants found in the method body (should be replaced with CGF when possible)

`ModelProvider` provides models for a give parameters set as described below.

Fuzz method returns a sequence of acceptable values for the method in random order. The sequence is lazy.

Model provider should implement

```kotlin
fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter>
```
For every parameter should exist at least one `UtModel`. `ModelProvider.withFallback` can be used to process  those classes which cannot be processed by provider. 
Several providers can be combined into one by using `ModelProvider.with(anotherModel: ModelProvider)`.

Common way to generate all combinations is:

```kotlin
ObjectModelProvider()
    .with(PrimitiveModelProvider)
    // ...
    .with(ObjectModelProvider)
    .withFallback { classId ->
        createDefaultModelByClass(classID)
    }
```
or

```kotlin
// org.utbot.fuzzer.FuzzerKt
fun defaultModelProviders(idGenerator: IntSupplier)
```

## List of builtin providers

### PrimitiveDefaultsModelProvider

Creates default values for every primitive types:

```
boolean: false
byte: 0
short: 0
int: 0
long: 0
float: 0.0
double: 0.0
char: \u0000
string: ""
```

### PrimitiveModelProvider

Creates default values and some corner case values such as Integer.MAX_VALUE, 0.0, Double.NaN, empty string, etc.

```
boolean: false, true
byte: 0, 1, -1, Byte.MIN_VALUE, Byte.MAX_VALUE
short: 0, 1, -1, Short.MIN_VALUE, Short.MAX_VALUE
int: 0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE
long: 0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE
float: 0.0, 1.1, -1.1, Float.MIN_VALUE, Float.MAX_VALUE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN
double: 0.0, 1.1, -1.1, Double.MIN_VALUE, Double.MAX_VALUE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN
char: Char.MIN_VALUE, Char.MAX_VALUE
string: "", "   ", "string", "\n\t\r"
```

### PrimitiveWrapperModelProvider

Creates primitive models for boxed types: `Boolean`, `Byte`, `Short`, `Integer`, `Long`, `Double`, `Float`, `Character`, `String` 

### ConstantsModelProvider

Uses information about concrete values from `FuzzedMethodDescription#concreteValues` to generate simple values.
Only primitive values are supported.

### NullModelProvider

Creates `UtNullModel` for every reference class.

### EnumModelProvider

Creates models for any enum type.

### CollectionModelProvider

Creates concrete collections for collection interfaces: `List`, `Set`, `Map`, `Collection`, `Iterable`, `Iterator`

### ArrayModelProvider

Creates an empty and non-empty for any type.

### ObjectModelProvider

ObjectModelProvider is the most sophisticated provider. It creates model of class that has public constructors 
and public mutators (fields or setters/getters). If class has constructor that accepts another object within an argument that value 
is created recursively. Depth of recursion is limited to 1. Thus, for inner object fuzzing doesn't try to use every
constructor but find the one with the least number of parameters and, if it is possible, only
constructor with primitives values. If there is available only constructor with another object as a parameter then
`null` is passed to it.

Let's look at this example:

```java
class A {
    private int a;
    private Object object;
    
    public A(int a, A o) {
        this.a = a;
        this.o = o;
    }
}
```

For it fuzzing create these models:
```
new Object(0, new A(0, null));
new Object(Integer.MIN_VALUE, new A(0, null));
new Object(Integer.MAX_VALUE, new A(0, null));
new Object(0, new A(Integer.MIN_VALUE, null));
new Object(Integer.MIN_VALUE, new A(Integer.MIN_VALUE, null));
new Object(Integer.MAX_VALUE, new A(Integer.MIN_VALUE, null));
new Object(0, new A(Integer.MAX_VALUE, null));
new Object(Integer.MIN_VALUE, new A(Integer.MAX_VALUE, null));
new Object(Integer.MAX_VALUE, new A(Integer.MAX_VALUE, null));
```

For classes that have empty public constructor and field mutators all those mutators will be fuzzed as well.
Field mutators are listed below:
* public or package-private (and accessible) non-final non-static fields
* pairs of setter/getter that satisfy the common agreement:
  * setter/getter is public or package-private (and accessible)
  * have field name as a postfix, e.g.: `int myField -> * setMyField(int v)/int getMyField()`, where * means any returned type

For example, fields _a_, _b_ and _d_ will be fuzzed, but _c_ and _e_ will not:

```java
class A {
    int a;
    public char b;
    public final int c = 0;
    private String d;
    private boolean e;
    
    public A setD(String s) {
        this.d = s;
        return this;
    }
    
    public String getD() {
        return d;
    }
    
    public boolean getE() {
        return e;
    }
}
```

### Other providers

There are several other providers that can find some values, using addition information, 
like `CharToStringModelProvider` that takes all chars found in `charAt(i) == c` statement
and merge them into several strings.
# Symbolic analysis of static initializers

## Problem

Before the [Prohibit to set static fields from library classes](https://github.com/UnitTestBot/UTBotJava/pull/699) 
change was implemented, every static field outside the `<clinit>` block (the so-called _meaningful_ static fields) 
was stored in `modelBefore` and `modelAfter`. These _meaningful_ static fields were set (and reset for test isolation) during code generation. This led to explicit static field initializations, which looked unexpected for a user. For example, an `EMPTY` static field from the `Optional` class might be set for the following method under test

```java
class OptionalEmptyExample {
    public java.util.Optional<Integer> optionalExample(boolean isEmpty) {
        return isEmpty ? java.util.Optional.empty() : java.util.Optional.of(42);
    }
}
```

like:

```java
setStaticField(optionalClazz, "EMPTY", empty);
```

**Goal**: we should not set such kind of static fields with initializers.

## Current solution

Having merged [Prohibit to set static fields from library classes](https://github.com/UnitTestBot/UTBotJava/pull/699)
, we now do not explicitly set the static fields of the classes from the so-called _trusted_ libraries (by default, 
they are JDK packages). This behavior is guided by the `org.utbot.framework.
UtSettings#getIgnoreStaticsFromTrustedLibraries` setting. Current solution possibly **leads to coverage regression** 
and needs to be investigated: [Investigate coverage regression because of not setting static fields](https://github.com/UnitTestBot/UTBotJava/issues/716). 
So, take a look at other ways to fix the problem.

## Alternative solutions

### Use concrete values as soft constraints _(not yet implemented)_

The essence of the problem is assigning values to the static fields that should be set at runtime. To prevent it, 
we can try to create models for the static fields according to their runtime values and filter out the static fields 
that are equal to runtime values, using the following algorithm:

1. Extract a concrete value for a static field.
2. Create `UtModel` for this value and store it.
3. Transform the produced model to soft constraints.
4. Add them to the current symbolic state.
5. Having resolved `stateBefore`, compare the resulting `UtModel` for the static field with the stored model and then drop the resulting model from `stateBefore` if they are equal.

### Propagate information on the read static fields _(not yet implemented)_

We can define the _meaningful_ static fields in a different way: we can mark the static fields as _meaningful_ if only they affect the method-under-test result. To decide if they do:

- find out whether a given statement reads a specific static value or not and store this info,
- while traversing the method graph, propagate this stored info to each of the following statements in a tree,
- upon reaching the `return` statement of the method under test, mark all these read static fields as _meaningful_.

### Filter out static methods: check if they affect `UtExecution` _(not yet implemented)_*
Having collected all executions, we can analyze them and check whether the given static field affects the result of a current execution. Changing the static field value may have the same effect on every execution or no effect at all. It may also be required as an entry point during the executions (e.g., an _if_-statement as the first statement in the method under test):

```java
class AlwaysThrowingException {
    public void throwIfMagic() {
        if (ClassWithStaticField.staticField == 42) {
            throw new RuntimeException("Magic number");
        }
    }
}

class ClassWithStaticField {
    public final static int staticField = 42;
} 
```

*This solution should only be used with the [propagation](#propagate-information-on-the-read-static-fields) solution.
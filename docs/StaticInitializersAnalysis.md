# Symbolic analysis of static initializers

## Current behavior
### Problem
Before [Prohibit to set static fields from library classes](https://github.com/UnitTestBot/UTBotJava/pull/699), every static field touched outside **_clinit_** block (so-called **_meaningful_** static fields) was stored in **modelBefore** and **modelAfter** and accordingly was set (and correspondingly reset for tests isolation) in code generation part. Such behavior led to unexpected for user explicit static field initializations - for example, setting `EMPTY` static field from `Optional` class for the following method under test:
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

Such behavior is mostly strange for a user and should be properly fixed - we should not set such static fields with initializers manually.

### Current solution
After merging [Prohibit to set static fields from library classes](https://github.com/UnitTestBot/UTBotJava/pull/699), we do not explicitly set static fields of classes from so-called **trusted** libraries (by default represents JDK packages), accordingly to `org.utbot.framework.UtSettings#getIgnoreStaticsFromTrustedLibraries` setting. But such a solution possibly leads to coverage regression, need to be investigated [Investigate coverage regression because of not setting static fields](https://github.com/UnitTestBot/UTBotJava/issues/716). So, there are a few other ways to fix such a problem with static fields.

## Solutions

### Use concrete values as soft constraints (not yet implemented)

The essence of the problem is assigning values for static fields that would be already set in runtime. So, to prevent it we can try to create models for static fields according to theirs runtime values and filter out statics that are equal to runtime values, with the following algorithm:

1. Extract concrete value for a static field.
2. Create `UtModel` for this value and store it.
3. Transform produced model to soft constraints.
4. Add them to the current symbolic state.
5. After resolving `stateBefore` compare resulted `UtModel` for the static field with the stored model and drop resulted model out from `stateBefore` in case theirs equality.

### Propagate reading of static fields (not yet implemented)

We can change a bit sense of the **meaningful** statics - mark static fields as meaningful only if they affect a result of the method under test. To decide it, we can propagate such knowledge with the following algorithm:

1. Store for each statement whether it reads a specific static value or not.
2. While traversing a graph of the method, propagate these stores for the each statement.
3. After reaching a **return** statement of the method under test, mark all propagated static fields as meaningful.

#### Filter out statics by `UtExecution` affecting (not yet implemented) (*)

After collecting all the executions, we can analyze them and for every static field check whether this affects a result. Briefly, if the static field value never changes in all executions, it means this value does not affect the result at all and can be dropped out **OR** it is required for all executions like an entering point (if statement as a first statement in the method under test, for example):

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

(*) This solution should only be used with [propagation](#propagate-reading-of-static-fields) solution.
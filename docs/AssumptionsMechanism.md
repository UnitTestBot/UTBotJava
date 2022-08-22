# Assumptions mechanism
We have a public API that might help both us and external users to interact with UTBot. 
This document contains detailed instructions about how to use `assume` methods of `UtMock` class and description 
of the problems we encountered during the implementation.

## Brief description
This section contains short explanations of the meaning for mentioned functions and examples of usage.

### UtMock.assume(predicate)

`assume` is a method that gives the opportunity for users to say to the symbolic virtual machine that
instructions of the MUT (Method Under Test) encountered after this instruction satisfy the given predicate.
It is natively understandable concept and the closest analog from Java is the `assert` function. 
When the virtual machine during the analysis encounters such instruction, it drops all the branches in the 
control flow graph violating the predicate.

Examples: 
```java
int foo(int x) {
    // Here `x` is unbounded symbolic variable.
    // It can be any value from [MIN_VALUE..MAX_VALUE] range.
        
    UtMock.assume(x > 0);
    // Now engine will adjust the range to (0..MAX_VALUE].
        
    if (x <= 0) {
        throw new RuntimeException("Unreachable exception");    
    } else {
        return 0;
    }
}

// A function that removes all the branches with a null, empty or unsorted list.
public List<Integer> sortedList(List<Integer> a) {
    // An invariant that the list is non-null, non-empty and sorted
    UtMock.assume(a != null);
    UtMock.assume(a.size() > 0);
    UtMock.assume(a.get(0) != null);
    
    for (int i = 0; i < a.size() - 1; i++) {
        Integer element = a.get(i + 1)
        UtMock.assume(element != null);
        UtMock.assume(a.get(i) <= element);
    }
    
    return a;
}
```

Thus, `assume` is a mechanism to provide some known properties of the program to the symbolic engine.

### UtMock.assumeOrExecuteConcretely(predicate)
It is a similar concept to the `assume` with the only difference: it does not drop 
paths violating the predicate, but execute them concretely from the moment of the 
first encountered controversy. Let's take a look at the example below:

```java
int foo(List<Integer> list) {
    // Let's assume that we have small lists only
    UtMock.assume(list.size() <= 10);
    
    if (list.size() > 10) {
        throw new RuntimeException("Unreachable branch");
    } else {
        return 0;    
    }
}
```
Here we decided to take into consideration lists with sizes less or equal to 10 to improve performance, and, therefore, lost
the branch with possible exception. Let's change `assume` to `assumeOrExecuteConcretely`. 
```java
int foo(List<Integer> list) {
    // Let's assume that we have small lists only
    UtMock.assumeOrExecuteConcretely(list.size() <= 10);
    
    if (list.size() > 10) {
        throw new RuntimeException("Now we will cover this branch");
    } else {
        return 0;    
    }
}
```
Now we will cover both branches of the MUT. How did we do it? 
At the moment we processed `if` condition we got conflicting constraints: `list.size <= 10` and 
`list.size > 10`. In contrast to the example with `assume` method usage, here we know that 
we got conflict with the provided predicate and we stop symbolic analysis, remove this assumption from 
the path constraints, resolve the MUT's parameters and run the MUT using concrete execution.

Thus, `assumeOrExecuteConcretely` is a way to provide to the engine information about the program
you'd like to take into account, but if it is impossible, the engine will run the MUT concretely trying to 
cover at least something after the encountered controversy.

## Implementation
Implementation of the `assume` does not have anything interesting --
we add the predicate into path hard-constraints, and it eventually removes violating 
paths from consideration.

Processing a predicate passed as argument in `assumeOrExecuteConcretely` is more tricky. 
Due to the way we work with the solver, it cannot be added to the path constraints
directly. We treat hard PC as hypothesis and add them to the solver directly, that deprives us
opportunity to calculate unsat-core to check whether the predicate was a part of it. 

Because of it, we introduced an additional type of path constraints. Now we have three of them: 
hard, soft and assumptions. 
* Hard constraints -- properties of the program that must be satisfied at any point of the program.
* Soft constraints -- properties of the program we want to satisfy, but we can remove them if it is impossible.
For example, it might be information that some number should be less than zero. But if it is not, we still
can continue exploration of the same path without this constraint. 
* Assumptions -- predicates passed in the `assumeOrExecuteConcretely`. If we have a controversy between 
an assumption and hard constraints, we should execute the MUT concretely without violating assumption.

Now, when we check if some state is satisfiable, we put hard constraints as hypothesis into the solver
and check their consistency with soft constraints and assumptions. If the solver returns UNSAT status with 
non-empty unsat core, we remove all conflicting soft constraints from it and try again. If we have 
UNSAT status for the second time and assumptions in it, we remove them from the request and calculates 
status once again. If it is SAT, we put this state in the queue for concrete executions, otherwise -- remove the
state from consideration.

## Problems
The main problem is that we didn't get the result we expected. We have many `assume` usages 
in overridden classes source code that limits their size to improve performance. Because of that, the following 
code does not work (we don't generate any executions for them).

```java
void bigList(List<Integer> list) {
    UtMock.assume(list != null && list.size() > MAX_LIST_SIZE);
}

void bigSet(Set<Integer> set) {
    UtMock.assume(set != null && set.size() > MAX_SET_SIZE);
}
        
void bigMap(Map<Integer> map) {
    UtMock.assume(map != null && map.size() > MAX_MAP_SIZE);
}
```
The problem in a `preconditionCheck` method of the wrappers. It contains something like that:
```java
private void preconditionCheck() {
    if (alreadyVisited(this)) {
        return;
    }
    ...
    assume(elementData.end >= 0 & elementData.end <= MAX_LIST_SIZE);
    ...
}
```
It is a method that imposes restrictions at the overridden classes provided as arguments.
For `Lists` the idea works fine, we replace `assume` with `assumeOrExecuteConcretely` and
find additional branches. 
```java
private void preconditionCheck() {
    if (alreadyVisited(this)) {
        return;
    }
    ...
    assume(elementData.end >= 0);
    assumeOrExecuteConcretely(elementData.end <= MAX_LIST_SIZE);
    ...
}

// Now it works!
void bigList(List<Integer> list) {
    UtMock.assume(list != null && list.size() > MAX_LIST_SIZE);
}
```

But it doesn't work for `String`, `HashSet` and `HashMap`.

The problem with `String` is connected with the way we represent them. Restriction for the size is closely
connected with the internal storage of chars -- we create a new arrays of chars with some size `n` using `new char[n]` instruction.
It adds hard constraint that max size of the string is `n` and assumption that `n` is less that `MAX_STRING_SIZE`.
Somewhere later in the code we have a condition that `String.length() > MAX_STRING_SIZE`. Unfortunately, 
it will cause not only controversy between the assumption and new hard constraints, but and controversy
between internal array size (hard constraint) and the new-coming hard constraint, that will cause UNSAT status and 
we will lose this branch anyway.

`HashSet` and `HashMap` is a different story. The problem there is inside of `preconditionCheck` implementation. Let's take 
a look at a part of it:
```java
assume(elementData.begin == 0);
assume(elementData.end >= 0);
assumeOrExecuteConcretely(elementData.end <= 3);

parameter(elementData);
parameter(elementData.storage);
doesntThrow();

// check that all elements are distinct.
for (int i = elementData.begin; i < elementData.end; i++) {
    E element = elementData.get(i);
    parameter(element);
    // make element address non-positive

    // if key is not null, check its hashCode for exception
    if (element != null) {
        element.hashCode();
    }

    // check that there are no duplicate values
    // we can start with a next value, as all previous values are already processed
    for (int j = i + 1; j < elementData.end; j++) {
        // we use Objects.equals to process null case too
        assume(!Objects.equals(element, elementData.get(j)));
    }
}

visit(this);
```
The problem happens at the first line of the cycle. We now (from the first line of the snippet) that 
the cycle will be from zero to three. The problem is in the `i < elementData.end` check. It produces 
at the fourth iteration hard constraint that `elementData.begin + 4 < elementData.end` and we have
an assumption that `elementData.end <= 3`. It will cause a concrete run of the MUT in every 
`preconditionCheck` analysis with a constraint `elementData.end == 4`. Moreover, it still won't 
help us with code like `if (someHashSet.size() == 10)`, since we will never get here without hard
constraint `elementData.end < 4` that came from the cycle.
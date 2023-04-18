# Speculative field non-nullability assumptions

## The problem

When a field is used as a base for a dot call (i.e., a method call or a field access),
the symbolic engine creates a branch corresponding to the potential `NullPointerException`
that can occur if the value of the field is `null`. For this path, the engine generates
the hard constraint `addr(field) == null`.

If the field is marked as `@NotNull`, a hard constraint `addr(field) != null` is generated
for it. If both constraints have been generated simultaneously, the `NPE` branch is discarded
as the constraint set is unsatisfiable.

If a field does not have `@NotNull` annotation, the `NPE` branch will be kept. This behavior
is desirable, as it increases the coverage, but it has a downside. It is possible that
most of generated branches would be `NPE` branches, while useful paths could be lost due to timeout.

Beyond that, in many cases the `null` value of a field can't be generated using the public API
of the class. 

- First of all, this is particularly true for final fields, especially in system classes.
it is also often true for non-public fields from standard library and third-party libraries (even setters often do not
allow `null` values). Automatically generated tests assign `null` values to fields using reflection,
but these tests may be uninformative as the corresponding `NPE` branches would never occur
in the real code that limits itself to the public API.

- After that, field may be declared with some annotation that shows that null value is actually impossible.
For example, in Spring applications `@InjectMocks` and `@Mock` annotations on the fields of class under test
mean that these fields always have value, so `NPE` branches for them would never occur in real code.


## The solution

To discard irrelevant `NPE` branches, we can speculatively mark fields we as non-nullable even they
do not have an explicit `@NotNull` annotation. 

- In particular, we can use this approach to final and non-public
fields of system classes, as they are usually correctly initialized and are not equal `null`
- For Spring application, we use this approach for the fields of class 
under test not obtained from Spring bean definitions

At the same time, for non-Spring classes, 
we can't always add the "not null" hard constraint for the field: it would break
some special cases like `Optional<T>` class, which uses the `null` value of its final field
as a marker of an empty value.

The engine checks for NPE and creates an NPE branch every time the address is used
as a base of a dot call (i.e., a method call or a field access);
see `UtBotSymbolicEngine.nullPointerExceptionCheck`). The problem is what at that moment, we would have
no way to check whether the address corresponds to a final field, as the corresponding node
of the global graph would refer to a local variable. The only place where we have the complete
information about the field is this method.

We use the following approach. If the field belongs to a library class (according to `soot.SootClass.isLibraryClass`) 
and is final or non-public, we mark it as a speculatively non-nullable in the memory
(see `org.utbot.engine.Memory.speculativelyNotNullAddresses`). During the NPE check
we will add the `!isSpeculativelyNotNull(addr(field))` constraint
to the `NPE` branch together with the usual `addr(field) == null` constraint.

For final/non-public fields, these two conditions can't be satisfied at the same time, as we speculatively
mark such fields as non-nullable. As a result, the NPE branch would be discarded. If a field
is public or not final, the condition is satisfiable, so the NPE branch would stay alive.

We limit this approach to the library classes only, because it is hard to speculatively assume
something about non-nullability of final/non-public fields in the user code.

The same approach can be extended for other cases where we want to speculatively consider some
fields as non-nullable to prevent `NPE` branch generation.

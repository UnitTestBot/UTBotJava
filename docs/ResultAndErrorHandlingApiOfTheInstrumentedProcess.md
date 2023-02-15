# Instrumented process API: handling errors and results

In UnitTestBot Java, there are three processes:
* IDE process
* Engine process
* Instrumented process

The IDE process launches the plugin so a user can request test generation.
Upon the user request, the Engine process is initiated — it is responsible for the input values generation.

Here, in the Engine process, there is a `ConcreteExecutor` class,
conveying the generated input values to the `InstrumentedProcess` class.
The `InstrumentedProcess` class creates the third physical process —
the Instrumented process that runs the user functions concretely with the provided input values
and returns the execution result.

A _client_ is an object that uses the `ConcreteExecutor` directly — it works in the Engine process as well.

`ConcreteExecutor` expects an `Instrumentation` object, which is responsible for, say, mocking static methods. In UnitTestBot Java, `UtExecutionInstrumentation` is one of the possible `Instrumentation` interface implementations.

Basically, if an exception occurs in the Instrumented process,
it is rethrown to the client object in the Engine process via Rd.

The logic of handling errors and results depends on the provided instrumentation,
e.g., `UtExecutionInstrumentation` in our case.

## Concrete execution outcomes

When `ConcreteExecutor` is parameterized with `UtExecutionInstrumentation`
and the `ConcreteExecutor::executeAsync` is called, it leads to one of the three possible outcomes:

* `InstrumentedProcessDeathException`

Some errors lead to the instant termination of the Instrumented process.
  Such errors are wrapped in `InstrumentedProcessDeathException`.
  Prior to processing the next request, the Instrumented process is restarted automatically, though it can take time.
`InstrumentedProcessDeathException` means that there is an Instrumented process internal issue.
Nonetheless, this exception is handled in the Engine process.

* `InstrumentedProcessError`

Errors that do not cause the Instrumented process termination are wrapped in `InstrumentedProcessError`.
  The process is not restarted, so client's requests will be handled by the same process.
  We believe that the Instrumented process state is consistent but in some tricky situations it _may be not_.
  These situations should be reported as bugs.
`InstrumentedProcessError` also means
that there is an Instrumented process internal issue that should be handled by the client object
(in the Engine process).
The issue may occur because the client provides the wrong configuration or parameters,
but the Instrumented process cannot exactly determine what's wrong with the client's data:
one can find a description of the phase the exception has been thrown from.

* `UtConcreteExecutionResult`

If the Instrumented process performs well or fails because of the known wrong input,
the `UtConcreteExecutionResult` becomes relevant.
The Instrumented process guarantees that the state is _consistent_.
A `UtConcreteExecutionResult::result` field helps to find the exact reason for a failure:
* `UtSandboxFailure` — permission violation;
* `UtTimeoutException` — test execution time exceeds the provided time limit (`UtConcreteExecutionData::timeout`);
* `UtExecutionSuccess` — successful test execution;
* `UtExplicitlyThrownException` — explicitly thrown exception for a target method (via `throw` instruction);
* `UtImplicitlyThrownException` — implicitly thrown exception for a target method (`NPE`, `OOB`, etc., or an exception thrown inside the system library).

## Error handling implementation

The pipeline of `UtExecutionInstrumentation::invoke` includes 6 phases:
1. `ValueConstructionPhase` — constructs values from the models;
2. `PreparationPhase` — prepares statics, etc.;
3. `InvocationPhase` — invokes the target method;
4. `StatisticsCollectionPhase` — collects coverage and execution-related data;
5. `ModelConstructionPhase` — constructs the result models from the heap objects (`Any?`);
6. `PostprocessingPhase` — restores statics, clears mocks, etc.

Each phase can throw two kinds of exceptions:
- `ExecutionPhaseStop` — indicates that the phase tries to stop the invocation pipeline completely because it already has a result. The returned result is the `ExecutionPhaseStop::result` field.
- `ExecutionPhaseError` — indicates that an unexpected error has occurred during the phase execution, and this error is rethrown to the Engine process.

`PhasesController::computeConcreteExecutionResult` then matches on the exception type:
* it rethrows the exception if the type is `ExecutionPhaseError`,
* it returns the result if type is `ExecutionPhaseStop`.

## Timeout

Concrete execution is limited in time: the  `UtExecutionInstrumentation::invoke` method is subject to timeout as well. 

We wrap the phases that can take a long time with the `executePhaseInTimeout` block.
This block tracks the elapsed time.
If a phase wrapped with this block exceeds the timeout, it returns `TimeoutException`.

One cannot be sure
that the cancellation request immediately breaks the invocation pipeline inside the Instrumented process.
Invocation is guaranteed to finish within timeout.
It may or _may not_ finish earlier.
The request that has been sent to the Instrumented process is _uncancellable_ by design.

Even if the `TimeoutException` occurs, the Instrumented process is ready to process the new requests.

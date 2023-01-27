# Result & Error Handling API of the Instrumented Process

## Terminology

- The _instrumented process_ is an external process used for the isolated invocation.
- The `ConcreteExecutor` is a class which provides smooth and concise interaction with the _instrumented process_. It works in the _main process_.
- A client is an object which directly uses the `ConcreteExecutor`, so it works in the _main process_ as well.
- An _Instrumentation_ is an object which has to be passed to the `ConcreteExecutor`. It defines the logic of invocation and bytecode instrumentation in the _instrumented process_.

## Common

Basically, if any exception happens inside the _instrumented process_, it is rethrown to the client process via RD.
- Errors which do not cause the termination of the _instrumented process_ are wrapped in `InstrumentedProcessError`. Process won't be restarted, so client's requests will be handled by the same process. We believe, that the state of the _instrumented process_ is consistent, but in some tricky situations it **may be not**. Such situations should be reported as bugs.
- Some of the errors lead to the instant death of the _instrumented process_. Such errors are wrapped in `InstrumentedProcessDeathException`. Before processing the next request, the _instrumented process_ will be restarted automatically, but it can take some time.

The extra logic of error and result handling depends on the provided instrumentation.

## UtExecutionInstrumentation

The next sections are related only to the `UtExecutionInstrumentation` passed to the _instrumented process_.

The calling of `ConcreteExecutor::executeAsync` instantiated by the `UtExecutionInstrumentation`  can lead to the three possible situations:
- `InstrumentedProcessDeathException` occurs. Usually, this situation means there is an internal issue in the _instrumented process_, but, nevertheless, this exception should be handled by the client. 
- `InstrumentedProcessError`  occurs. It also means an internal issue and should be handled by the client. Sometimes it happens because the client provided the wrong configuration or parameters, but the _instrumented process_ **can't determine exactly** what's wrong with the client's data. The cause contains the description of the phase which threw the exception.
- No exception occurs, so the `UtConcreteExecutionResult` is returned. It means that everything went well during the invocation or something broke down because of the wrong input, and the _instrumented process_ **knows exactly** what's wrong with the client's input. The _instrumented process_ guarantees that the state **is consistent**. The exact reason of failure is a `UtConcreteExecutionResult::result` field. It includes:
	- `UtSandboxFailure` --- violation of permissions.
	- `UtTimeoutException` --- the test execution time exceeds the provided time limit (`UtConcreteExecutionData::timeout`).
	- `UtExecutionSuccess` --- the test executed successfully.
	- `UtExplicitlyThrownException` --- the target method threw exception explicitly (via `throw` instruction).
	- `UtImplicitlyThrownException` --- the target method threw exception implicitly (`NPE`, `OOB`, etc. or it was thrown inside the system library)
	- etc.

### How the error handling works

The pipeline of the `UtExecutionInstrumentation::invoke` consists of 6 phases:
-  `ValueConstructionPhase` --- constructs values from the models.
- `PreparationPhase` --- prepares statics, etc.
- `InvocationPhase` --- invokes the target method.
- `StatisticsCollectionPhase` --- collects the coverage and execution-related data.
- `ModelConstructionPhase` --- constructs the result models from the heap objects (`Any?`).
- `PostprocessingPhase` --- restores statics, clear mocks, etc.

Each phase can throw two kinds of exceptions:
- `ExecutionPhaseStop` --- indicates that the phase want to stop the invocation of the pipeline completely, because it's already has a result. The returned result is the `ExecutionPhaseStop::result` field.
- `ExecutionPhaseError` --- indicates that an unexpected error happened inside the phase execution, so it's rethrown to the main process.

The `PhasesController::computeConcreteExecutionResult` then matches on the exception type and rethrows the exception if it's an `ExecutionPhaseError`, and returns the result if it's an `ExecutionPhaseStop`.

###   Timeout

There is a time limit on the concrete execution, so the  `UtExecutionInstrumentation::invoke` method must respect it. We wrap phases which can take a long time with the `executePhaseInTimeout` block, which internally keeps and updates the elapsed time. If any wrapped phase exceeds the timeout, we return the `TimeoutException` as the result of that phase.

The clients cannot depend that cancellation request immediately breaks the invocation inside the _instrumented process_. The invocation is guaranteed to finish in the time of passed timeout. It may or **may not** finish earlier. Already started query in instrumented process is **uncancellable** - this is by design.

Even if the `TimeoutException` occurs, the _instrumented process_ is ready to process new requests.  

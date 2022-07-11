//all id values of synthetic default models must be greater that for real ones
private var nextDefaultModelId = 1500_000_000

/*
class PythonEngine(private val methodUnderTest: PythonMethod) {
    fun fuzzing(until: Long = Long.MAX_VALUE /*, modelProvider: (ModelProvider) -> ModelProvider = { it }*/) = flow {

        val methodUnderTestDescription = FuzzedMethodDescription(
            methodUnderTest.name,
            methodUnderTest.returnType,
            methodUnderTest.parameters.maps { it.getValue() },
            emptyList()
        )

        /*.apply {
            compilableName = if (methodUnderTest.isMethod) executableId.name else null
            val names = graph.body.method.tags.filterIsInstance<ParamNamesTag>().firstOrNull()?.names
            parameterNameMap = { index -> names?.getOrNull(index) }
        }
        */

        val modelProvider = modelProvider(defaultModelProviders { nextDefaultModelId++ })

        // model provider with fallback?
        // attempts?

        fuzz(methodUnderTestDescription, modelProvider /* with fallback? */ ).foreach { values ->
            modelList = values.map { it.model }
            // execute method to get function return

            // what if exception happens?

            emit(UtExecution(/* .... */))
        }
    }
}

 */
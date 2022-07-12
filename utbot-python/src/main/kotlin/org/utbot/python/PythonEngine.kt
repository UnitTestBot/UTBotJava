import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.defaultModelProviders
import org.utbot.fuzzer.fuzz
import org.utbot.python.PythonMethod

//all id values of synthetic default models must be greater that for real ones
private var nextDefaultModelId = 1500_000_000

class PythonEngine(private val methodUnderTest: PythonMethod) {
    // TODO: change sequence to flow
    fun fuzzing(until: Long = Long.MAX_VALUE /*, modelProvider: (ModelProvider) -> ModelProvider = { it }*/): Sequence<UtResult> = sequence {

        val returnType = methodUnderTest.returnType
        val argumentTypes = methodUnderTest.arguments.map { it.type }

        if (returnType == null || argumentTypes.any { it == null }) {
            return@sequence
        }

        val methodUnderTestDescription = FuzzedMethodDescription(
            methodUnderTest.name,
            returnType,
            argumentTypes.map { it!! },
            emptyList()
        )

        /*.apply {
            compilableName = if (methodUnderTest.isMethod) executableId.name else null
            val names = graph.body.method.tags.filterIsInstance<ParamNamesTag>().firstOrNull()?.names
            parameterNameMap = { index -> names?.getOrNull(index) }
        }
        */

        val modelProvider = defaultModelProviders { nextDefaultModelId++ }

        // model provider with fallback?
        // attempts?

        fuzz(methodUnderTestDescription, modelProvider /* with fallback? */ ).forEach { values ->
            val modelList = values.map { it.model }
            // execute method to get function return

            // what if exception happens?

            // emit(UtExecution(/* .... */))
        }
    }
}
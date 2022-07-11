data class PythonMethod(
    val name: String,
    val arguments,
    val body: PythonCode
)

data class PythonTestCase(
    val method: PythonMethod,
    val argument: List<UTModel>
)
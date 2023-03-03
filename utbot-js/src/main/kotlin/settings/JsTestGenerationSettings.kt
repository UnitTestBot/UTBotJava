package settings

object JsTestGenerationSettings {

    // Used for toplevel functions in IDEA plugin.
    const val dummyClassName = "toplevelHack"

    // Default timeout for Node.js to try run a single testcase.
    const val defaultTimeout = 10L

    const val fuzzingTimeout = 30_000L

    // Default timeout for any operations with npm packages
    const val npmPackageExecutorTimeout = 15L

    // Name of file under test when importing it.
    const val fileUnderTestAliases = "fileUnderTest"

    // Name of temporary files created.
    const val tempFileName = "temp"

    // Number of test cases that can fit in one temporary file for Fast coverage mode
    const val fuzzingThreshold = 300
}

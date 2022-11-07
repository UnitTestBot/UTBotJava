package settings

object TsTestGenerationSettings {

    // Used for toplevel functions in IDEA plugin.
    const val dummyClassName = "toplevelHack"

    // Default timeout for Node.js to try run a single testcase.
    const val defaultTimeout = 15L

    // Name of file under test when importing it.
    const val fileUnderTestAliases = "fileUnderTest"

    // Name of temporary files created.
    const val tempFileName = "temp"
}
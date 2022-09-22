package org.utbot.cli.util

class RunTestsCommandUtils {
    private fun getCommand(
        testFramework: String,
        mockStrategy: String,
        staticMocksOption: String,
        forceStaticMocking: String,
        withSarif: Boolean = false,
        compiledRoot: String,
        sourcesRoot: String,
        outputRoot: String,
        codegenLanguage: String,
    ): List<String> {

        val sarifFile = "$outputRoot/utbot.sarif"

        val cmd = mutableListOf(
            "-c", compiledRoot,
            "-s", sourcesRoot,
            "-o", outputRoot,
            "--test-framework", testFramework,
            "-m", mockStrategy,
            "--mock-statics", staticMocksOption,
            "-f", forceStaticMocking,
            "-l", codegenLanguage
        )
        if (withSarif)
            cmd.addAll(
                listOf(
                    "--output", compiledRoot,
                    "--sarif", sarifFile
                )
            )
        return cmd
    }
}
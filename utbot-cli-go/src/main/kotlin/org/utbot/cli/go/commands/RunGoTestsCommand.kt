package org.utbot.cli.go.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import mu.KotlinLogging
import org.utbot.cli.go.util.*
import org.utbot.go.util.convertObjectToJsonString
import java.io.File

private val logger = KotlinLogging.logger {}

class RunGoTestsCommand : CliktCommand(name = "runGo", help = "Runs tests for the specified Go package") {

    private val packageDirectory: String by option(
        "-p", "--package",
        help = "Specifies Go package to run tests for"
    )
        .required()
        .check("Must exist and be directory") {
            File(it).let { file -> file.exists() && file.isDirectory }
        }

    private val goExecutablePath: String by option(
        "-go", "--go-path",
        help = "Specifies path to Go executable. For example, it could be [/usr/local/go/bin/go] for some systems"
    )
        .required() // TODO: attempt to find it if not specified

    private val verbose: Boolean by option(
        "-v", "--verbose",
        help = "Specifies whether an output should be verbose. Is disabled by default"
    )
        .flag(default = false)

    private val json: Boolean by option(
        "-j", "--json",
        help = "Specifies whether an output should be in JSON format. Is disabled by default"
    )
        .flag(default = false)

    private val output: String? by option(
        "-o", "--output",
        help = "Specifies output file for tests run report. Prints to StdOut by default"
    )

    private enum class CoverageMode(val displayName: String) {
        REGIONS_HTML("html"), PERCENTS_BY_FUNCS("func"), REGIONS_JSON("json");

        override fun toString(): String = displayName

        val fileExtensionValidator: (String) -> Boolean
            get() = when (this) {
                REGIONS_HTML -> {
                    { it.substringAfterLast('.') == "html" }
                }

                REGIONS_JSON -> {
                    { it.substringAfterLast('.') == "json" }
                }

                PERCENTS_BY_FUNCS -> {
                    { true }
                }
            }
    }

    private val coverageMode: CoverageMode? by option(
        "-cov-mode", "--coverage-mode",
        help = StringBuilder()
            .append("Specifies whether a test coverage report should be generated and defines its mode. ")
            .append("Coverage report generation is disabled by default")
            .toString()
    )
        .choice(
            CoverageMode.REGIONS_HTML.toString() to CoverageMode.REGIONS_HTML,
            CoverageMode.PERCENTS_BY_FUNCS.toString() to CoverageMode.PERCENTS_BY_FUNCS,
            CoverageMode.REGIONS_JSON.toString() to CoverageMode.REGIONS_JSON,
        )
        .check(
            StringBuilder()
                .append("Test coverage report output file must be set ")
                .append("and have an extension that matches the coverage mode")
                .toString()
        ) { mode ->
            coverageOutput?.let { mode.fileExtensionValidator(it) } ?: false
        }

    private val coverageOutput: String? by option(
        "-cov-out", "--coverage-output",
        help = "Specifies output file for test coverage report. Required if [--coverage-mode] is set"
    )
        .check("Test coverage report mode must be specified") {
            coverageMode != null
        }

    override fun run() {
        val runningTestsStarted = now()
        try {
            logger.debug { "Running tests for [$packageDirectory] - started" }

            /* run tests */

            val packageDirectoryFile = File(packageDirectory).canonicalFile

            val coverProfileFile = if (coverageMode != null) {
                createFile(createCoverProfileFileName())
            } else {
                null
            }

            try {
                val runGoTestCommand = mutableListOf(
                    goExecutablePath.toAbsolutePath().toString(),
                    "test",
                    "./"
                )
                if (verbose) {
                    runGoTestCommand.add("-v")
                }
                if (json) {
                    runGoTestCommand.add("-json")
                }
                if (coverageMode != null) {
                    runGoTestCommand.add("-coverprofile")
                    runGoTestCommand.add(coverProfileFile!!.canonicalPath)
                }

                val outputStream = if (output == null) {
                    System.out
                } else {
                    createFile(output!!).outputStream()
                }
                executeCommandAndRedirectStdoutOrFail(runGoTestCommand, packageDirectoryFile, outputStream)

                /* generate coverage report */

                val coverageOutputFile = coverageOutput?.let { createFile(it) } ?: return

                when (coverageMode) {
                    null -> {
                        return
                    }

                    CoverageMode.REGIONS_HTML, CoverageMode.PERCENTS_BY_FUNCS -> {
                        val runToolCoverCommand = mutableListOf(
                            "go",
                            "tool",
                            "cover",
                            "-${coverageMode!!.displayName}",
                            coverProfileFile!!.canonicalPath,
                            "-o",
                            coverageOutputFile.canonicalPath
                        )
                        executeCommandAndRedirectStdoutOrFail(runToolCoverCommand, packageDirectoryFile)
                    }

                    CoverageMode.REGIONS_JSON -> {
                        val coveredSourceFiles = parseCoverProfile(coverProfileFile!!)
                        val jsonCoverage = convertObjectToJsonString(coveredSourceFiles)
                        coverageOutputFile.writeText(jsonCoverage)
                    }
                }
            } finally {
                coverProfileFile?.delete()
            }
        } catch (t: Throwable) {
            logger.error { "An error has occurred while running tests for [$packageDirectory]: $t" }
            throw t
        } finally {
            val duration = durationInMillis(runningTestsStarted)
            logger.debug { "Running tests for [$packageDirectory] - completed in [$duration] (ms)" }
        }
    }

    private fun createCoverProfileFileName(): String {
        return "ut_go_cover_profile.out"
    }

    private fun parseCoverProfile(coverProfileFile: File): List<CoveredSourceFile> {
        data class CoverageRegions(
            val covered: MutableList<CodeRegion>,
            val uncovered: MutableList<CodeRegion>
        )

        val coverageRegionsBySourceFilesNames = mutableMapOf<String, CoverageRegions>()

        coverProfileFile.readLines().asSequence()
            .drop(1) // drop "mode" value
            .forEach { fullLine ->
                val (sourceFileFullName, coverageInfoLine) = fullLine.split(":", limit = 2)
                val sourceFileName = sourceFileFullName.substringAfterLast("/")
                val (regionString, _, countString) = coverageInfoLine.split(" ", limit = 3)

                fun parsePosition(positionString: String): Position {
                    val (lineNumber, columnNumber) = positionString.split(".", limit = 2).asSequence()
                        .map { it.toInt() }
                        .toList()
                    return Position(lineNumber, columnNumber)
                }
                val (startString, endString) = regionString.split(",", limit = 2)
                val region = CodeRegion(parsePosition(startString), parsePosition(endString))

                val regions = coverageRegionsBySourceFilesNames.getOrPut(sourceFileName) {
                    CoverageRegions(
                        mutableListOf(),
                        mutableListOf()
                    )
                }
                // it is called "count" in docs, but in reality it is like boolean for covered / uncovered
                val count = countString.toInt()
                if (count == 0) {
                    regions.uncovered.add(region)
                } else {
                    regions.covered.add(region)
                }
            }

        return coverageRegionsBySourceFilesNames.map { (sourceFileName, regions) ->
            CoveredSourceFile(sourceFileName, regions.covered, regions.uncovered)
        }
    }
}
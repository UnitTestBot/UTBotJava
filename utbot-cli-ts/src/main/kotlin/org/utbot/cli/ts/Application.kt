package org.utbot.cli.ts

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.enum
import org.slf4j.event.Level
import org.utbot.cli.getVersion
import org.utbot.cli.setVerbosity
import kotlin.system.exitProcess

class UtBotJsCli : CliktCommand(name = "UnitTestBot TypeScript Command Line Interface") {
    private val verbosity by option("--verbosity", help = "Changes verbosity level, case insensitive")
        .enum<Level>(ignoreCase = true)
        .default(Level.INFO)

    override fun run() = setVerbosity(verbosity)

    init {
        versionOption(getVersion())
    }
}

fun main(args: Array<String>) = try {
    UtBotJsCli().subcommands(
        TsGenerateTestsCommand(),
    ).main(args)
} catch (ex: Throwable) {
    ex.printStackTrace()
    exitProcess(1)
}

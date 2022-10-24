package org.utbot.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.enum
import java.util.Properties
import kotlin.system.exitProcess
import mu.KotlinLogging
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator
import org.slf4j.event.Level

private val logger = KotlinLogging.logger {}

class UtBotCli : CliktCommand(name = "UnitTestBot Java Command Line Interface") {
    private val verbosity by option("--verbosity", help = "Changes verbosity level, case insensitive")
        .enum<Level>(ignoreCase = true)
        .default(Level.INFO)

    override fun run() = setVerbosity(verbosity)

    init {
        versionOption(getVersion())
    }
}

fun main(args: Array<String>) = try {
    UtBotCli().subcommands(GenerateTestsCommand(), BunchTestGeneratorCommand(), RunTestsCommand()).main(args)
} catch (ex: Throwable) {
    ex.printStackTrace()
    exitProcess(1)
}

fun setVerbosity(verbosity: Level) {
    Configurator.setAllLevels(LogManager.getRootLogger().name, level(verbosity))
    logger.debug { "Log Level changed to [$verbosity]" }
}

private fun level(level: Level) = org.apache.logging.log4j.Level.toLevel(level.name)

fun getVersion(): String {
    val prop = Properties()
    Thread.currentThread().contextClassLoader.getResourceAsStream("version.properties")
        .use { stream ->
            prop.load(stream)
        }
    return prop.getProperty("version")
}
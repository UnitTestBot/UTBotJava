package org.utbot.intellij.plugin.process

import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.utbot.common.utBotTempDirectory
import org.utbot.framework.UtSettings
import org.utbot.intellij.plugin.util.assertReadAccessNotAllowed
import org.utbot.rd.ProcessWithRdServer
import org.utbot.rd.exceptions.InstantProcessDeathException
import org.utbot.rd.onSchedulerBlocking
import org.utbot.rd.startBlocking
import org.utbot.spring.process.generated.SpringAnalyzerParams
import org.utbot.spring.process.generated.SpringAnalyzerProcessModel
import org.utbot.spring.process.generated.springAnalyzerProcessModel
import java.io.File
import java.nio.file.Files

class SpringAnalyzerProcessInstantDeathException :
    InstantProcessDeathException(UtSettings.springAnalyzerProcessDebugPort, UtSettings.runSpringAnalyzerProcessWithDebug)

private const val SPRING_ANALYZER_JAR_FILENAME = "utbot-spring-analyzer-shadow.jar"

class SpringAnalyzerProcess private constructor(
    rdProcess: ProcessWithRdServer
) : AbstractProcess(rdProcess) {

    companion object : AbstractProcess.Companion<Unit, SpringAnalyzerProcess>(
        displayName = "Spring analyzer",
        logConfigFileGetter = { UtSettings.springAnalyzerProcessLogConfigFile },
        debugPortGetter = { UtSettings.springAnalyzerProcessDebugPort },
        runWithDebugGetter = { UtSettings.runSpringAnalyzerProcessWithDebug },
        suspendExecutionInDebugModeGetter = { UtSettings.suspendSpringAnalyzerProcessExecutionInDebugMode },
        logConfigurationsDirectory = utBotTempDirectory.toFile().resolve("rdSpringAnalyzerProcessLogConfigurations"),
        logDirectory = utBotTempDirectory.toFile().resolve("rdSpringAnalyzerProcessLogs"),
        logConfigurationFileDeleteKey = "spring_analyzer_process_appender_comment_key",
        logAppender = "SpringAnalyzerProcessAppender",
        currentLogFilename = "utbot-spring-analyzer-current.log",
        logger = KotlinLogging.logger {}
    ) {
        override fun obtainProcessSpecificCommandLineArgs(): List<String> {
            val jarFile =
                Files.createDirectories(utBotTempDirectory.toFile().resolve("spring-analyzer").toPath())
                    .toFile().resolve(SPRING_ANALYZER_JAR_FILENAME)
            FileUtils.copyURLToFile(
                this::class.java.classLoader.getResource("lib/$SPRING_ANALYZER_JAR_FILENAME"),
                jarFile
            )
            return listOf("-jar", jarFile.path)
        }

        override fun getWorkingDirectory(): File =
            Files.createTempDirectory(utBotTempDirectory, "spring-analyzer").toFile()

        override fun createFromRDProcess(params: Unit, rdProcess: ProcessWithRdServer) =
            SpringAnalyzerProcess(rdProcess)

        override fun createInstantDeathException() = SpringAnalyzerProcessInstantDeathException()
    }

    private val springAnalyzerModel: SpringAnalyzerProcessModel = onSchedulerBlocking { protocol.springAnalyzerProcessModel }

    fun getBeanQualifiedNames(
        classpath: List<String>,
        configuration: String,
        propertyFilesPaths: List<String>,
        xmlConfigurationPaths: List<String>
    ): List<String> {
        assertReadAccessNotAllowed()
        val params = SpringAnalyzerParams(
            classpath.toTypedArray(),
            configuration,
            propertyFilesPaths.toTypedArray(),
            xmlConfigurationPaths.toTypedArray()
        )
        val result = springAnalyzerModel.analyze.startBlocking(params)
        return result.beanTypes.toList()
    }
}

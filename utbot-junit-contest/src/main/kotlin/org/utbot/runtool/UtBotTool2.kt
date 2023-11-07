package org.utbot.runtool

import org.utbot.contest.ContestMessage
import java.io.BufferedReader
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import sbst.runtool.ITestingTool

class UtBotTool2 : ITestingTool {

    override fun getExtraClassPath(): List<File> {
        return mutableListOf(File("lib", "mockito-core-4.11.0.jar"))
    }

    lateinit var sootClasspathString: String
    lateinit var classfileDir: String

    var utbotProcess: Process? = null


    override fun initialize(src: File, bin: File, classPath: List<File>) {
        classfileDir = bin.absolutePath
        sootClasspathString =
            classfileDir + File.pathSeparator + classPath.joinToString(separator = File.pathSeparator) { it.absolutePath }


        tempDir.listFiles { _, name -> name.endsWith(".log") }?.forEach { it.delete() }

        restartProcess()

        while (true) {
            val line = inputChannel.readLine()
            if (line == null) {
                log("INIT FAILED, INPUT LINE IS <NULL>")
                break
            }

            logUtbotOut(line)
            if (line.startsWith("${ContestMessage.INIT}"))
                break
        }
    }

    lateinit var inputChannel: BufferedReader
    lateinit var outputChannel: PrintStream


    var restartOrdinal = 0
    val homeDir = File(".").canonicalFile
    val tempDir = File(homeDir, "temp")

    private fun log(msg: String, ex: Throwable? = null) {
        val logfile = File(tempDir, "utbot-runtool.log")
        logfile.appendText(msg + "\n")

        val sw = StringWriter()
        ex?.printStackTrace(PrintWriter(sw, true))
        logfile.appendText(sw.toString() + "\n")
    }


    private fun logUtbotOut(msg: String) {
        val logfile = File(tempDir, "utbot-${restartOrdinal}-out.log")
        logfile.appendText(msg + "\n")
    }

    private fun restartProcess() {
        restartOrdinal++
        val prevProcess = utbotProcess

        try {
            if (prevProcess?.isAlive == true) {
                prevProcess.destroyForcibly()
            }
        } catch (e: Throwable) {
            log("FAILED TO TERMINATED PREVIOUS UTBOT", e)
        }


        //TODO I think we have some strange problems with z3, this addition must absent
        val additionalUnpackedZ3 = File("utbot-framework/build/resources/main").run {
            if (isDirectory && exists())
                File.pathSeparator + absolutePath
            else
                ""
        }
        val utBotAppClasspath = System.getProperty("java.class.path") +
                File.pathSeparator + sootClasspathString +
                additionalUnpackedZ3


        val generatedTestsOutputDir = File(tempDir, "testcases")
        val errorFile = File(tempDir, "utbot-${restartOrdinal}-err.log")
        errorFile.parentFile.mkdirs()

        val pb = ProcessBuilder(
            listOfNotNull(
                "${System.getenv("JAVA_HOME")}/bin/java",
                System.getenv("UTBOT_EXTRA_PARAMS"),
                "-cp",
                utBotAppClasspath,
                "org.utbot.contest.ContestKt",
                classfileDir,
                sootClasspathString,
                generatedTestsOutputDir.canonicalPath
            )
        ).directory(homeDir).redirectError(errorFile)

        val newProcess = pb.start()
        log("Started process (ordinal=$restartOrdinal): \n\t" + pb.command().joinToString("\n\t"))


        utbotProcess = newProcess

        inputChannel = newProcess.inputStream.reader().buffered()
        outputChannel = PrintStream(newProcess.outputStream, true)
    }


    override fun run(cName: String, timeBudget: Long) {
        val start = System.currentTimeMillis()
        try {
            log("Started RUN  $cName $timeBudget")
            if (utbotProcess?.isAlive != true) {
                restartProcess()
            }

            outputChannel.println("${ContestMessage.RUN} $cName $timeBudget")
            while (true) {
                val line = inputChannel.readLine()
                if (line == null) {
                    log("INPUT FROM UTBOT IS NULL, RESTARTING UTBOT")
                    restartProcess()
                    break
                }
                logUtbotOut(line)

                if (line.startsWith("${ContestMessage.READY}"))
                    break
                else
                    continue
            }
        } catch (e: Throwable) {
            log("ERROR HAPPENED, RESTARTING UTBOT", e)
            restartProcess()
        } finally {
            log("Finished RUN (elapsed ${System.currentTimeMillis() - start} ms)  $cName")
        }
    }
}
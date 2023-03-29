package org.utbot.rider

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.createLifetime
import com.jetbrains.rd.platform.util.idea.ProtocolSubscribedProjectComponent
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.ISignal
import com.jetbrains.rider.projectView.solution
import org.utbot.rider.generated.UtBotRiderModel
import org.utbot.rider.generated.utBotRiderModel
import kotlin.random.Random

private fun ConsoleView.printYellowLine(any: Any) {
    print("$any\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
}

private fun ConsoleView.printNormalLine(any: Any) {
    print("$any\n", ConsoleViewContentType.NORMAL_OUTPUT)
}

private fun ConsoleView.printErrorLine(any: Any) {
    print("$any\n", ConsoleViewContentType.ERROR_OUTPUT)
}

private fun ISignal<Int>.advisePrintExitCode(lifetime: Lifetime, consoleView: ConsoleView) {
    advise(lifetime) { exitCode ->
        val msg = "Process exited with code: $exitCode"
        if (exitCode == 0)
            consoleView.printNormalLine(msg)
        else
            consoleView.printErrorLine(msg)
    }
}

private fun ISignal<String>.advisePrintNormal(lifetime: Lifetime, consoleView: ConsoleView) {
    advise(lifetime) { output ->
        consoleView.printNormalLine(output)
    }
}

private fun ISignal<String>.advisePrintError(lifetime: Lifetime, consoleView: ConsoleView) {
    advise(lifetime) { output ->
        consoleView.printErrorLine(output)
    }
}

class UtBotConsoleViewComponent(project: Project) : ProtocolSubscribedProjectComponent(project) {
    private var currentExecutionId: Long = -1
    private val model: UtBotRiderModel

    private fun update(
        firstLine: String,
        displayName: String,
        previous: RunContentDescriptor?
    ): Triple<RunContentDescriptor, ConsoleView, Lifetime> {
        val textConsoleBuilderFactory = TextConsoleBuilderFactory.getInstance()
        val consoleView = textConsoleBuilderFactory.createBuilder(project).console
        val runContentDescriptor =
            RunContentDescriptor(consoleView, null, consoleView.component, displayName).apply {
                executionId = currentExecutionId
            }
        val runContentManager = RunContentManager.getInstance(project)

        runContentManager.showRunContent(
            DefaultRunExecutor.getRunExecutorInstance(),
            runContentDescriptor,
            previous
        )
        consoleView.printYellowLine(firstLine)

        return Triple(runContentDescriptor, consoleView, consoleView.createLifetime())
    }

    private fun initPublish() {
        var previousPublish: RunContentDescriptor? = null
        model.startPublish.advise(project.lifetime) { publishArgs ->
            currentExecutionId = Random.nextLong()
            val (fileName, arguments, workingDirectory) = publishArgs
            val firstLine = "$workingDirectory .> $fileName $arguments"
            val (currentPublish, consoleView, consoleLifetime) = update(
                firstLine,
                "Project publish for UtBot",
                previousPublish
            )
            previousPublish = currentPublish
            model.logPublishOutput.advisePrintNormal(consoleLifetime, consoleView)
            model.logPublishError.advisePrintError(consoleLifetime, consoleView)
            model.stopPublish.advisePrintExitCode(consoleLifetime, consoleView)
        }
    }

    init {
        model = project.solution.utBotRiderModel
        initPublish()
        initVSharp()
    }

    private fun initVSharp() {
        var previousVSharp: RunContentDescriptor? = null
        model.startVSharp.advise(project.lifetime) {
            val (currentVSharp, consoleView, consoleLifetime) = update(
                "Started VSharp Engine",
                "Running VSharp Engine",
                previousVSharp
            )
            previousVSharp = currentVSharp
            model.logVSharp.advisePrintNormal(consoleLifetime, consoleView)
            model.stopVSharp.advisePrintExitCode(consoleLifetime, consoleView)
        }
    }
}
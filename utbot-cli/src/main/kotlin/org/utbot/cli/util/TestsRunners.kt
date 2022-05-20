package org.utbot.cli.util

import org.utbot.cli.writers.IWriter
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.runner.JUnitCore
import org.testng.TestListenerAdapter
import org.testng.TestNG

fun junit4RunTests(classWithTests: Class<*>, writer: IWriter) {
    val junit = JUnitCore()
    val result = junit.run(classWithTests)
    val testsRunInfo = "Tests run: ${result.runCount}\n" +
            "Succeeded tests: ${result.runCount - result.failureCount}\n" +
            "Failed tests: ${result.failureCount}\n" +
            (if (result.failureCount > 0) {
                "\n***FAILED TESTS INFO***\n" +
                        result.failures.joinToString("\n") {
                            "Test header: ${it.testHeader}\n" +
                                    "Exception: ${it.exception}\n" +
                                    "Message: ${it.message}\n" +
                                    "Description: ${it.description}\n"
                        } + "\n"
            } else "") +
            "Ignored tests: ${result.ignoreCount}\n" +
            "Run tests: ${if (result.wasSuccessful()) "SUCCEEDED\n" else "FAILED\n"}" +
            "Running the entire test suite - completed in ${result.runTime} (ms)\n"

    writer.append(testsRunInfo)
}

fun junit5RunTests(classWithTests: Class<*>, writer: IWriter) {
    val listener = SummaryGeneratingListener()
    val request: LauncherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectClass(classWithTests))
        .build()
    val launcher = LauncherFactory.create()
    launcher.registerTestExecutionListeners(listener)
    launcher.execute(request)
    val summary = listener.summary

    val testsRunInfo = "Tests found: ${summary.testsFoundCount}\n" +
            "Started tests: ${summary.testsStartedCount}\n" +
            "Succeeded tests: ${summary.testsSucceededCount}\n" +
            "Failed tests: ${summary.testsFailedCount}\n" +
            (if (summary.testsFailedCount > 0) {
                "\n***FAILED TESTS INFO***\n" +
                        summary.failures.joinToString("\n") {
                            "Test identifier: ${it.testIdentifier}\n" +
                                    "Exception: ${it.exception}\n"
                        } + "\n"
            } else "") +
            "Skipped tests: ${summary.testsSkippedCount}\n" +
            "Aborted tests: ${summary.testsAbortedCount}\n" +
            "Running the entire test suite - completed in [${summary.timeFinished - summary.timeStarted}] (ms)\n"

    writer.append(testsRunInfo)
}

fun testngRunTests(classWithTests: Class<*>, writer: IWriter) {
    val listener = TestListenerAdapter()
    val testng = TestNG()
    testng.setTestClasses(arrayOf(classWithTests))
    testng.addListener(listener)
    testng.run()

    val testsRunInfo = "Succeeded tests: ${listener.passedTests.size}\n" +
            (if (listener.failedTests.size > 0) {
                "Failed tests: ${listener.failedTests.size}\n" +

                        "\n***FAILED TESTS INFO***\n" +
                        listener.failedTests.joinToString("\n") {
                            "Test class: ${it.testClass}\n" +
                                    "Test name: ${it.name}\n" +
                                    "Method: ${it.method}\n" +
                                    "Time (ms): [${it.endMillis - it.startMillis}]\n"
                        } + "\n"
            } else "") +
            "Skipped tests: ${listener.skippedTests.size}\n"

    writer.append(testsRunInfo)
}
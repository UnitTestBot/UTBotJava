package org.utbot.tests

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.waitForIgnoringError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.utbot.data.IdeaBuildSystem
import org.utbot.data.JDKVersion
import org.utbot.data.NEW_PROJECT_NAME_START
import org.utbot.pages.*
import org.utbot.samples.additionFunction
import java.time.Duration.ofSeconds

class UnitTestBotActionTest : BaseTest() {

    @ParameterizedTest(name = "Generate tests in {0} project with JDK {1}")
    @MethodSource("supportedProjectsProvider")
    @Tags(Tag("Java"), Tag("UnitTestBot"), Tag("Positive"))
    fun basicTestGeneration(ideaBuildSystem: IdeaBuildSystem, jdkVersion: JDKVersion,
                                               remoteRobot: RemoteRobot) : Unit = with(remoteRobot) {
        val createdProjectName = NEW_PROJECT_NAME_START + ideaBuildSystem.system + jdkVersion.number
        remoteRobot.welcomeFrame {
            findText(createdProjectName).click()
        }
        val ideaFrame = getIdeaFrameForBuildSystem(remoteRobot, ideaBuildSystem)
        with (ideaFrame) {
            val newClassName = "Addition"
            createNewJavaClass(newClassName, "org.example")
            textEditor().additionFunction(newClassName)
            openUTBotDialogFromProjectViewForClass(newClassName)
            unitTestBotDialog.generateTestsButton.click()
            waitForIgnoringError (ofSeconds(10)){
                inlineProgressTextPanel.isShowing
            }
            waitForIgnoringError(ofSeconds(30)) {
                inlineProgressTextPanel.hasText("Generate tests: read classes")
            }
            waitForIgnoringError (ofSeconds(10)){
                inlineProgressTextPanel.hasText("Generate test cases for class $newClassName")
            }
            waitForIgnoringError(ofSeconds(30)) { //Can be changed to 60 for a complex class
                infoNotification.isShowing
            }
            assertThat(infoNotification.title.hasText("UnitTestBot: unit tests generated successfully")).isTrue
            assertThat(textEditor().editor.text).contains("class ${newClassName}Test")
            assertThat(textEditor().editor.text).contains("@Test\n")
        }
    }

    @ParameterizedTest(name = "Check Generate tests button is disabled in {0} project with unsupported JDK {1}")
    @MethodSource("unsupportedProjectsProvider")
    @Tags(Tag("Java"), Tag("UnitTestBot"), Tag("Negative"))
    fun checkTestGenerationIsUnavailable(ideaBuildSystem: IdeaBuildSystem, jdkVersion: JDKVersion,
                                         remoteRobot: RemoteRobot) : Unit = with(remoteRobot) {
        val createdProjectName = NEW_PROJECT_NAME_START + ideaBuildSystem.system + jdkVersion.number
        remoteRobot.welcomeFrame {
            findText(createdProjectName).click()
        }
        val ideaFrame = getIdeaFrameForBuildSystem(remoteRobot, ideaBuildSystem)
        with (ideaFrame) {
            val newClassName = "Addition"
            createNewJavaClass(newClassName, "org.example")
            textEditor().additionFunction(newClassName)
            openUTBotDialogFromProjectViewForClass(newClassName)
            assertThat(unitTestBotDialog.generateTestsButton.isEnabled().not())
            unitTestBotDialog.generateTestsButton.click()
            waitForIgnoringError (ofSeconds(3)){
                inlineProgressTextPanel.isShowing.not()
            }
        }
    }
}
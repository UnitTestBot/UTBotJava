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
import org.utbot.samples.typeAdditionFunction
import org.utbot.samples.typeDivisionFunction
import java.time.Duration.ofSeconds

class UnitTestBotActionTest : BaseTest() {

    @ParameterizedTest(name = "Generate tests in {0} project with JDK {1}")
    @MethodSource("supportedProjectsProvider")
    @Tags(Tag("Java"), Tag("UnitTestBot"), Tag("Positive"))
    fun checkBasicTestGeneration(ideaBuildSystem: IdeaBuildSystem, jdkVersion: JDKVersion,
                                 remoteRobot: RemoteRobot) {
        val createdProjectName = NEW_PROJECT_NAME_START + ideaBuildSystem.system + jdkVersion.number
        remoteRobot.welcomeFrame {
            findText(createdProjectName).click()
        }
        val ideaFrame = getIdeaFrameForBuildSystem(remoteRobot, ideaBuildSystem)
        with (ideaFrame) {
            val newClassName = "Arithmetic"
            createNewJavaClass(newClassName, "Main")
            val returnsFromTagBody = textEditor().typeDivisionFunction(newClassName)
            openUTBotDialogFromProjectViewForClass(newClassName)
            unitTestBotDialog.generateTestsButton.click()
            waitForIgnoringError (ofSeconds(5)){
                inlineProgressTextPanel.isShowing
            }
            waitForIgnoringError (ofSeconds(90)){
                inlineProgressTextPanel.hasText("Generate test cases for class $newClassName")
            }
            waitForIgnoringError(ofSeconds(30)) {
                utbotNotification.title.hasText("UnitTestBot: unit tests generated successfully")
            }
            assertThat(textEditor().editor.text).contains("class ${newClassName}Test")
            assertThat(textEditor().editor.text).contains("@Test\n")
            assertThat(textEditor().editor.text).contains("assertEquals(")
            assertThat(textEditor().editor.text).contains("@utbot.classUnderTest {@link ${newClassName}}")
            assertThat(textEditor().editor.text).contains("@utbot.methodUnderTest {@link ${newClassName}#")
            assertThat(textEditor().editor.text).contains(returnsFromTagBody)
            //ToDo verify how many tests are generated
            //ToDo verify Problems view and Arithmetic exception on it
        }
    }

    @ParameterizedTest(name = "Check Generate tests button is disabled in {0} project with unsupported JDK {1}")
    @MethodSource("unsupportedProjectsProvider")
    @Tags(Tag("Java"), Tag("UnitTestBot"), Tag("Negative"))
    fun checkProjectWithUnsupportedJDK(ideaBuildSystem: IdeaBuildSystem, jdkVersion: JDKVersion,
                                       remoteRobot: RemoteRobot) {
        val createdProjectName = NEW_PROJECT_NAME_START + ideaBuildSystem.system + jdkVersion.number
        remoteRobot.welcomeFrame {
            findText(createdProjectName).click()
        }
        val ideaFrame = getIdeaFrameForBuildSystem(remoteRobot, ideaBuildSystem)
        return with (ideaFrame) {
            val newClassName = "Arithmetic"
            createNewJavaClass(newClassName, "Main")
            textEditor().typeAdditionFunction(newClassName)
            openUTBotDialogFromProjectViewForClass(newClassName)
            assertThat(unitTestBotDialog.generateTestsButton.isEnabled().not())
            assertThat(unitTestBotDialog.sdkNotificationLabel.hasText("SDK version 19 is not supported, use 1.8, 11 or 17 instead."))
            assertThat(unitTestBotDialog.setupSdkLink.isShowing)
            unitTestBotDialog.closeButton.click()
        }
    }
}
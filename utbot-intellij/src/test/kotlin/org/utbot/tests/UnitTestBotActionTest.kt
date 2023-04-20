package org.utbot.tests

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitForIgnoringError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.utbot.data.IdeaBuildSystem
import org.utbot.data.JDKVersion
import org.utbot.data.NEW_PROJECT_NAME_START
import org.utbot.data.TEST_RUN_NUMBER
import org.utbot.pages.*
import java.time.Duration

@Order(2)
class UnitTestBotActionTest : UTBotTest() {

    @ParameterizedTest(name = "Run UTBot action on a new Java class in existing {0} project")
    @MethodSource("projectListProvider")
    @Tags(Tag("Java"), Tag("UTBot"), Tag("NewClass"))
    fun runActionOnNewJavaClassInExistingProject(ideaBuildSystem: IdeaBuildSystem, jdkVersion: JDKVersion,
                                                 remoteRobot: RemoteRobot) : Unit = with(remoteRobot) {
        val createdProjectName = NEW_PROJECT_NAME_START + ideaBuildSystem.system + jdkVersion.number
        remoteRobot.welcomeFrame {
            findText(createdProjectName).click()
        }
        val ideaFrame = getIdeaFrameForSpecificBuildSystem(remoteRobot, ideaBuildSystem)
        with (ideaFrame) {
            waitProjectIsOpened()
            expandProjectTree(projectName)
            val newClassName = "Example"
            createNewJavaClass(newClassName)
            with(textEditor()) {
                editor.selectText(newClassName)
                keyboard {
                    key(java.awt.event.KeyEvent.VK_END)
                    enter()
                }
                keyboard {
                    enterText("public int function(")
                    enterText("int a, int b")
                    key(java.awt.event.KeyEvent.VK_END)
                    enterText("{")
                    enter()
                    enterText("// UTBot UI ${TEST_RUN_NUMBER} test. ${ideaBuildSystem.system} project. ${jdkVersion.number} JDK")
                    enter()
                    enterText("return a + b;")
                }
            }
            callUnitTestBotActionOn(newClassName)
            waitForIgnoringError (Duration.ofSeconds(10)){
                inlineProgressTextPanel.isShowing
            }
            waitForIgnoringError(Duration.ofSeconds(30)) {
                inlineProgressTextPanel.hasText("Generate tests: read classes")
            }
            waitForIgnoringError (Duration.ofSeconds(10)){
                inlineProgressTextPanel.hasText("Generate test cases for class $newClassName")
            }
            waitForIgnoringError(Duration.ofSeconds(30)) { //Can be changed to 60 for a complex class
                infoNotification.isShowing
            }
            assertThat(infoNotification.title.hasText("UnitTestBot: unit tests generated successfully")).isTrue
            assertThat(textEditor().editor.text).contains("class ${newClassName}Test")
            assertThat(textEditor().editor.text).contains("@Test\n")
        }
    }
}
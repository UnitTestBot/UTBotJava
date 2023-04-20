package org.utbot.pages

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import com.intellij.remoterobot.utils.waitForIgnoringError
import org.assertj.swing.core.MouseButton
import org.utbot.data.IdeaBuildSystem
import org.utbot.dialogs.UnitTestBotDialogFixture
import org.utbot.elements.NotificationFixture
import java.awt.event.KeyEvent
import java.time.Duration
import java.time.Duration.ofSeconds

fun RemoteRobot.idea(function: IdeaFrame.() -> Unit) {
    find<IdeaFrame>(timeout = ofSeconds(5)).apply(function)
}

@FixtureName("Idea frame")
@DefaultXpath("IdeFrameImpl type", "//div[@class='IdeFrameImpl']")
open class IdeaFrame(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : CommonContainerFixture(remoteRobot, remoteComponent) {

    open val buildSystemToUse: IdeaBuildSystem = IdeaBuildSystem.INTELLIJ

    val projectViewTree
        get() = find<ContainerFixture>(byXpath("ProjectViewTree", "//div[@class='ProjectViewTree']"),
            ofSeconds(10))

    val projectName
        get() = step("Get project name") { return@step callJs<String>("component.getProject().getName()") }

    val menuBar: JMenuBarFixture
        get() = step("Menu...") {
            return@step remoteRobot.find(JMenuBarFixture::class.java, JMenuBarFixture.byType())
        }

    val unitTestBotDialog
        get() = remoteRobot.find(UnitTestBotDialogFixture::class.java,
            ofSeconds(10))

    val inlineProgressTextPanel
        get() = remoteRobot.find<ComponentFixture>(byXpath("//div[@class='InlineProgressPanel']//div[@class='TextPanel']"),
            ofSeconds(10))

    val statusTextPanel
        get() = remoteRobot.find<ComponentFixture>(byXpath("//div[@class='StatusPanel']//div[@class='TextPanel']"),
            ofSeconds(10))

    val buildResultInEditor
        get() = remoteRobot.find<ComponentFixture>(byXpath("//div[@class='TrafficLightButton']"),
            ofSeconds(20))

    val buildResult
        get() = textField(byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]"),
            ofSeconds(20))

    val ideError
        get() = remoteRobot.find<NotificationFixture>(byXpath( "//div[@class='NotificationCenterPanel'][.//div[@accessiblename.key='error.new.notification.title']]"),
            ofSeconds(10))

    val infoNotification
        get() = remoteRobot.find<NotificationFixture>(byXpath( "//div[@val_icon='balloonInformation.svg']/../div[@class='NotificationCenterPanel']"),
            ofSeconds(10))

    @JvmOverloads
    fun dumbAware(timeout: Duration = Duration.ofMinutes(5), function: () -> Unit) {
        step("Wait for smart mode") {
            waitFor(duration = timeout, interval = ofSeconds(5)) {
                runCatching { isDumbMode().not() }.getOrDefault(false)
            }
            function()
            step("..wait for smart mode again") {
                waitFor(duration = timeout, interval = ofSeconds(5)) {
                    isDumbMode().not()
                }
            }
        }
    }

    fun isDumbMode(): Boolean {
        return callJs("""
            const frameHelper = com.intellij.openapi.wm.impl.ProjectFrameHelper.getFrameHelper(component)
            if (frameHelper) {
                const project = frameHelper.getProject()
                project ? com.intellij.openapi.project.DumbService.isDumb(project) : true
            } else { 
                true 
            }
        """, true)
    }

    fun closeProject() {
        if (remoteRobot.isMac()) {
            keyboard {
                hotKey(KeyEvent.VK_SHIFT, KeyEvent.VK_META, KeyEvent.VK_A)
                enterText("Close Project")
                enter()
            }
        } else {
            menuBar.select("File", "Close Project")
        }
    }

    fun callUnitTestBotActionOn(classname: String) {
        step("Call UnitTestBot action") {
            waitFor(ofSeconds(200)) { !isDumbMode() }
            with(projectViewTree) {
                findText(classname).click(MouseButton.RIGHT_BUTTON)
            }
            remoteRobot.actionMenuItem("Generate Tests with UnitTestBot...").click()
            unitTestBotDialog.generateTestsButton.click()
       }
    }

    open fun waitProjectIsOpened() {
        waitForIgnoringError(ofSeconds(30)) {
            projectViewTree.hasText(projectName)
        }
    }

    open fun waitProjectIsCreated() {
        waitProjectIsOpened()
    }

    open fun expandProjectTree(projectName: String) {
        with(projectViewTree) {
            if (hasText("src").not()) {
                findText(projectName).doubleClick()
                waitForIgnoringError{
                    hasText("src").and(hasText(".idea"))
                }
            }
        }
    }

    fun createNewJavaClass(newClassname: String = "Example",
                           textToClickOn: String = "Main") {
        with(projectViewTree) {
            findText(textToClickOn).click(MouseButton.RIGHT_BUTTON)
        }
        remoteRobot.actionMenu("New").click()
        remoteRobot.actionMenuItem("Java Class").click()
        remoteRobot.keyboard {
            enterText(newClassname)
            enter()
        }
    }
}
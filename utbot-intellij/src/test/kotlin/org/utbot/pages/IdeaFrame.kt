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
import org.utbot.dialogs.AddFileToGitDialogFixture
import org.utbot.dialogs.ProjectStructureDialogFixture
import org.utbot.dialogs.UnitTestBotDialogFixture
import org.utbot.dialogs.WarningDialogFixture
import org.utbot.elements.NotificationFixture
import org.utbot.tabs.InspectionViewFixture
import java.awt.event.KeyEvent
import java.time.Duration
import java.time.Duration.ofSeconds

fun RemoteRobot.idea(function: IdeaFrame.() -> Unit) {
    find<IdeaFrame>(timeout = ofSeconds(5)).apply(function)
}

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

    val utbotNotification
        get() = remoteRobot.find<NotificationFixture>(byXpath( "//div[@class='NotificationCenterPanel'][div[contains(.,'UnitTestBot')]]"),
            ofSeconds(10))

    val inspectionsView
        get() = remoteRobot.find(InspectionViewFixture::class.java)

    val problemsTabButton
        get() = button( byXpath("//div[contains(@text.key, 'toolwindow.stripe.Problems_View')]"))

    // Dialogs
    val unitTestBotDialog
        get() = remoteRobot.find(UnitTestBotDialogFixture::class.java)

    val projectStructureDialog
        get() = remoteRobot.find(ProjectStructureDialogFixture::class.java)

    val addFileToGitDialog
        get() = remoteRobot.find(AddFileToGitDialogFixture::class.java)

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
        try {
            remoteRobot.find(WarningDialogFixture::class.java, ofSeconds(1))
                .terminateButton.click()
        } catch (ignore: Throwable) {}
    }

    fun openUTBotDialogFromProjectViewForClass(classname: String, packageName: String = "") {
        step("Call UnitTestBot action") {
            waitFor(ofSeconds(200)) { !isDumbMode() }
            with(projectViewTree) {
                if (hasText(classname).not() && packageName != "") {
                    findText{it.text.endsWith(packageName)}.doubleClick()
                }
                findText(classname).click(MouseButton.RIGHT_BUTTON)
            }
            remoteRobot.actionMenuItem("Generate Tests with UnitTestBot...").click()
       }
    }

    open fun waitProjectIsBuilt() {
        projectViewTree.click()
        keyboard { key(KeyEvent.VK_PAGE_UP) }
        waitForIgnoringError(ofSeconds(30)) {
            projectViewTree.hasText(projectName)
        }
    }

    open fun waitProjectIsCreated() {
        waitProjectIsBuilt()
    }

    open fun expandProjectTree() {
        with(projectViewTree) {
            if (hasText("src").not()) {
                findText(projectName).doubleClick()
                waitForIgnoringError{
                    hasText("src").and(hasText(".idea"))
                }
            }
        }
    }

    open fun createNewPackage(packageName: String) {
        with(projectViewTree) {
            if (hasText("src").not()) {
                findText(projectName).doubleClick()
                waitFor { hasText("src") }
            }
            findText("src").click(MouseButton.RIGHT_BUTTON)
        }
        remoteRobot.actionMenu("New").click()
        remoteRobot.actionMenuItem("Package").click()
        keyboard {
            enterText(packageName)
            enter()
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

    fun openProjectStructureDialog() {
        if (remoteRobot.isMac()) {
            keyboard {
                hotKey(KeyEvent.VK_SHIFT, KeyEvent.VK_META, KeyEvent.VK_A)
                enterText("Project Structure...")
                enter()
            }
        } else {
            menuBar.select("File", "Project Structure...")
        }
    }
}
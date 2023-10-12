package org.utbot.elements

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration.ofSeconds

@FixtureName("Notification Center Panel")
@DefaultXpath("NotificationCenterPanel type", "//div[@class='NotificationCenterPanel']")
class NotificationFixture(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : CommonContainerFixture(remoteRobot, remoteComponent) {

    val title
        get() = jLabel(byXpath("//div[@class='JLabel']"),
            ofSeconds(5))

    val body
        get() = remoteRobot.find<ComponentFixture>(byXpath("//div[@class='JEditorPane']"),
            ofSeconds(5))

    val projectLoadButton
        get() = button(byXpath("//div[@text.key='unlinked.project.notification.load.action']"))

    // For add file to Git notification
    val alwaysAddButton
        get() = button(byXpath("//div[contains(@text.key, 'external.files.add.notification.action.add')]"))

    val dontAskAgainButton
        get() = button(byXpath("//div[contains(@text.key, 'external.files.add.notification.action.mute')]"))

}


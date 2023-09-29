package org.utbot.tabs

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration.ofSeconds

@FixtureName("Inspection Results View")
@DefaultXpath("InspectionResultsView type", "//div[@class='InspectionResultsView']")
class InspectionViewFixture(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : CommonContainerFixture(remoteRobot, remoteComponent) {

    val inspectionTree // not all problems, but only inspections tab
        get() = find<ComponentFixture>(byXpath("//div[@class='InspectionTree']"),
            ofSeconds(10))
}

package org.utbot.dialogs

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.Keyboard
import java.time.Duration

@FixtureName("UnitTestBotDialog")
@DefaultXpath("Dialog type", "//*[contains(@title, 'UnitTestBot')]")
class UnitTestBotDialogFixture(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent) : DialogFixture(remoteRobot, remoteComponent) {
    val keyboard: Keyboard = Keyboard(remoteRobot)

    val sdkNotificationLabel
        get() = jLabel(
            byXpath("//div[@class='SdkNotificationPanel']//div[@defaulticon='fatalError.svg']"))

    val setupSdkLink
        get() = actionLink(
            byXpath("//div[@class='SdkNotificationPanel']//div[@class='HyperlinkLabel']"))

    val testSourcesRootLabel
        get() = jLabel(
            byXpath("//div[@text='Test sources root:']"))

    val testSourcesRootComboBox
        get() = comboBox(
            byXpath("//div[@class='TestFolderComboWithBrowseButton']/div[@class='ComboBox']"))

    val testingFrameworkLabel
        get() = jLabel(
            byXpath("//div[@text='Testing framework:']"))

    val testingFrameworkComboBox
        get() = comboBox(
            byXpath("//div[@accessiblename='Testing framework:' and @class='ComboBox']"))

    val mockingStrategyLabel
        get() = jLabel(
            byXpath("//div[@text='Mocking strategy:']"))

    val mockingStrategyComboBox
        get() = comboBox(
            byXpath("//div[@accessiblename='Mocking strategy:' and @class='ComboBox']"))

    val mockStaticMethodsCheckbox
        get() = checkBox(
            byXpath("//div[@text='Mock static methods']"))

    val parameterizedTestsCheckbox
        get() = checkBox(
            byXpath("//div[@text='Parameterized tests']"))

    val testGenerationTimeoutLabel
        get() = jLabel(
            byXpath("//div[@text='Test generation timeout:']"))

    val testGenerationTimeoutTextField
        get() = textField(
            byXpath("//div[@class='JFormattedTextField']"))

    val timeoutSecondsPerClassLabel
        get() = jLabel(
            byXpath("//div[@text='seconds per class']"))

    val generateTestsForLabel
        get() = jLabel(
            byXpath("//div[@text='Generate tests for:']"))

    val memberListTable
        get() = remoteRobot.find<JTableFixture>(byXpath("//div[@class='MemberSelectionTable']"),
            Duration.ofSeconds(5)
        )

    val generateTestsButton
        get() = button(
            byXpath("//div[@class='MainButton']"))

    val arrowOnGenerateTestsButton
        get() = button(
            byXpath("//div[@class='JBOptionButton' and @text='Generate Tests']//div[@class='ArrowButton']"))

    val buttonsList
        get() = heavyWeightWindow().itemsList


    // Spring-specific elements
    val springConfigurationLabel
        get() = jLabel(
            byXpath("//div[@text='Spring configuration:']"))

    val springConfigurationComboBox
        get() = comboBox(
            byXpath("//div[@accessiblename='Spring configuration:' and @class='ComboBox']"))

    val springTestsTypeLabel
        get() = jLabel(
            byXpath("//div[@text='Tests type:']"))

    val springTestsTypeComboBox
        get() = comboBox(
            byXpath("//div[@accessiblename='Tests type:' and @class='ComboBox']"))

    val springActiveProfilesLabel
        get() = jLabel(
            byXpath("//div[@text='Active profile(s):']"))

    val springActiveProfilesTextField
        get() = textField(
            byXpath("//div[@accessiblename='Active profile(s):' and @class='JBTextField']"))

}
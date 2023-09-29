package org.utbot.tests

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.waitForIgnoringError
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.*
import org.utbot.data.IdeaBuildSystem
import org.utbot.pages.IdeaGradleFrame
import org.utbot.pages.idea
import org.utbot.pages.welcomeFrame
import java.time.Duration

class SpringUTBotActionTest : BaseTest() {

    val SPRING_PROJECT_DIRECTORY = "D:\\JavaProjects\\spring"
    val SPRING_EXISTING_PROJECT_NAME = "spring-petclinic"
    val APP_PACKAGE_NAME = "org.springframework.samples.petclinic"
    val EXISTING_PACKAGE_NAME = "vet"
    val EXISTING_CLASS_NAME = "VetController"

    @BeforeEach
    fun openExistingSpringProject(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {
        welcomeFrame {
            try {
                findText(SPRING_EXISTING_PROJECT_NAME).click()
            } catch (ignore: NoSuchElementException) {
                openProjectByPath(SPRING_PROJECT_DIRECTORY, SPRING_EXISTING_PROJECT_NAME)
            }
        }
        val ideaFrame = remoteRobot.find(IdeaGradleFrame::class.java, Duration.ofSeconds(10))
        return with(ideaFrame) {
            waitProjectIsOpened()
            expandProjectTree()
        }
    }

    @Test
    @DisplayName("Check action dialog UI default state in a Spring project")
    @Tags(Tag("Spring"), Tag("Java"), Tag("UnitTestBot"), Tag("UI"))
    fun checkSpringDefaultActionDialog(remoteRobot: RemoteRobot) {
        val ideaFrame = getIdeaFrameForBuildSystem(remoteRobot, IdeaBuildSystem.GRADLE)
        return with (ideaFrame) {
            openUTBotDialogFromProjectViewForClass(EXISTING_CLASS_NAME, EXISTING_PACKAGE_NAME)
            with (unitTestBotDialog) {
                val softly = SoftAssertions()
                softly.assertThat(springConfigurationLabel.isVisible())
                softly.assertThat(springConfigurationComboBox.isShowing)
                softly.assertThat(springConfigurationComboBox.selectedText()== "No Configuration")
                softly.assertThat(springConfigurationComboBox.listValues()
                    .containsAll(listOf("No Configuration", "PetClinicApplication", "CacheConfiguration")))
                softly.assertThat(springTestsTypeLabel.isVisible())
                softly.assertThat(springTestsTypeComboBox.isShowing)
                softly.assertThat(springTestsTypeComboBox.selectedText() == "Unit tests")
                softly.assertThat(springActiveProfilesLabel.isShowing)
                softly.assertThat(springActiveProfilesTextField.text =="default")
                softly.assertThat(mockingStrategyLabel.isVisible())
                softly.assertThat(mockingStrategyComboBox.selectedText() == "Mock everything outside the class")
                softly.assertThat(mockingStrategyComboBox.listValues()
                    .containsAll(listOf("Do not mock", "Mock everything outside the package", "Mock everything outside the class")))
                softly.assertThat(mockStaticMethodsCheckbox.isShowing)
                softly.assertThat(parameterizedTestsCheckbox.isShowing)
                softly.assertThat(parameterizedTestsCheckbox.isSelected().not())
                softly.assertThat(testGenerationTimeoutLabel.isShowing)
                softly.assertThat(testGenerationTimeoutTextField.text.isNotEmpty())
                softly.assertThat(memberListTable.isShowing)
                softly.assertThat(memberListTable.columnCount == 1)
                softly.assertThat(memberListTable.rowCount == 2)
                softly.assertThat(memberListTable.data.getAll().map { it.text }
                    .containsAll(listOf("showResourcesVetList():Vets", "showVetList(page:int, model:Model):String")))
                softly.assertThat(generateTestsButton.isEnabled())
                softly.assertThat(arrowOnGenerateTestsButton.isShowing)
                arrowOnGenerateTestsButton.click()
                softly.assertThat(buttonsList.isShowing)
                softly.assertThat(buttonsList.collectItems().containsAll(listOf("Generate Tests", "Generate and Run")))
                softly.assertAll()
            }
        }
    }

    @Test
    @DisplayName("Check action dialog UI when Spring configuration is selected")
    @Tags(Tag("Spring"), Tag("Java"), Tag("UnitTestBot"), Tag("UI"))
    fun checkActionDialogWithSpringConfiguration(remoteRobot: RemoteRobot) {
        val ideaFrame = getIdeaFrameForBuildSystem(remoteRobot, IdeaBuildSystem.GRADLE)
        return with (ideaFrame) {
            openUTBotDialogFromProjectViewForClass(EXISTING_CLASS_NAME, EXISTING_PACKAGE_NAME)
            with (unitTestBotDialog) {
                springConfigurationComboBox.click() /* ComboBoxFixture::selectItem doesn't work with heavyWeightWindow */
                heavyWeightWindow().itemsList.clickItem("PetClinicApplication")
                val softly = SoftAssertions()
                softly.assertThat(springConfigurationComboBox.selectedText()== "PetClinicApplication")
                softly.assertThat(springConfigurationComboBox.listValues()
                    .containsAll(listOf("No Configuration", "PetClinicApplication", "CacheConfiguration")))
                softly.assertThat(springTestsTypeComboBox.selectedText() == "Unit tests")
                softly.assertThat(springActiveProfilesTextField.text =="default")
                softly.assertThat(generateTestsButton.isEnabled())
                softly.assertAll()
            }
        }
    }

    @Test
    @DisplayName("Check action dialog UI when Integration tests are selected")
    @Tags(Tag("Spring"), Tag("Java"), Tag("UnitTestBot"), Tag("UI"))
    fun checkActionDialogWithIntegrationTests(remoteRobot: RemoteRobot) {
        val ideaFrame = getIdeaFrameForBuildSystem(remoteRobot, IdeaBuildSystem.GRADLE)
        return with (ideaFrame) {
            openUTBotDialogFromProjectViewForClass(EXISTING_CLASS_NAME, EXISTING_PACKAGE_NAME)
            with (unitTestBotDialog) {
                springConfigurationComboBox.click() /* ComboBoxFixture::selectItem doesn't work with heavyWeightWindow */
                heavyWeightWindow().itemsList.clickItem("PetClinicApplication")
                springTestsTypeComboBox.selectItem("Integration tests")
                val softly = SoftAssertions()
                softly.assertThat(springConfigurationComboBox.selectedText()== "PetClinicApplication")
                softly.assertThat(springConfigurationComboBox.listValues()
                    .containsAll(listOf("No Configuration", "PetClinicApplication", "CacheConfiguration")))
                softly.assertThat(springTestsTypeComboBox.selectedText() == "Unit tests")
                softly.assertThat(springActiveProfilesTextField.text =="default")
                softly.assertThat(generateTestsButton.isEnabled())
                softly.assertAll()
            }
        }
    }

    @Test
    @DisplayName("Check Spring Unit tests generation")
    @Tags(Tag("Spring"), Tag("Java"), Tag("UnitTestBot"), Tag("Unit tests"), Tag("Generate tests"))
    fun checkSpringUnitTestsGeneration(remoteRobot: RemoteRobot) {
        val ideaFrame = getIdeaFrameForBuildSystem(remoteRobot, IdeaBuildSystem.GRADLE)
        return with (ideaFrame) {
            openUTBotDialogFromProjectViewForClass(EXISTING_CLASS_NAME, EXISTING_PACKAGE_NAME)
            with (unitTestBotDialog) {
                springConfigurationComboBox.click() /* ComboBoxFixture::selectItem doesn't work with heavyWeightWindow */
                heavyWeightWindow().itemsList.clickItem("PetClinicApplication")
                unitTestBotDialog.generateTestsButton.click()
            }
            waitForIgnoringError (Duration.ofSeconds(10)){
                inlineProgressTextPanel.isShowing
            }
            waitForIgnoringError (Duration.ofSeconds(60)){
                inlineProgressTextPanel.hasText("Generate test cases for class $EXISTING_CLASS_NAME")
            }
            waitForIgnoringError(Duration.ofSeconds(90)) {
                utbotNotification.title.hasText("UnitTestBot: unit tests generated successfully")
            }
            val softly = SoftAssertions()
            softly.assertThat(utbotNotification.body.hasText("Target: org.springframework.samples.petclinic.vet.VetController Overall test methods: 7"))
            softly.assertThat(textEditor().editor.text).contains("class ${EXISTING_CLASS_NAME}Test")
            softly.assertThat(textEditor().editor.text).contains("@Test\n")
//            softly.assertThat(textEditor().editor.text).contains(CONTEXT_LOADS_TEST_TEXT)
            softly.assertThat(textEditor().editor.text).contains("@utbot.classUnderTest {@link ${EXISTING_CLASS_NAME}}")
            softly.assertThat(textEditor().editor.text).contains("@utbot.methodUnderTest {@link ${EXISTING_CLASS_NAME}#showResourcesVetList")
            softly.assertThat(textEditor().editor.text).contains("@utbot.methodUnderTest {@link ${EXISTING_CLASS_NAME}#showVetList")
            softly.assertThat(inspectionsView.inspectionTree.isShowing)
            softly.assertThat(inspectionsView.inspectionTree.hasText("Errors detected by UnitTestBot"))
            softly.assertThat(inspectionsView.inspectionTree.hasText("${EXISTING_CLASS_NAME}.java"))
            hideInspectionViewButton.click()
            softly.assertAll()
        }
    }

    @AfterEach
    fun closeDialogIfNotClosed (remoteRobot: RemoteRobot): Unit = with(remoteRobot){
        idea {
            try {
                unitTestBotDialog.closeButton.click()
            } catch (ignore: Throwable) {}
        }
    }

    val CONTEXT_LOADS_TEST_TEXT = """	/**
	 * This sanity check test fails if the application context cannot start.
	 */
	@Test
	public void contextLoads() {
	}"""

}
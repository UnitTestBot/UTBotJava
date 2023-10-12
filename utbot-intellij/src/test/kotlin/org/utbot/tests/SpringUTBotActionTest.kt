package org.utbot.tests

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.waitForIgnoringError
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.*
import org.utbot.data.*
import org.utbot.pages.idea
import org.utbot.pages.welcomeFrame
import java.io.File
import java.time.Duration

class SpringUTBotActionTest : BaseTest() {

    val EXISTING_PACKAGE_NAME = "vet"
    val EXISTING_CLASS_NAME = "VetController"

    @BeforeEach
    fun openSpringProject(remoteRobot: RemoteRobot): Unit = with(remoteRobot) {
        welcomeFrame {
            findText {
                it.text.endsWith(CURRENT_RUN_DIRECTORY_END + File.separator + SPRING_PROJECT_NAME)
            }.click()
        }
        with (getIdeaFrameForBuildSystem(remoteRobot, IdeaBuildSystem.GRADLE)) {
            waitProjectIsBuilt()
            try {
                loadProjectNotification.projectLoadButton.click()
                waitProjectIsBuilt()
            } catch (ignore: Throwable) {}
            expandProjectTree()
            openUTBotDialogFromProjectViewForClass(EXISTING_CLASS_NAME, EXISTING_PACKAGE_NAME)
        }
    }

    @Test
    @DisplayName("Check action dialog UI default state in a Spring project")
    @Tags(Tag("Spring"), Tag("Java"), Tag("UnitTestBot"), Tag("UI"))
    fun checkSpringDefaultActionDialog(remoteRobot: RemoteRobot) {
        with (getIdeaFrameForBuildSystem(remoteRobot, IdeaBuildSystem.GRADLE)) {
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
        with (getIdeaFrameForBuildSystem(remoteRobot, IdeaBuildSystem.GRADLE)) {
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
        with (getIdeaFrameForBuildSystem(remoteRobot, IdeaBuildSystem.GRADLE)) {
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

    @Order(1) // to close git notification
    @Test
    @DisplayName("Check Spring Unit tests generation")
    @Tags(Tag("Spring"), Tag("Java"), Tag("UnitTestBot"), Tag("Unit tests"), Tag("Generate tests"))
    fun checkSpringUnitTestsGeneration(remoteRobot: RemoteRobot) {
        with (getIdeaFrameForBuildSystem(remoteRobot, IdeaBuildSystem.GRADLE)) {
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
            waitForIgnoringError (Duration.ofSeconds(90)){
                addToGitNotification.isShowing
            }
            addToGitNotification.alwaysAddButton.click() // otherwise prompt dialog will be shown for each created test file
            waitForIgnoringError(Duration.ofSeconds(90)) {
                utbotNotification.title.hasText("UnitTestBot: unit tests generated with warnings")
                // because project has several test frameworks
            }
            val softly = SoftAssertions()
            softly.assertThat(utbotNotification.body.hasText("Target: org.springframework.samples.petclinic.vet.VetController Overall test methods: 7"))
            softly.assertThat(textEditor().editor.text).contains("class ${EXISTING_CLASS_NAME}Test")
            softly.assertThat(textEditor().editor.text).contains("@Test\n")
            softly.assertThat(textEditor().editor.text).contains("@InjectMocks\n\tprivate VetController vetController;")
            softly.assertThat(textEditor().editor.text).contains("@Mock\n\tprivate VetRepository vetRepositoryMock;")
            softly.assertThat(textEditor().editor.text).contains("@utbot.classUnderTest {@link ${EXISTING_CLASS_NAME}}")
            softly.assertThat(textEditor().editor.text).contains("@utbot.methodUnderTest {@link ${EXISTING_CLASS_NAME}#showResourcesVetList")
            softly.assertThat(textEditor().editor.text).contains("@utbot.methodUnderTest {@link ${EXISTING_CLASS_NAME}#showVetList")
            softly.assertThat(inspectionsView.inspectionTree.isShowing)
            softly.assertThat(inspectionsView.inspectionTree.hasText("Errors detected by UnitTestBot"))
            softly.assertThat(inspectionsView.inspectionTree.hasText("${EXISTING_CLASS_NAME}.java"))
            buildResultInEditor.rightClick() // to check test class is compilable
            softly.assertThat(heavyWeightWindow().data.getAll().toString().contains("error").not())
            problemsTabButton.click() //to close problems view
            softly.assertAll()
        }
    }

    @Test
    @DisplayName("Check Spring Integration tests generation")
    @Tags(Tag("Spring"), Tag("Java"), Tag("UnitTestBot"), Tag("Integration tests"), Tag("Generate tests"))
    fun checkSpringIntegrationTestsGeneration(remoteRobot: RemoteRobot) {
        with (getIdeaFrameForBuildSystem(remoteRobot, IdeaBuildSystem.GRADLE)) {
            with (unitTestBotDialog) {
                springConfigurationComboBox.click() /* ComboBoxFixture::selectItem doesn't work with heavyWeightWindow */
                heavyWeightWindow().itemsList.clickItem("PetClinicApplication")
                springTestsTypeComboBox.selectItem("Integration tests")
                unitTestBotDialog.generateTestsButton.click()
                unitTestBotDialog.integrationTestsWarningDialog.proceedButton.click()
            }
            waitForIgnoringError (Duration.ofSeconds(10)){
                inlineProgressTextPanel.isShowing
            }
            waitForIgnoringError (Duration.ofSeconds(60)){
                inlineProgressTextPanel.hasText("Generate test cases for class $EXISTING_CLASS_NAME")
            }
            waitForIgnoringError(Duration.ofSeconds(90)) {
                utbotNotification.title.hasText("UnitTestBot: unit tests generated with warnings")
                // because project has several test frameworks
            }
            val softly = SoftAssertions()
            softly.assertThat(utbotNotification.body.hasText("Target: org.springframework.samples.petclinic.vet.VetController Overall test methods: "))
            softly.assertThat(textEditor().editor.text).contains("@SpringBootTest\n")
            softly.assertThat(textEditor().editor.text).contains("@BootstrapWith(SpringBootTestContextBootstrapper.class)\n")
            softly.assertThat(textEditor().editor.text).contains("@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)\n")
            softly.assertThat(textEditor().editor.text).contains("@Transactional\n")
            softly.assertThat(textEditor().editor.text).contains("@AutoConfigureTestDatabase\n")
            softly.assertThat(textEditor().editor.text).contains("@AutoConfigureMockMvc\n")
            softly.assertThat(textEditor().editor.text).contains("class ${EXISTING_CLASS_NAME}Test")
            softly.assertThat(textEditor().editor.text).contains("@Test\n")
            softly.assertThat(textEditor().editor.text).contains(CONTEXT_LOADS_TEST_TEXT)
            softly.assertThat(textEditor().editor.text).contains("///region FUZZER: SUCCESSFUL EXECUTIONS for method showResourcesVetList()")
            softly.assertThat(inspectionsView.inspectionTree.isShowing)
            softly.assertThat(inspectionsView.inspectionTree.hasText("Errors detected by UnitTestBot"))
            softly.assertThat(inspectionsView.inspectionTree.hasText("${EXISTING_CLASS_NAME}.java"))
            buildResultInEditor.rightClick() // to check test class is compilable
            softly.assertThat(heavyWeightWindow().data.getAll().toString().contains("error").not())
            problemsTabButton.click() //to close problems view
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
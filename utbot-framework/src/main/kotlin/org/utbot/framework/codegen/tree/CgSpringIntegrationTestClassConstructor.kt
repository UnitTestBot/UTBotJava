package org.utbot.framework.codegen.tree

import mu.KotlinLogging
import org.utbot.common.tryLoadClass
import org.utbot.framework.codegen.domain.Junit4
import org.utbot.framework.codegen.domain.Junit5
import org.utbot.framework.codegen.domain.TestNg
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.*
import org.utbot.framework.codegen.domain.models.AnnotationTarget.*
import org.utbot.framework.codegen.domain.models.CgTestMethodType.FAILING
import org.utbot.framework.codegen.domain.models.CgTestMethodType.SUCCESSFUL
import org.utbot.framework.codegen.domain.models.SpringTestClassModel
import org.utbot.framework.codegen.util.escapeControlChars
import org.utbot.framework.codegen.util.resolve
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConcreteContextLoadingResult
import org.utbot.framework.plugin.api.SpringSettings.*
import org.utbot.framework.plugin.api.SpringConfiguration.*
import org.utbot.framework.plugin.api.util.IndentUtil.TAB
import org.utbot.framework.plugin.api.util.SpringModelUtils
import org.utbot.framework.plugin.api.util.SpringModelUtils.activeProfilesClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.autoConfigureTestDbClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.bootstrapWithClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.contextConfigurationClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.dirtiesContextClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.dirtiesContextClassModeClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.extendWithClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.mockMvcClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.repositoryClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.runWithClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.springBootTestClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.springBootTestContextBootstrapperClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.springExtensionClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.springRunnerClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.transactionalClassId
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.spring.api.UTSpringContextLoadingException

class CgSpringIntegrationTestClassConstructor(
    context: CgContext,
    private val concreteContextLoadingResult: ConcreteContextLoadingResult?,
    private val springSettings: PresentSpringSettings,
) : CgAbstractSpringTestClassConstructor(context) {

    private val autowiredFieldManager = CgAutowiredFieldsManager(context)
    private val persistenceContextFieldsManager = CgPersistenceContextFieldsManager.createIfPossible(context)

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun constructTestClass(testClassModel: SpringTestClassModel): CgClass {
        addNecessarySpringSpecificAnnotations(testClassModel)
        return super.constructTestClass(testClassModel)
    }

    override fun constructClassFields(testClassModel: SpringTestClassModel): List<CgFieldDeclaration> {
        return constructFieldsWithAnnotation(
            autowiredFieldManager,
            testClassModel.springSpecificInformation.autowiredFromContextModels
        ) + persistenceContextFieldsManager?.let { persistenceContextFieldsManager ->
            constructFieldsWithAnnotation(
                persistenceContextFieldsManager,
                testClassModel.springSpecificInformation.entityManagerModels
            )
        }.orEmpty()
    }

    override fun constructAdditionalTestMethods() =
        CgMethodsCluster.withoutDocs(
            listOfNotNull(constructContextLoadsMethod())
        )

    private fun constructContextLoadsMethod() : CgTestMethod {
        if (concreteContextLoadingResult == null)
            logger.error { "Missing contextLoadingResult" }
        val exception = concreteContextLoadingResult?.exceptions?.firstOrNull()
        return CgTestMethod(
            name = "contextLoads",
            statements = listOfNotNull(
                exception?.let { e -> constructFailedContextLoadingTraceComment(e) },
                if (concreteContextLoadingResult == null) CgSingleLineComment("Error: context loading result from concrete execution is missing") else null
            ),
            annotations = listOf(addAnnotation(context.testFramework.testAnnotationId, Method)),
            documentation = CgDocumentationComment(listOf(
                CgDocRegularLineStmt("This sanity check test fails if the application context cannot start.")
            ) + exception?.let { constructFailedContextLoadingDocComment() }.orEmpty()),
            type = if (concreteContextLoadingResult != null && exception == null) SUCCESSFUL else FAILING
        )
    }

    private fun constructFailedContextLoadingDocComment() = listOf(
        CgDocRegularLineStmt("<p>"),
        CgDocRegularLineStmt("Context loading throws an exception."),
        CgDocRegularLineStmt("Please try to fix your context or environment configuration."),
        CgDocRegularLineStmt("Spring configuration applied: ${springSettings.configuration.fullDisplayName}."),
    )

    private fun constructFailedContextLoadingTraceComment(exception: Throwable) = CgMultilineComment(
        exception
            .stackTraceToString()
            .lines()
            .let { lines ->
                if (exception is UTSpringContextLoadingException) lines.dropWhile { !it.contains("Caused") }
                else lines
            }
            .mapIndexed { i, line ->
                if (i == 0) "Failure ${line.replace("Caused", "caused")}"
                else TAB + line
            }
            .map { it.escapeControlChars() }
    )

    private fun addNecessarySpringSpecificAnnotations(testClassModel: SpringTestClassModel) {
        val isSpringBootTestAccessible = utContext.classLoader.tryLoadClass(springBootTestClassId.name) != null
        if (isSpringBootTestAccessible) {
            addAnnotation(springBootTestClassId, Class)
        }

        val (testFrameworkExtension, springExtension) = when (testFramework) {
            Junit4 -> runWithClassId to springRunnerClassId
            Junit5 -> extendWithClassId to springExtensionClassId
            TestNg -> error("Spring extension is not implemented in TestNg")
            else -> error("Trying to generate tests for Spring project with non-JVM framework")
        }

        // @SpringBootTest contains @ExtendWith(SpringExtension.class), no need to add it manually
        if (!isSpringBootTestAccessible || testFrameworkExtension != extendWithClassId) {
            addAnnotation(
                classId = testFrameworkExtension,
                argument = createGetClassExpression(springExtension, codegenLanguage),
                target = Class,
            )
        }

        if (utContext.classLoader.tryLoadClass(springBootTestContextBootstrapperClassId.name) != null)
            // TODO in somewhat new versions of Spring Boot, @SpringBootTest
            //  already includes @BootstrapWith(SpringBootTestContextBootstrapper.class),
            //  so we should avoid adding it manually to reduce number of annotations
            addAnnotation(
                classId = bootstrapWithClassId,
                argument = createGetClassExpression(springBootTestContextBootstrapperClassId, codegenLanguage),
                target = Class,
            )

        val defaultProfileIsUsed = springSettings.profiles.singleOrNull() == "default"
        if (!defaultProfileIsUsed) {
            addAnnotation(
                classId = activeProfilesClassId,
                namedArguments = listOf(
                    CgNamedAnnotationArgument(
                        name = "profiles",
                        value = CgArrayAnnotationArgument(springSettings.profiles.map { profile -> profile.resolve() })
                    )
                ),
                target = Class,
            )
        }

        // TODO: we support only JavaBasedConfiguration in integration tests.
        //  Adapt for XMLConfigurations when supported.
        val configClass = springSettings.configuration as JavaBasedConfiguration
        if (configClass is JavaConfiguration || configClass is SpringBootConfiguration && !configClass.isUnique) {
            addAnnotation(
                classId = contextConfigurationClassId,
                namedArguments = listOf(
                    CgNamedAnnotationArgument(
                        name = "classes",
                        value = CgArrayAnnotationArgument(
                            listOf(
                                createGetClassExpression(
                                    ClassId(configClass.configBinaryName),
                                    codegenLanguage
                                )
                            )
                        )
                    )
                ),
                target = Class,
            )
        }


        addAnnotation(
            classId = dirtiesContextClassId,
            namedArguments = listOf(
                CgNamedAnnotationArgument(
                    name = "classMode",
                    value = CgEnumConstantAccess(dirtiesContextClassModeClassId, "BEFORE_EACH_TEST_METHOD")
                ),
            ),
            target = Class,
        )

        if (utContext.classLoader.tryLoadClass(transactionalClassId.name) != null)
            addAnnotation(transactionalClassId, Class)

        // `@AutoConfigureTestDatabase` can itself be on the classpath, while spring-data
        // (i.e. module containing `Repository`) is not.
        //
        // If we add `@AutoConfigureTestDatabase` without having spring-data,
        // generated tests will fail with `ClassNotFoundException: org.springframework.dao.DataAccessException`.
        if (utContext.classLoader.tryLoadClass(repositoryClassId.name) != null)
            addAnnotation(autoConfigureTestDbClassId, Class)

        if (mockMvcClassId in testClassModel.springSpecificInformation.autowiredFromContextModels)
            addAnnotation(SpringModelUtils.autoConfigureMockMvcClassId, Class)
    }
}
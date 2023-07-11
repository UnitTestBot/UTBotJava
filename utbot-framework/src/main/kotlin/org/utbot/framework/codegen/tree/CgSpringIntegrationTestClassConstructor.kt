package org.utbot.framework.codegen.tree

import org.utbot.common.tryLoadClass
import org.utbot.framework.codegen.domain.Junit4
import org.utbot.framework.codegen.domain.Junit5
import org.utbot.framework.codegen.domain.TestNg
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.*
import org.utbot.framework.codegen.domain.models.AnnotationTarget.*
import org.utbot.framework.codegen.util.resolve
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.SpringSettings.*
import org.utbot.framework.plugin.api.SpringConfiguration.*
import org.utbot.framework.plugin.api.util.SpringModelUtils
import org.utbot.framework.plugin.api.util.SpringModelUtils.activeProfilesClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.autoConfigureTestDbClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.autowiredClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.bootstrapWithClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.contextConfigurationClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.crudRepositoryClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.dirtiesContextClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.dirtiesContextClassModeClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.springBootTestContextBootstrapperClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.springExtensionClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.transactionalClassId
import org.utbot.framework.plugin.api.util.utContext

class CgSpringIntegrationTestClassConstructor(
    context: CgContext,
    private val springSettings: PresentSpringSettings
) : CgAbstractSpringTestClassConstructor(context) {
    override fun constructTestClass(testClassModel: TestClassModel): CgClass {
        addNecessarySpringSpecificAnnotations()
        return super.constructTestClass(testClassModel)
    }

    override val fieldManagers: Set<CgFieldManager>
        get() = TODO("Not yet implemented")

// TODO move logic from here to CgFieldManagers
//    override fun constructClassFields(testClassModel: SpringTestClassModel): List<CgFieldDeclaration> {
//        val autowiredFromContextModels =
//            testClassModel.springSpecificInformation.autowiredFromContextModels
//        return constructFieldsWithAnnotation(autowiredClassId, autowiredFromContextModels)
//    }

    override fun constructAdditionalMethods() =
        CgMethodsCluster(header = null, content = emptyList())

    private fun addNecessarySpringSpecificAnnotations() {
        val springRunnerType = when (testFramework) {
            Junit4 -> SpringModelUtils.runWithClassId
            Junit5 -> SpringModelUtils.extendWithClassId
            TestNg -> error("Spring extension is not implemented in TestNg")
            else -> error("Trying to generate tests for Spring project with non-JVM framework")
        }

        addAnnotation(
            classId = springRunnerType,
            argument = createGetClassExpression(springExtensionClassId, codegenLanguage),
            target = Class,
        )
        addAnnotation(
            classId = bootstrapWithClassId,
            argument = createGetClassExpression(springBootTestContextBootstrapperClassId, codegenLanguage),
            target = Class,
        )
        addAnnotation(
            classId = activeProfilesClassId,
            namedArguments =
            listOf(
                CgNamedAnnotationArgument(
                    name = "profiles",
                    value =
                    CgArrayAnnotationArgument(
                        springSettings.profiles.map { profile ->
                            profile.resolve()
                        }
                    )
                )
            ),
            target = Class,
        )
        addAnnotation(
            classId = contextConfigurationClassId,
            namedArguments =
            listOf(
                CgNamedAnnotationArgument(
                    name = "classes",
                    value = CgArrayAnnotationArgument(
                        listOf(
                            createGetClassExpression(
                                // TODO:
                                //  For now we support only JavaConfigurations in integration tests.
                                //  Adapt for XMLConfigurations when supported.
                                ClassId((springSettings.configuration as JavaConfiguration).classBinaryName),
                                codegenLanguage
                            )
                        )
                    )
                )
            ),
            target = Class,
        )
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
        // (i.e. module containing `CrudRepository`) is not.
        //
        // If we add `@AutoConfigureTestDatabase` without having spring-data,
        // generated tests will fail with `ClassNotFoundException: org.springframework.dao.DataAccessException`.
        if (utContext.classLoader.tryLoadClass(crudRepositoryClassId.name) != null)
            addAnnotation(autoConfigureTestDbClassId, Class)
    }
}
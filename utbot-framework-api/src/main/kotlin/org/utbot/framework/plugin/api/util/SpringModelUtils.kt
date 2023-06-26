package org.utbot.framework.plugin.api.util

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.SpringRepositoryId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtSpringContextModel

object SpringModelUtils {
    val applicationContextClassId = ClassId("org.springframework.context.ApplicationContext")
    val crudRepositoryClassId = ClassId("org.springframework.data.repository.CrudRepository")

    // most likely only one persistent library is on the classpath, but we need to be able to work with either of them
    private val persistentLibraries = listOf("javax.persistence", "jakarta.persistence")
    private fun persistentClassIds(simpleName: String) = persistentLibraries.map { ClassId("$it.$simpleName") }

    val entityClassIds = persistentClassIds("Entity")
    val generatedValueClassIds = persistentClassIds("GeneratedValue")

    private val getBeanMethodId = MethodId(
        classId = applicationContextClassId,
        name = "getBean",
        returnType = Any::class.id,
        parameters = listOf(String::class.id),
        bypassesSandbox = true // TODO may be we can use some alternative sandbox that has more permissions
    )

    private val saveMethodId = MethodId(
        classId = crudRepositoryClassId,
        name = "save",
        returnType = Any::class.id,
        parameters = listOf(Any::class.id),
        bypassesSandbox = true // TODO may be we can use some alternative sandbox that has more permissions
    )


    fun createBeanModel(beanName: String, id: Int, classId: ClassId) = UtAssembleModel(
        id = id,
        classId = classId,
        modelName = "@Autowired $beanName",
        instantiationCall = UtExecutableCallModel(
            instance = UtSpringContextModel,
            executable = getBeanMethodId,
            params = listOf(UtPrimitiveModel(beanName))
        ),
        modificationsChainProvider = { mutableListOf() }
    )

    fun createSaveCallModel(repositoryId: SpringRepositoryId, id: Int, entityModel: UtModel) = UtExecutableCallModel(
        instance = createBeanModel(
            beanName = repositoryId.repositoryBeanName,
            id = id,
            classId = repositoryId.repositoryClassId,
        ),
        executable = saveMethodId,
        params = listOf(entityModel)
    )

    fun UtModel.isAutowiredFromContext(): Boolean =
        this is UtAssembleModel && this.instantiationCall.instance is UtSpringContextModel
}
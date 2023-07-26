package org.utbot.framework.plugin.api.util

import org.utbot.common.tryLoadClass
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.SpringRepositoryId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtSpringContextModel

object SpringModelUtils {
    val autowiredClassId = ClassId("org.springframework.beans.factory.annotation.Autowired")

    val applicationContextClassId = ClassId("org.springframework.context.ApplicationContext")
    val crudRepositoryClassId = ClassId("org.springframework.data.repository.CrudRepository")

    val springBootTestClassId = ClassId("org.springframework.boot.test.context.SpringBootTest")

    val dirtiesContextClassId = ClassId("org.springframework.test.annotation.DirtiesContext")
    val dirtiesContextClassModeClassId = ClassId("org.springframework.test.annotation.DirtiesContext\$ClassMode")
    val transactionalClassId = ClassId("org.springframework.transaction.annotation.Transactional")
    val autoConfigureTestDbClassId = ClassId("org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase")

    val runWithClassId = ClassId("org.junit.runner.RunWith")
    val springRunnerClassId = ClassId("org.springframework.test.context.junit4.SpringRunner")

    val extendWithClassId = ClassId("org.junit.jupiter.api.extension.ExtendWith")
    val springExtensionClassId = ClassId("org.springframework.test.context.junit.jupiter.SpringExtension")

    val bootstrapWithClassId = ClassId("org.springframework.test.context.BootstrapWith")
    val springBootTestContextBootstrapperClassId =
        ClassId("org.springframework.boot.test.context.SpringBootTestContextBootstrapper")

    val activeProfilesClassId = ClassId("org.springframework.test.context.ActiveProfiles")
    val contextConfigurationClassId = ClassId("org.springframework.test.context.ContextConfiguration")


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

    fun UtModel.getBeanNameOrNull(): String? = if (isAutowiredFromContext()) {
        this as UtAssembleModel
        val beanNameParam = this.instantiationCall.params.single()
        val paramValue = (beanNameParam as? UtPrimitiveModel)?.value
        paramValue.toString()
    } else {
        null
    }

    ///region spring-web
    private val requestMappingClassId = ClassId("org.springframework.web.bind.annotation.RequestMapping")
    private val pathVariableClassId = ClassId("org.springframework.web.bind.annotation.PathVariable")
    private val requestBodyClassId = ClassId("org.springframework.web.bind.annotation.RequestBody")
    private val uriComponentsBuilderClassId = ClassId("org.springframework.web.util.UriComponentsBuilder")
    private val mediaTypeClassId = ClassId("org.springframework.http.MediaType")
    private val mockHttpServletResponseClassId = ClassId("org.springframework.mock.web.MockHttpServletResponse")

    private val mockMvcRequestBuildersClassId = ClassId("org.springframework.test.web.servlet.request.MockMvcRequestBuilders")
    private val requestBuilderClassId = ClassId("org.springframework.test.web.servlet.RequestBuilder")
    private val resultActionsClassId = ClassId("org.springframework.test.web.servlet.ResultActions")
    private val mockMvcClassId = ClassId("org.springframework.test.web.servlet.MockMvc")
    private val mvcResultClassId = ClassId("org.springframework.test.web.servlet.MvcResult")
    private val mockHttpServletRequestBuilderClassId = ClassId("org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder")

    private val objectMapperClassId = ClassId("com.fasterxml.jackson.databind.ObjectMapper")

    fun createMockMvcModel(idGenerator: () -> Int) =
        createBeanModel("mockMvc", idGenerator(), mockMvcClassId)

    fun createGetMockMvcResponseModel(requestBuilderModel: UtModel, idGenerator: () -> Int): UtModel {
        val mockMvcModel = createMockMvcModel(idGenerator)

        val performModel = UtAssembleModel(
            id = idGenerator(),
            classId = resultActionsClassId,
            modelName = "perform",
            instantiationCall = UtExecutableCallModel(
                instance = mockMvcModel,
                executable = MethodId(
                    classId = mockMvcClassId,
                    name = "perform",
                    parameters = listOf(requestBuilderClassId),
                    returnType = resultActionsClassId
                ),
                params = listOf(requestBuilderModel)
            )
        )
        // TODO add `andDo(print())`
        val andReturnModel = UtAssembleModel(
            id = idGenerator(),
            classId = mvcResultClassId,
            modelName = "andReturn",
            instantiationCall = UtExecutableCallModel(
                instance = performModel,
                executable = MethodId(
                    classId = mvcResultClassId,
                    name = "andReturn",
                    parameters = listOf(),
                    returnType = mvcResultClassId
                ),
                params = listOf()
            )
        )
        return UtAssembleModel(
            id = idGenerator(),
            classId = mockHttpServletResponseClassId,
            modelName = "getResponse",
            instantiationCall = UtExecutableCallModel(
                instance = andReturnModel,
                executable = MethodId(
                    classId = mvcResultClassId,
                    name = "getResponse",
                    parameters = listOf(),
                    returnType = mockHttpServletResponseClassId
                ),
                params = listOf()
            )
        )
    }

    fun createRequestBuilderModelOrNull(methodId: MethodId, arguments: List<UtModel>, idGenerator: () -> Int): UtModel? {
        check(methodId.parameters.size == arguments.size)

        if (methodId.isStatic) return null

        val requestMappingAnnotation = getRequestMappingAnnotationOrNull(methodId) ?: return null
        val requestMethod = getRequestMethodOrNull(requestMappingAnnotation) ?: return null

        @Suppress("UNCHECKED_CAST")
        val classRequestMappingAnnotation: Annotation? =
            methodId.classId.jClass.getAnnotation(requestMappingClassId.jClass as Class<out Annotation>)
        val cassRequestPath = classRequestMappingAnnotation?.let { getRequestPathOrNull(it) }.orEmpty()

        val requestPath = cassRequestPath + (getRequestPathOrNull(requestMappingAnnotation) ?: return null)

        val pathVariablesModel = createPathVariablesModel(methodId, arguments, idGenerator)

        val urlTemplateModel = createUrlTemplateModel(pathVariablesModel, requestPath, idGenerator)

        val requestBuilderModel = UtAssembleModel(
            id = idGenerator(),
            classId = mockHttpServletRequestBuilderClassId,
            modelName = "requestBuilder",
            instantiationCall = UtExecutableCallModel(
                instance = null,
                executable = requestMethod.requestBuilderMethodId,
                params = listOf(
                    urlTemplateModel,
                    UtArrayModel(
                        id = idGenerator(),
                        classId = objectArrayClassId,
                        length = 0,
                        constModel = UtNullModel(objectClassId),
                        stores = mutableMapOf()
                    )
                )
            )
        )

        // TODO support @RequestParam, @RequestHeader, @CookieValue, @RequestAttribute
        return addContentToRequestBuilderModel(methodId, arguments, requestBuilderModel, idGenerator)
    }

    private fun addContentToRequestBuilderModel(
        methodId: MethodId,
        arguments: List<UtModel>,
        requestBuilderModel: UtAssembleModel,
        idGenerator: () -> Int
    ): UtAssembleModel? {
        @Suppress("NAME_SHADOWING")
        var requestBuilderModel = requestBuilderModel
        methodId.method.parameters.zip(arguments).forEach { (param, arg) ->
            @Suppress("UNCHECKED_CAST")
            param.getAnnotation(requestBodyClassId.jClass as Class<out Annotation>) ?: return@forEach

            // TODO filter out `null` and `Optional.empty()` values of `arg`
            val mediaTypeModel = UtAssembleModel(
                id = idGenerator(),
                classId = mediaTypeClassId,
                modelName = "mediaType",
                instantiationCall = UtExecutableCallModel(
                    instance = null,
                    executable = MethodId(
                        classId = mediaTypeClassId,
                        name = "valueOf",
                        returnType = mediaTypeClassId,
                        parameters = listOf(stringClassId)
                    ),
                    // TODO detect actual media type ("application/json" is very common default)
                    params = listOf(UtPrimitiveModel("application/json"))
                ),
            )
            requestBuilderModel = UtAssembleModel(
                id = idGenerator(),
                classId = mockHttpServletRequestBuilderClassId,
                modelName = "requestBuilder",
                instantiationCall = UtExecutableCallModel(
                    instance = requestBuilderModel,
                    executable = MethodId(
                        classId = mockHttpServletRequestBuilderClassId,
                        name = "contentType",
                        returnType = mockHttpServletRequestBuilderClassId,
                        parameters = listOf(mediaTypeClassId)
                    ),
                    params = listOf(
                        mediaTypeModel
                    )
                )
            )
            val content = UtAssembleModel(
                id = idGenerator(),
                classId = stringClassId,
                modelName = "content",
                instantiationCall = UtExecutableCallModel(
                    instance =
                    // TODO support libraries other than Jackson
                    if (utContext.classLoader.tryLoadClass(objectMapperClassId.name) == null)
                        return@addContentToRequestBuilderModel null
                    // TODO `getBean(ObjectMapper.class)`, name may change depending on Spring version
                    else createBeanModel("jacksonObjectMapper", idGenerator(), objectMapperClassId),
                    executable = MethodId(
                        classId = objectMapperClassId,
                        name = "writeValueAsString",
                        returnType = stringClassId,
                        parameters = listOf(objectClassId)
                    ),
                    params = listOf(arg)
                )
            )
            requestBuilderModel = UtAssembleModel(
                id = idGenerator(),
                classId = mockHttpServletRequestBuilderClassId,
                modelName = "requestBuilder",
                instantiationCall = UtExecutableCallModel(
                    instance = requestBuilderModel,
                    executable = MethodId(
                        classId = mockHttpServletRequestBuilderClassId,
                        name = "content",
                        returnType = mockHttpServletRequestBuilderClassId,
                        parameters = listOf(stringClassId)
                    ),
                    params = listOf(content)
                )
            )
        }
        return requestBuilderModel
    }

    private fun createPathVariablesModel(
        methodId: MethodId,
        arguments: List<UtModel>,
        idGenerator: () -> Int
    ): UtAssembleModel {
        val pathVariables = mutableMapOf<String, UtModel>()

        methodId.method.parameters.zip(arguments).forEach { (param, arg) ->
            @Suppress("UNCHECKED_CAST") val pathVariableAnnotation =
                param.getAnnotation(pathVariableClassId.jClass as Class<out Annotation>) ?: return@forEach
            val name = (pathVariableClassId.jClass.getMethod("name").invoke(pathVariableAnnotation) as? String).orEmpty()
                .ifEmpty { pathVariableClassId.jClass.getMethod("value").invoke(pathVariableAnnotation) as? String }.orEmpty()
                .ifEmpty { param.name }
            pathVariables[name] = arg
        }

        // TODO filter out `null` and `Optional.empty()` values of `arg`
        return UtAssembleModel(
            id = idGenerator(),
            classId = Map::class.java.id,
            modelName = "pathVariables",
            instantiationCall = UtExecutableCallModel(
                instance = null,
                executable = HashMap::class.java.getConstructor().executableId,
                params = emptyList()
            ),
            modificationsChainProvider = {
                pathVariables.map { (name, value) ->
                    UtExecutableCallModel(
                        instance = this,
                        executable = Map::class.java.getMethod(
                            "put",
                            Object::class.java,
                            Object::class.java
                        ).executableId,
                        params = listOf(UtPrimitiveModel(name), value)
                    )
                }
            }
        )
    }

    private fun createUrlTemplateModel(
        pathVariablesModel: UtAssembleModel,
        requestPath: String,
        idGenerator: () -> Int
    ): UtModel {
        val requestPathModel = UtPrimitiveModel(requestPath)
        return if (pathVariablesModel.modificationsChain.isEmpty()) requestPathModel
        else {
            val uriBuilderFromPath = UtAssembleModel(
                id = idGenerator(),
                classId = uriComponentsBuilderClassId,
                modelName = "uriBuilderFromPath",
                instantiationCall = UtExecutableCallModel(
                    instance = null,
                    executable = MethodId(
                        classId = uriComponentsBuilderClassId,
                        name = "fromPath",
                        parameters = listOf(stringClassId),
                        returnType = uriComponentsBuilderClassId
                    ),
                    params = listOf(requestPathModel),
                )
            )
            val uriBuilderWithPathVariables = UtAssembleModel(
                id = idGenerator(),
                classId = uriComponentsBuilderClassId,
                modelName = "uriBuilderWithPathVariables",
                instantiationCall = UtExecutableCallModel(
                    instance = uriBuilderFromPath,
                    executable = MethodId(
                        classId = uriComponentsBuilderClassId,
                        name = "uriVariables",
                        parameters = listOf(Map::class.java.id),
                        returnType = uriComponentsBuilderClassId
                    ),
                    params = listOf(pathVariablesModel),
                )
            )
            UtAssembleModel(
                id = idGenerator(),
                classId = stringClassId,
                modelName = "uriString",
                instantiationCall = UtExecutableCallModel(
                    instance = uriBuilderWithPathVariables,
                    executable = MethodId(
                        classId = uriComponentsBuilderClassId,
                        name = "toUriString",
                        parameters = emptyList(),
                        returnType = stringClassId
                    ),
                    params = emptyList(),
                )
            )
        }
    }

    // TODO handle multiple annotations on one method
    private fun getRequestMappingAnnotationOrNull(methodId: MethodId): Annotation? =
        methodId.method.annotations
            .firstOrNull { it.annotationClass.id in UtRequestMethod.annotationClassIds }

    // TODO support placeholders (e.g. "/${profile_path}").
    private fun getRequestPathOrNull(requestMappingAnnotation: Annotation): String? =
        ((requestMappingAnnotation.annotationClass.java.getMethod("path")
            .invoke(requestMappingAnnotation) as Array<*>)
            .getOrNull(0) ?:
        (requestMappingAnnotation.annotationClass.java.getMethod("value") // TODO separate support for @AliasFor
            .invoke(requestMappingAnnotation) as Array<*>)
            .getOrNull(0)) as? String // TODO support multiple paths

    private fun getRequestMethodOrNull(requestMappingAnnotation: Annotation): UtRequestMethod? =
        UtRequestMethod.values().firstOrNull { requestMappingAnnotation.annotationClass.id == it.annotationClassId } ?:
        (requestMappingClassId.jClass.getMethod("method").invoke(requestMappingAnnotation) as Array<*>)
            .getOrNull(0)?.let {
                UtRequestMethod.valueOf(it.toString())
            }

    private enum class UtRequestMethod {
        GET,
        HEAD,
        POST,
        PUT,
        PATCH,
        DELETE,
        OPTIONS,
        TRACE;

        val annotationClassId get() = ClassId(
            "org.springframework.web.bind.annotation.${name.lowercase().capitalize()}Mapping"
        )

        val requestBuilderMethodId = MethodId(
            classId = mockMvcRequestBuildersClassId,
            name = name.lowercase(),
            returnType = mockHttpServletRequestBuilderClassId,
            parameters = listOf(stringClassId, objectArrayClassId)
        )

        companion object {
            val annotationClassIds = values().map { it.annotationClassId } +
                    requestMappingClassId
        }
    }

    ///endregion
}
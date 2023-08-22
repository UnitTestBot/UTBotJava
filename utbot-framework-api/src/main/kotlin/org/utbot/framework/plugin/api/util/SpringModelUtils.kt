package org.utbot.framework.plugin.api.util

import org.utbot.common.tryLoadClass
import org.utbot.common.withToStringThreadLocalReentrancyGuard
import org.utbot.framework.plugin.api.isNotNull
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtSpringContextModel
import java.util.Optional

object SpringModelUtils {
    val autowiredClassId = ClassId("org.springframework.beans.factory.annotation.Autowired")

    val applicationContextClassId = ClassId("org.springframework.context.ApplicationContext")
    val repositoryClassId = ClassId("org.springframework.data.repository.Repository")

    val springBootTestClassId = ClassId("org.springframework.boot.test.context.SpringBootTest")

    val dirtiesContextClassId = ClassId("org.springframework.test.annotation.DirtiesContext")
    val dirtiesContextClassModeClassId = ClassId("org.springframework.test.annotation.DirtiesContext\$ClassMode")
    val transactionalClassId = ClassId("org.springframework.transaction.annotation.Transactional")
    val autoConfigureTestDbClassId = ClassId("org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase")
    val autoConfigureMockMvcClassId = ClassId("org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc")
    val withMockUserClassId = ClassId("org.springframework.security.test.context.support.WithMockUser")

    val runWithClassId = ClassId("org.junit.runner.RunWith")
    val springRunnerClassId = ClassId("org.springframework.test.context.junit4.SpringRunner")

    val extendWithClassId = ClassId("org.junit.jupiter.api.extension.ExtendWith")
    val springExtensionClassId = ClassId("org.springframework.test.context.junit.jupiter.SpringExtension")

    val bootstrapWithClassId = ClassId("org.springframework.test.context.BootstrapWith")
    val springBootTestContextBootstrapperClassId =
        ClassId("org.springframework.boot.test.context.SpringBootTestContextBootstrapper")

    val activeProfilesClassId = ClassId("org.springframework.test.context.ActiveProfiles")
    val contextConfigurationClassId = ClassId("org.springframework.test.context.ContextConfiguration")

    private fun getClassIdFromEachAvailablePackage(
        packages: List<String>,
        classNameFromPackage: String
    ): List<ClassId> = packages.map { ClassId("$it.$classNameFromPackage") }
        .filter { utContext.classLoader.tryLoadClass(it.name) != null }

    // most likely only one persistent library is on the classpath, but we need to be able to work with either of them
    private val persistentLibraries = listOf("javax.persistence", "jakarta.persistence")
    private fun persistentClassIds(simpleName: String) = getClassIdFromEachAvailablePackage(persistentLibraries, simpleName)

    val entityClassIds get() = persistentClassIds("Entity")
    val generatedValueClassIds get() = persistentClassIds("GeneratedValue")
    val idClassIds get() = persistentClassIds("Id")
    val persistenceContextClassIds get() = persistentClassIds("PersistenceContext")
    val entityManagerClassIds get() = persistentClassIds("EntityManager")

    val persistMethodIdOrNull: MethodId?
        get() {
            return MethodId(
                classId = entityManagerClassIds.firstOrNull() ?: return null,
                name = "persist",
                returnType = voidClassId,
                parameters = listOf(objectClassId),
                bypassesSandbox = true // TODO may be we can use some alternative sandbox that has more permissions
            )
        }

    val detachMethodIdOrNull: MethodId?
        get() {
            return MethodId(
                classId = entityManagerClassIds.firstOrNull() ?: return null,
                name = "detach",
                returnType = voidClassId,
                parameters = listOf(objectClassId),
                bypassesSandbox = true // TODO may be we can use some alternative sandbox that has more permissions
            )
        }

    private val validationLibraries = listOf("jakarta.validation.constraints")
    private fun validationClassIds(simpleName: String) = getClassIdFromEachAvailablePackage(validationLibraries, simpleName)
        .filter { utContext.classLoader.tryLoadClass(it.name) != null }

    val notEmptyClassIds get() = validationClassIds("NotEmpty")
    val notBlankClassIds get() = validationClassIds("NotBlank")
    val emailClassIds get() = validationClassIds("Email")


    private val getBeanMethodId = MethodId(
        classId = applicationContextClassId,
        name = "getBean",
        returnType = Any::class.id,
        parameters = listOf(String::class.id),
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
    private val requestHeaderClassId = ClassId("org.springframework.web.bind.annotation.RequestHeader")
    private val cookieValueClassId = ClassId("org.springframework.web.bind.annotation.CookieValue")
    private val requestAttributesClassId = ClassId("org.springframework.web.bind.annotation.RequestAttribute")
    private val sessionAttributesClassId = ClassId("org.springframework.web.bind.annotation.SessionAttribute")
    private val modelAttributesClassId = ClassId("org.springframework.web.bind.annotation.ModelAttribute")
    private val requestBodyClassId = ClassId("org.springframework.web.bind.annotation.RequestBody")
    private val requestParamClassId = ClassId("org.springframework.web.bind.annotation.RequestParam")
    private val uriComponentsBuilderClassId = ClassId("org.springframework.web.util.UriComponentsBuilder")
    private val mediaTypeClassId = ClassId("org.springframework.http.MediaType")
    private val mockHttpServletResponseClassId = ClassId("org.springframework.mock.web.MockHttpServletResponse")

    private val mockMvcRequestBuildersClassId = ClassId("org.springframework.test.web.servlet.request.MockMvcRequestBuilders")
    private val requestBuilderClassId = ClassId("org.springframework.test.web.servlet.RequestBuilder")
    val resultActionsClassId = ClassId("org.springframework.test.web.servlet.ResultActions")
    val mockMvcClassId = ClassId("org.springframework.test.web.servlet.MockMvc")
    private val mvcResultClassId = ClassId("org.springframework.test.web.servlet.MvcResult")
    private val resultHandlerClassId = ClassId("org.springframework.test.web.servlet.ResultHandler")
    val mockMvcResultHandlersClassId = ClassId("org.springframework.test.web.servlet.result.MockMvcResultHandlers")
    private val resultMatcherClassId = ClassId("org.springframework.test.web.servlet.ResultMatcher")
    val mockMvcResultMatchersClassId = ClassId("org.springframework.test.web.servlet.result.MockMvcResultMatchers")
    private val statusResultMatchersClassId = ClassId("org.springframework.test.web.servlet.result.StatusResultMatchers")
    private val contentResultMatchersClassId = ClassId("org.springframework.test.web.servlet.result.ContentResultMatchers")
    private val viewResultMatchersClassId = ClassId("org.springframework.test.web.servlet.result.ViewResultMatchers")
    private val mockHttpServletRequestBuilderClassId = ClassId("org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder")
    private val modelAndViewClassId = ClassId("org.springframework.web.servlet.ModelAndView")
    private val httpHeaderClassId = ClassId("org.springframework.http.HttpHeaders")

    private val objectMapperClassId = ClassId("com.fasterxml.jackson.databind.ObjectMapper")
    private val cookieClassId = ClassId("javax.servlet.http.Cookie")

    private val requestAttributesMethodId = MethodId(
        classId = mockHttpServletRequestBuilderClassId,
        name = "requestAttr",
        returnType = mockHttpServletRequestBuilderClassId,
        parameters = listOf(stringClassId, objectClassId)
    )

    private val sessionAttributesMethodId = MethodId(
        classId = mockHttpServletRequestBuilderClassId,
        name = "sessionAttr",
        returnType = mockHttpServletRequestBuilderClassId,
        parameters = listOf(stringClassId, objectClassId)
    )

    private val modelAttributesMethodId = MethodId(
        classId = mockHttpServletRequestBuilderClassId,
        name = "flashAttr",
        returnType = mockHttpServletRequestBuilderClassId,
        parameters = listOf(stringClassId, objectClassId)
    )

    private val mockHttpServletHeadersMethodId = MethodId(
        classId = mockHttpServletRequestBuilderClassId,
        name = "headers",
        returnType = mockHttpServletRequestBuilderClassId,
        parameters = listOf(httpHeaderClassId)
    )

    private val mockHttpServletCookieMethodId = MethodId(
        classId = mockHttpServletRequestBuilderClassId,
        name = "cookie",
        returnType = mockHttpServletRequestBuilderClassId,
        parameters = listOf(getArrayClassIdByElementClassId(cookieClassId))
    )

    private val mockHttpServletContentTypeMethodId = MethodId(
        classId = mockHttpServletRequestBuilderClassId,
        name = "contentType",
        returnType = mockHttpServletRequestBuilderClassId,
        parameters = listOf(mediaTypeClassId)
    )

    private val mockHttpServletContentMethodId = MethodId(
        classId = mockHttpServletRequestBuilderClassId,
        name = "content",
        returnType = mockHttpServletRequestBuilderClassId,
        parameters = listOf(stringClassId)
    )

    val mockMvcPerformMethodId = MethodId(
        classId = mockMvcClassId,
        name = "perform",
        parameters = listOf(requestBuilderClassId),
        returnType = resultActionsClassId
    )

    val resultActionsAndReturnMethodId = MethodId(
        classId = resultActionsClassId,
        name = "andReturn",
        parameters = listOf(),
        returnType = mvcResultClassId
    )

    val mvcResultGetResponseMethodId = MethodId(
        classId = mvcResultClassId,
        name = "getResponse",
        parameters = listOf(),
        returnType = mockHttpServletResponseClassId
    )

    val responseGetStatusMethodId = MethodId(
        classId = mockHttpServletResponseClassId,
        name = "getStatus",
        parameters = listOf(),
        returnType = intClassId
    )

    val responseGetErrorMessageMethodId = MethodId(
        classId = mockHttpServletResponseClassId,
        name = "getErrorMessage",
        parameters = listOf(),
        returnType = stringClassId
    )

    val responseGetContentAsStringMethodId = MethodId(
        classId = mockHttpServletResponseClassId,
        name = "getContentAsString",
        parameters = listOf(),
        returnType = stringClassId
    )

    val mvcResultGetModelAndViewMethodId = MethodId(
        classId = mvcResultClassId,
        name = "getModelAndView",
        parameters = listOf(),
        returnType = modelAndViewClassId
    )

    val modelAndViewGetModelMethodId = MethodId(
        classId = modelAndViewClassId,
        name = "getModel",
        parameters = listOf(),
        returnType = mapClassId
    )

    val modelAndViewGetViewNameMethodId = MethodId(
        classId = modelAndViewClassId,
        name = "getViewName",
        parameters = listOf(),
        returnType = stringClassId
    )

    val resultActionsAndDoMethodId = MethodId(
        classId = resultActionsClassId,
        name = "andDo",
        parameters = listOf(resultHandlerClassId),
        returnType = resultActionsClassId
    )

    val resultHandlersPrintMethodId = MethodId(
        classId = mockMvcResultHandlersClassId,
        name = "print",
        parameters = listOf(),
        returnType = resultHandlerClassId
    )

    val resultActionsAndExpectMethodId = MethodId(
        classId = resultActionsClassId,
        name = "andExpect",
        parameters = listOf(resultMatcherClassId),
        returnType = resultActionsClassId
    )

    val resultMatchersStatusMethodId = MethodId(
        classId = mockMvcResultMatchersClassId,
        name = "status",
        parameters = listOf(),
        returnType = statusResultMatchersClassId
    )

    val statusMatchersIsMethodId = MethodId(
        classId = statusResultMatchersClassId,
        name = "is",
        parameters = listOf(intClassId),
        returnType = resultMatcherClassId
    )

    val resultMatchersContentMethodId = MethodId(
        classId = mockMvcResultMatchersClassId,
        name = "content",
        parameters = listOf(),
        returnType = contentResultMatchersClassId
    )

    val contentMatchersStringMethodId = MethodId(
        classId = contentResultMatchersClassId,
        name = "string",
        parameters = listOf(stringClassId),
        returnType = resultMatcherClassId
    )

    val resultMatchersViewMethodId = MethodId(
        classId = mockMvcResultMatchersClassId,
        name = "view",
        parameters = listOf(),
        returnType = viewResultMatchersClassId
    )

    val viewMatchersNameMethodId = MethodId(
        classId = viewResultMatchersClassId,
        name = "name",
        parameters = listOf(stringClassId),
        returnType = resultMatcherClassId
    )

    fun createMockMvcModel(idGenerator: () -> Int) =
        createBeanModel("mockMvc", idGenerator(), mockMvcClassId)

    fun createRequestBuilderModelOrNull(methodId: MethodId, arguments: List<UtModel>, idGenerator: () -> Int): UtModel? {
        check(methodId.parameters.size == arguments.size)

        if (methodId.isStatic) return null

        val requestMappingAnnotation = getRequestMappingAnnotationOrNull(methodId) ?: return null
        val requestMethod = getRequestMethodOrNull(requestMappingAnnotation) ?: return null

        @Suppress("UNCHECKED_CAST")
        val classRequestMappingAnnotation: Annotation? =
            methodId.classId.jClass.getAnnotation(requestMappingClassId.jClass as Class<out Annotation>)
        val classRequestPath = classRequestMappingAnnotation?.let { getRequestPathOrNull(it) }.orEmpty()

        val requestPath = classRequestPath + (getRequestPathOrNull(requestMappingAnnotation) ?: return null)

        val pathVariablesModel = createPathVariablesModel(methodId, arguments, idGenerator)

        val requestParamsModel = createRequestParamsModel(methodId, arguments, idGenerator)

        val urlTemplateModel = createUrlTemplateModel(requestPath, pathVariablesModel, requestParamsModel, idGenerator)

        var requestBuilderModel = UtAssembleModel(
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

        val headersContentModel = createHeadersContentModel(methodId, arguments, idGenerator)
        requestBuilderModel = addHeadersToRequestBuilderModel(headersContentModel, requestBuilderModel, idGenerator)

        val cookieValuesModel = createCookieValuesModel(methodId, arguments, idGenerator)
        requestBuilderModel =
            addCookiesToRequestBuilderModel(cookieValuesModel, requestBuilderModel, idGenerator)

        val requestAttributes = collectArgumentsWithAnnotationModels(methodId, requestAttributesClassId, arguments)
        requestBuilderModel =
            addRequestAttributesToRequestModelBuilder(requestAttributes, requestBuilderModel, idGenerator)

        val sessionAttributes = collectArgumentsWithAnnotationModels(methodId, sessionAttributesClassId, arguments)
        requestBuilderModel =
            addSessionAttributesToRequestModelBuilder(sessionAttributes, requestBuilderModel, idGenerator)

        val modelAttributes = collectArgumentsWithAnnotationModels(methodId, modelAttributesClassId, arguments)
        requestBuilderModel =
            addModelAttributesToRequestModelBuilder(modelAttributes, requestBuilderModel, idGenerator)

        return addContentToRequestBuilderModel(methodId, arguments, requestBuilderModel, idGenerator)
    }

    private fun addRequestAttributesToRequestModelBuilder(
        requestAttributes: Map<String, UtModel>,
        requestBuilderModel: UtAssembleModel,
        idGenerator: () -> Int
    ): UtAssembleModel = addAttributesToRequestBuilderModel(
        requestAttributes,
        requestAttributesMethodId,
        requestBuilderModel,
        idGenerator
    )


    private fun addSessionAttributesToRequestModelBuilder(
        sessionAttributes: Map<String, UtModel>,
        requestBuilderModel: UtAssembleModel,
        idGenerator: () -> Int
    ): UtAssembleModel = addAttributesToRequestBuilderModel(
        sessionAttributes,
        sessionAttributesMethodId,
        requestBuilderModel,
        idGenerator
    )

    private fun addModelAttributesToRequestModelBuilder(
        modelAttributes: Map<String, UtModel>,
        requestBuilderModel: UtAssembleModel,
        idGenerator: () -> Int
    ): UtAssembleModel = addAttributesToRequestBuilderModel(
        modelAttributes,
        modelAttributesMethodId,
        requestBuilderModel,
        idGenerator
    )


    private fun addAttributesToRequestBuilderModel(
        attributes: Map<String, UtModel>,
        addAttributesMethodId: MethodId,
        requestBuilderModel: UtAssembleModel,
        idGenerator: () -> Int
    ): UtAssembleModel{
        @Suppress("NAME_SHADOWING")
        var requestBuilderModel = requestBuilderModel

        attributes.forEach { (name, model) ->
            requestBuilderModel = UtAssembleModel(
                id = idGenerator(),
                classId = mockHttpServletRequestBuilderClassId,
                modelName = "requestBuilder",
                instantiationCall = UtExecutableCallModel(
                    instance = requestBuilderModel,
                    executable = addAttributesMethodId,
                    params = listOf(UtPrimitiveModel(name), model)
                )
            )
        }

        return requestBuilderModel
    }

    private fun addCookiesToRequestBuilderModel(
        cookieValuesModel: UtArrayModel,
        requestBuilderModel: UtAssembleModel,
        idGenerator: () -> Int
    ): UtAssembleModel {
        @Suppress("NAME_SHADOWING")
        var requestBuilderModel = requestBuilderModel

        if(cookieValuesModel.length > 0) {
            requestBuilderModel = UtAssembleModel(
                id = idGenerator(),
                classId = mockHttpServletRequestBuilderClassId,
                modelName = "requestBuilder",
                instantiationCall = UtExecutableCallModel(
                    instance = requestBuilderModel,
                    executable = mockHttpServletCookieMethodId,
                    params = listOf(cookieValuesModel)
                )
            )
        }
        return requestBuilderModel
    }

    private fun addHeadersToRequestBuilderModel(
        headersContentModel: UtAssembleModel,
        requestBuilderModel: UtAssembleModel,
        idGenerator: () -> Int
    ): UtAssembleModel {
        @Suppress("NAME_SHADOWING")
        var requestBuilderModel = requestBuilderModel

        if (headersContentModel.modificationsChain.isEmpty()) {
            return requestBuilderModel
        }

        val headers = UtAssembleModel(
            id = idGenerator(),
            classId = httpHeaderClassId,
            modelName = "headers",
            instantiationCall = UtExecutableCallModel(
                instance = null,
                executable = constructorId(httpHeaderClassId),
                params = emptyList(),
            ),
            modificationsChainProvider = {
                listOf(
                    UtExecutableCallModel(
                        instance = this,
                        executable = methodId(
                            classId = httpHeaderClassId,
                            name = "setAll",
                            returnType = voidClassId,
                            arguments = arrayOf(Map::class.java.id),
                        ),
                        params = listOf(headersContentModel)
                    )
                )
            }
        )

        requestBuilderModel = UtAssembleModel(
            id = idGenerator(),
            classId = mockHttpServletRequestBuilderClassId,
            modelName = "requestBuilder",
            instantiationCall = UtExecutableCallModel(
                instance = requestBuilderModel,
                executable = mockHttpServletHeadersMethodId,
                params = listOf(headers)
            )
        )

        return requestBuilderModel
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
                    executable = mockHttpServletContentTypeMethodId,
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
                    executable = mockHttpServletContentMethodId,
                    params = listOf(content)
                )
            )
        }
        return requestBuilderModel
    }

    private fun createCookieValuesModel(
        methodId: MethodId,
        arguments: List<UtModel>,
        idGenerator: () -> Int,
    ): UtArrayModel {
        val cookieValues = collectArgumentsWithAnnotationModels(methodId, cookieValueClassId, arguments)
            .mapValues { (_, model) -> convertModelValueToString(model) }.toList()

        // Creating an indexed Map for `UtArrayModel.stores`
        val indexedCookieValues = HashMap<Int, UtModel>()
        cookieValues.indices.forEach { ind ->
            indexedCookieValues[ind] = UtAssembleModel(
                id = idGenerator(),
                classId = cookieClassId,
                modelName = "cookie",
                instantiationCall = UtExecutableCallModel(
                    instance = null,
                    executable = constructorId(cookieClassId, stringClassId, stringClassId),
                    params = listOf(UtPrimitiveModel(cookieValues[ind].first), cookieValues[ind].second),
                )
            )
        }

        return UtArrayModel(
            id = idGenerator(),
            classId = getArrayClassIdByElementClassId(cookieClassId),
            length = cookieValues.size,
            constModel = UtNullModel(cookieClassId),
            stores = indexedCookieValues,
        )
    }

    private fun createHeadersContentModel(
        methodId: MethodId,
        arguments: List<UtModel>,
        idGenerator: () -> Int,
    ): UtAssembleModel {
        // Converts Map models values to String because `HttpHeaders.setAll(...)` method takes `Map<String, String>`
        val headersContent = collectArgumentsWithAnnotationModels(methodId, requestHeaderClassId, arguments)
            .mapValues { (_, model) -> convertModelValueToString(model) }

        return UtAssembleModel(
            id = idGenerator(),
            classId = Map::class.java.id,
            modelName = "headersContent",
            instantiationCall = UtExecutableCallModel(
                instance = null,
                executable = HashMap::class.java.getConstructor().executableId,
                params = emptyList()
            ),
            modificationsChainProvider = {
                headersContent.map { (name, value) ->
                    UtExecutableCallModel(
                        instance = this,
                        // Actually it is a `Map<String, String>`, but we use `Object::class.java` to avoid concrete failures
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

    private fun createPathVariablesModel(
        methodId: MethodId,
        arguments: List<UtModel>,
        idGenerator: () -> Int
    ): UtAssembleModel {
        val pathVariables = collectArgumentsWithAnnotationModels(methodId, pathVariableClassId, arguments)

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

    private fun createRequestParamsModel(
        methodId: MethodId,
        arguments: List<UtModel>,
        idGenerator: () -> Int
    ): List< Pair<UtPrimitiveModel, UtAssembleModel> > {
        val requestParams = collectArgumentsWithAnnotationModels(methodId, requestParamClassId, arguments)

        return requestParams.map { (name, value) ->
            Pair(UtPrimitiveModel(name),
                UtAssembleModel(
                    id = idGenerator(),
                    classId = listClassId,
                    modelName = "queryParams",
                    instantiationCall = UtExecutableCallModel(
                        instance = null,
                        executable = constructorId(java.util.ArrayList::class.id),
                        params = emptyList()
                    ),
                    modificationsChainProvider = {
                        listOf(
                            UtExecutableCallModel(
                                instance = this,
                                executable = methodId(
                                    classId = listClassId,
                                    name = "add",
                                    returnType = booleanClassId,
                                    arguments = arrayOf(Object::class.id),
                                ),
                                params = listOf(value)
                            )
                        )
                    }
                )
            )
        }
    }

    private fun collectArgumentsWithAnnotationModels(
        methodId: MethodId,
        annotationClassId: ClassId,
        arguments: List<UtModel>
    ): MutableMap<String, UtModel> {
        fun UtModel.isEmptyOptional(): Boolean {
            return classId == Optional::class.java.id && this is UtAssembleModel &&
                    instantiationCall is UtExecutableCallModel && instantiationCall.executable.name == "empty"
        }

        val argumentsModels = mutableMapOf<String, UtModel>()
        methodId.method.parameters.zip(arguments).forEach { (param, arg) ->
            @Suppress("UNCHECKED_CAST") val paramAnnotation =
                param.getAnnotation(annotationClassId.jClass as Class<out Annotation>) ?: return@forEach
            val name = (annotationClassId.jClass.getMethod("name").invoke(paramAnnotation) as? String).orEmpty()
                .ifEmpty { annotationClassId.jClass.getMethod("value").invoke(paramAnnotation) as? String }.orEmpty()
                .ifEmpty { param.name }

            if (arg.isNotNull() && !arg.isEmptyOptional()) {
                argumentsModels[name] = arg
            }
        }

        return argumentsModels
    }

    /**
     * Converts the model into a form that is understandable for annotations.
     * Example: UtArrayModel([UtPrimitiveModel("a"), UtPrimitiveModel("b"), UtPrimitiveModel("c")]) -> UtPrimitiveModel("a, b, c")
     *
     * There is known issue when using `model.toString()` is not reliable:
     *              https://github.com/UnitTestBot/UTBotJava/issues/2505
     * This issue may be improved in the future.
     */
    private fun convertModelValueToString(model: UtModel): UtModel {
        return UtPrimitiveModel(
            when(model){
                is UtArrayModel -> withToStringThreadLocalReentrancyGuard {
                    (0 until model.length).map { model.stores[it] ?: model.constModel }.joinToString(", ")
                }
                else -> model.toString()
            }
        )
    }

    private fun createUrlTemplateModel(
        requestPath: String,
        pathVariablesModel: UtAssembleModel,
        requestParamModel: List<Pair<UtPrimitiveModel, UtAssembleModel>>,
        idGenerator: () -> Int
    ): UtModel {
        val requestPathModel = UtPrimitiveModel(requestPath)
        return if (pathVariablesModel.modificationsChain.isEmpty() && requestParamModel.isEmpty()) requestPathModel
        else {
            var uriBuilderFromPath = UtAssembleModel(
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

            if(pathVariablesModel.modificationsChain.isNotEmpty()) {
                uriBuilderFromPath = UtAssembleModel(
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
            }

            requestParamModel.forEach { (name, value) ->
                uriBuilderFromPath = UtAssembleModel(
                    id = idGenerator(),
                    classId = uriComponentsBuilderClassId,
                    modelName = "uriBuilderWithRequestParam",
                    instantiationCall = UtExecutableCallModel(
                        instance = uriBuilderFromPath,
                        executable = MethodId(
                            classId = uriComponentsBuilderClassId,
                            name = "queryParam",
                            parameters = listOf(stringClassId, collectionClassId),
                            returnType = uriComponentsBuilderClassId
                        ),
                        params = listOf(name, value),
                    )
                )
            }

            return UtAssembleModel(
                id = idGenerator(),
                classId = stringClassId,
                modelName = "uriString",
                instantiationCall = UtExecutableCallModel(
                    instance = uriBuilderFromPath,
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
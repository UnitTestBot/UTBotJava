package org.utbot.quickcheck.internal

import org.javaruntype.type.*
import org.javaruntype.type.Type
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.internal.FakeAnnotatedTypeFactory.makeFrom
import org.utbot.quickcheck.internal.Items.choose
import org.utbot.quickcheck.random.SourceOfRandomness
import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.context.MethodGenericsContext
import java.lang.reflect.*

class ParameterTypeContext(
    private val parameterName: String,
    private val parameterType: AnnotatedType,
    internal val declarerName: String,
    val resolved: Type<*>,
    val generics: GenericsContext,
    private val parameterIndex: Int = -1
) {
    private val explicits = mutableListOf<Weighted<Generator>>()
    private var annotatedElement: AnnotatedElement? = null
    private var allowMixedTypes = false

    fun annotate(element: AnnotatedElement?): ParameterTypeContext {
        annotatedElement = element
        return this
    }

    fun allowMixedTypes(value: Boolean): ParameterTypeContext {
        allowMixedTypes = value
        return this
    }

    fun allowMixedTypes(): Boolean {
        return allowMixedTypes
    }

    /**
     * Gives a context for generation of the return type of a lambda method.
     *
     * @param method method whose return type we want to resolve
     * @return an associated parameter context
     */
    fun methodReturnTypeContext(method: Method): ParameterTypeContext {
        check(generics is MethodGenericsContext) { "invoking methodReturnTypeContext in present of $generics" }
        val argMethodGenerics = generics.parameterType(parameterIndex).method(method)
        return ParameterTypeContext(
            "return value",
            method.annotatedReturnType,
            method.name,
            Types.forJavaLangReflectType(argMethodGenerics.resolveReturnType()),
            argMethodGenerics
        )
    }

    private fun makeGenerator(
        generatorType: Class<out Generator>
    ): Generator {
        val ctor = try {
            // for Ctor/Fields
            Reflection.findConstructor(generatorType, Class::class.java)
        } catch (ex: ReflectionException) {
            return Reflection.instantiate(generatorType)
        }
        return Reflection.instantiate(ctor, rawParameterType())
    }

    private fun rawParameterType(): Class<*> {
        return when {
            type() is ParameterizedType -> resolved.rawClass
            type() is TypeVariable<*> -> resolved.rawClass
            else -> type() as Class<*>
        }
    }

    fun name(): String {
        return "$declarerName:$parameterName"
    }

    fun annotatedType(): AnnotatedType {
        return parameterType
    }

    fun type(): java.lang.reflect.Type {
        return parameterType.type
    }

    /**
     * @see [
     * this issue](https://github.com/pholser/junit-quickcheck/issues/77)
     *
     * @return the annotated program element this context represents
     */
    @Deprecated(
        """This will likely go away when languages whose compilers
      and interpreters produce class files that support annotations on type
      uses.
      """
    )
    fun annotatedElement(): AnnotatedElement? {
        return annotatedElement
    }

    /**
     * @see [
     * this issue](https://github.com/pholser/junit-quickcheck/issues/77)
     *
     * @return the annotated program element this context represents
     */
    @Deprecated(
        """This will likely go away when languages whose compilers
      and interpreters produce class files that support annotations on type
      uses.
      """
    )
    fun topLevel(): Boolean {
        return (annotatedElement is Parameter || annotatedElement is Field)
    }

    fun explicitGenerators(): List<Weighted<Generator>> {
        return explicits
    }

    private fun addParameterTypeContextToDeque(deque: ArrayDeque<ParameterTypeContext>, ptx: ParameterTypeContext) {
        if (ptx.resolved.name == Zilch::class.java.name) return
        deque.add(ptx)
    }

    fun getAllSubParameterTypeContexts(sourceOfRandomness: SourceOfRandomness?): List<ParameterTypeContext> {
        val res = mutableListOf(this)
        val deque = ArrayDeque<ParameterTypeContext>()
        if (isArray) {
            addParameterTypeContextToDeque(deque, arrayComponentContext())
        }
        typeParameterContexts(sourceOfRandomness).forEach { ptx: ParameterTypeContext ->
            addParameterTypeContextToDeque(deque, ptx)
        }
        while (!deque.isEmpty()) {
            val ptx = deque.removeFirst()
            res.add(ptx)
            if (ptx.isArray) {
                addParameterTypeContextToDeque(deque, ptx.arrayComponentContext())
            }
            ptx.typeParameterContexts(sourceOfRandomness).forEach { ptxNested: ParameterTypeContext ->
                addParameterTypeContextToDeque(deque, ptxNested)
            }
        }
        return res
    }

    fun arrayComponentContext(): ParameterTypeContext {
        val component = Types.arrayComponentOf(resolved as Type<Array<Any>>)
        val annotatedComponent = annotatedArrayComponent(component)
        return ParameterTypeContext(
            annotatedComponent.type.typeName,
            annotatedComponent,
            parameterType.type.typeName,
            component,
            generics
        ).annotate(annotatedComponent).allowMixedTypes(true)
    }

    private fun annotatedArrayComponent(component: Type<*>): AnnotatedType {
        return if (parameterType is AnnotatedArrayType) {
            parameterType.annotatedGenericComponentType
        } else {
            makeFrom(component.componentClass)
        }
    }

    val isArray: Boolean
        get() = resolved.isArray
    val rawClass: Class<*>
        get() = resolved.rawClass
    val isEnum: Boolean
        get() = rawClass.isEnum
    val typeParameters: List<TypeParameter<*>>
        get() = resolved.typeParameters

    fun typeParameterContexts(random: SourceOfRandomness?): List<ParameterTypeContext> {
        val typeParamContexts = mutableListOf<ParameterTypeContext>()
        val typeParameters = typeParameters
        val annotatedTypeParameters = Reflection.annotatedComponentTypes(annotatedType())
        for (i in typeParameters.indices) {
            val p = typeParameters[i]
            val a = if (annotatedTypeParameters.size > i) annotatedTypeParameters[i] else zilch()
            when (p) {
                is StandardTypeParameter<*> -> addStandardTypeParameterContext(typeParamContexts, p, a)
                is WildcardTypeParameter -> addWildcardTypeParameterContext(typeParamContexts, a)
                is ExtendsTypeParameter<*> -> addExtendsTypeParameterContext(typeParamContexts, p, a)
                else -> {
                    // must be "? super X"
                    addSuperTypeParameterContext(random, typeParamContexts, p, a)
                }
            }
        }
        return typeParamContexts
    }

    private fun addStandardTypeParameterContext(
        typeParameterContexts: MutableList<ParameterTypeContext>,
        p: TypeParameter<*>,
        a: AnnotatedType
    ) {
        typeParameterContexts.add(
            ParameterTypeContext(
                p.type.name,
                a,
                annotatedType().type.typeName,
                p.type,
                generics
            ).allowMixedTypes(a !is TypeVariable<*>).annotate(a)
        )
    }

    private fun addWildcardTypeParameterContext(
        typeParameterContexts: MutableList<ParameterTypeContext>,
        a: AnnotatedType
    ) {
        typeParameterContexts.add(
            ParameterTypeContext(
                "Zilch",
                a,
                annotatedType().type.typeName,
                Types.forJavaLangReflectType(Zilch::class.java),
                GenericsResolver.resolve(Zilch::class.java)
            ).allowMixedTypes(true).annotate(a)
        )
    }

    private fun addExtendsTypeParameterContext(
        typeParameterContexts: MutableList<ParameterTypeContext>,
        p: TypeParameter<*>,
        a: AnnotatedType
    ) {
        typeParameterContexts.add(
            ParameterTypeContext(
                p.type.name,
                Reflection.annotatedComponentTypes(a)[0],
                annotatedType().type.typeName,
                p.type,
                generics
            ).allowMixedTypes(false).annotate(a)
        )
    }

    private fun addSuperTypeParameterContext(
        random: SourceOfRandomness?,
        typeParameterContexts: MutableList<ParameterTypeContext>,
        p: TypeParameter<*>,
        a: AnnotatedType
    ) {
        val supertypes = Reflection.supertypes(p.type)
        val choice = choose(supertypes, random!!)
        typeParameterContexts.add(
            ParameterTypeContext(
                p.type.name,
                Reflection.annotatedComponentTypes(a)[0],
                annotatedType().type.typeName,
                choice,
                generics
            ).allowMixedTypes(false).annotate(a)
        )
    }

    companion object {
        @Suppress("unused")
        @JvmStatic
        private val zilch: Zilch = Zilch

        fun forType(type: java.lang.reflect.Type): ParameterTypeContext {
            return forType(type, null)
        }

        fun forType(type: java.lang.reflect.Type, generics: GenericsContext?): ParameterTypeContext {
            val gctx: GenericsContext = generics ?: GenericsResolver.resolve(type.javaClass)
            return ParameterTypeContext(
                type.typeName,
                FakeAnnotatedTypeFactoryWithType.makeFrom(type),
                type.typeName,
                Types.forJavaLangReflectType(type),
                gctx
            )
        }

        fun forClass(clazz: Class<*>): ParameterTypeContext {
            return ParameterTypeContext(
                clazz.typeName,
                makeFrom(clazz),
                clazz.typeName,
                Types.forJavaLangReflectType(clazz),
                GenericsResolver.resolve(clazz)
            )
        }

        fun forField(field: Field): ParameterTypeContext {
            val generics = GenericsResolver.resolve(field.declaringClass)
            return ParameterTypeContext(
                field.name,
                field.annotatedType,
                field.declaringClass.name,
                Types.forJavaLangReflectType(generics.resolveFieldType(field)),
                generics
            )
        }
        fun forField(field: Field, generics: GenericsContext): ParameterTypeContext {
            return ParameterTypeContext(
                field.name,
                field.annotatedType,
                field.declaringClass.name,
                Types.forJavaLangReflectType(generics.resolveFieldType(field)),
                generics
            )
        }
        fun forParameter(parameter: Parameter): ParameterTypeContext {
            val exec = parameter.declaringExecutable
            val clazz = exec.declaringClass
            val declarerName = clazz.name + '.' + exec.name
            val parameterIndex = parameterIndex(exec, parameter)
            val generics: GenericsContext
            val resolved: Type<*>
            when (exec) {
                is Method -> {
                    val methodGenerics = GenericsResolver.resolve(clazz).method(exec)
                    resolved = Types.forJavaLangReflectType(
                        methodGenerics.resolveParameterType(parameterIndex)
                    )
                    generics = methodGenerics
                }

                is Constructor<*> -> {
                    val constructorGenerics = GenericsResolver.resolve(clazz).constructor(exec)
                    resolved = Types.forJavaLangReflectType(
                        constructorGenerics.resolveParameterType(parameterIndex)
                    )
                    generics = constructorGenerics
                }

                else -> {
                    throw IllegalStateException("Unrecognized subtype of Executable")
                }
            }
            return ParameterTypeContext(
                parameter.name,
                parameter.annotatedType,
                declarerName,
                resolved,
                generics,
                parameterIndex
            )
        }

        fun forParameter(
            parameter: Parameter,
            generics: MethodGenericsContext
        ): ParameterTypeContext {
            val exec = parameter.declaringExecutable
            val clazz = exec.declaringClass
            val declarerName = clazz.name + '.' + exec.name
            val parameterIndex = parameterIndex(exec, parameter)
            return ParameterTypeContext(
                parameter.name,
                parameter.annotatedType,
                declarerName,
                Types.forJavaLangReflectType(
                    generics.resolveParameterType(parameterIndex)
                ),
                generics,
                parameterIndex
            )
        }

        private fun parameterIndex(exec: Executable, parameter: Parameter): Int {
            val parameters = exec.parameters
            for (i in parameters.indices) {
                if (parameters[i] == parameter) return i
            }
            throw IllegalStateException(
                "Cannot find parameter $parameter on $exec"
            )
        }

        private fun zilch(): AnnotatedType {
            return try {
                ParameterTypeContext::class.java.getDeclaredField("zilch").annotatedType
            } catch (e: NoSuchFieldException) {
                throw AssertionError(e)
            }
        }
    }
}
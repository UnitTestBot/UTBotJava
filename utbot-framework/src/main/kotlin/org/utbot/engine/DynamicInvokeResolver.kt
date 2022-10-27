package org.utbot.engine

import soot.SootClass
import soot.Type
import soot.jimple.StringConstant
import soot.jimple.internal.JDynamicInvokeExpr


/**
 * Map of supported boostrap function names to their resolvers
 */
private val defaultProcessors = mapOf(
    // referencing by a name instead of a signature, because we believe, that all bootstrap methods have unique names
    StringMakeConcatResolver.MAKE_CONCAT_METHOD_NAME to StringMakeConcatResolver,
    StringMakeConcatResolver.MAKE_CONCAT_WITH_CONSTANTS_NAME to StringMakeConcatResolver
)

interface DynamicInvokeResolver {
    /**
     * @return a successfully resolved [Invocation] or `null`, if it can't be resolved with this [DynamicInvokeResolver]
     */
    context(Traverser)
    fun TraversalContext.resolveDynamicInvoke(invokeExpr: JDynamicInvokeExpr): Invocation?
}

/**
 * Composes several [DynamicInvokeResolver]s into a single [DynamicInvokeResolver] based on bootstrap method names.
 */
class DelegatingDynamicInvokeResolver(
    private val bootstrapMethodToProcessor: Map<String, DynamicInvokeResolver> = defaultProcessors
) : DynamicInvokeResolver {
    context(Traverser)
    override fun TraversalContext.resolveDynamicInvoke(invokeExpr: JDynamicInvokeExpr): Invocation? {
        val processor = bootstrapMethodToProcessor[invokeExpr.bootstrapMethod.name] ?: return null
        return with(processor) { resolveDynamicInvoke(invokeExpr) }
    }

}

/**
 * Implements the logic of [java.lang.invoke.StringConcatFactory].
 *
 * This is useful when analyzing only Java 9+ bytecode.
 */
object StringMakeConcatResolver : DynamicInvokeResolver {
    const val STRING_CONCAT_LIBRARY_NAME = "java.lang.invoke.StringConcatFactory"
    const val MAKE_CONCAT_METHOD_NAME = "makeConcat"
    const val MAKE_CONCAT_WITH_CONSTANTS_NAME = "makeConcatWithConstants"

    /**
     * Implements the logic of [java.lang.invoke.StringConcatFactory.makeConcat] and
     * [java.lang.invoke.StringConcatFactory.makeConcatWithConstants] in a symbolic way.
     *
     * - Generates [soot.SootMethod] performing string concatenation if it has not generated yet
     * - Links it
     *
     * Check out [java.lang.invoke.StringConcatFactory] documentation for more details.
     *
     * @return [Invocation] with a single generated [soot.SootMethod], which represents a concatenating function.
     */
    context(Traverser)
    override fun TraversalContext.resolveDynamicInvoke(invokeExpr: JDynamicInvokeExpr): Invocation {
        val bootstrapMethod = invokeExpr.bootstrapMethod
        require(bootstrapMethod.declaringClass.name == STRING_CONCAT_LIBRARY_NAME)

        val bootstrapArguments = invokeExpr.bootstrapArgs.map { arg ->
            requireNotNull((arg as? StringConstant)?.value) { "StringConstant expected, but got ${arg::class.java}"}
        }

        val (recipe, constants) = when (bootstrapMethod.name) {
            MAKE_CONCAT_METHOD_NAME -> {
                "\u0001".repeat(invokeExpr.args.size) to emptyList()
            }
            MAKE_CONCAT_WITH_CONSTANTS_NAME -> {
                val recipe = requireNotNull(bootstrapArguments.firstOrNull()) { "At least one bootstrap argument expected" }
                recipe to bootstrapArguments.drop(1)
            }
            else -> error("Unknown bootstrap method for string concatenation!")
        }

        val declaringClass = environment.method.declaringClass
        val dynamicParameterTypes = invokeExpr.methodRef.parameterTypes

        val parameters = resolveParameters(invokeExpr.args, dynamicParameterTypes)
        return makeInvocation(declaringClass, recipe, dynamicParameterTypes, constants, parameters)
    }

    private fun makeInvocation(
        declaringClass: SootClass,
        recipe: String,
        dynamicParameterTypes: MutableList<Type>,
        constants: List<String>,
        parameters: List<SymbolicValue>
    ): Invocation {
        val sootMethod = makeSootConcat(
            declaringClass,
            recipe,
            dynamicParameterTypes,
            constants
        )

        val invocationTarget = InvocationTarget(
            instance = null,
            sootMethod
        )

        return Invocation(
            instance = null,
            sootMethod,
            parameters,
            invocationTarget
        )
    }
}
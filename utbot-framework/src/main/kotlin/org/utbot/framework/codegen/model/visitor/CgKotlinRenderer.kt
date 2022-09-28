package org.utbot.framework.codegen.model.visitor

import org.apache.commons.text.StringEscapeUtils
import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.framework.codegen.RegularImport
import org.utbot.framework.codegen.StaticImport
import org.utbot.framework.codegen.isLanguageKeyword
import org.utbot.framework.codegen.model.tree.AbstractCgClass
import org.utbot.framework.codegen.model.tree.CgAllocateArray
import org.utbot.framework.codegen.model.tree.CgAllocateInitializedArray
import org.utbot.framework.codegen.model.tree.CgAnonymousFunction
import org.utbot.framework.codegen.model.tree.CgArrayAnnotationArgument
import org.utbot.framework.codegen.model.tree.CgArrayElementAccess
import org.utbot.framework.codegen.model.tree.CgArrayInitializer
import org.utbot.framework.codegen.model.tree.CgAuxiliaryClass
import org.utbot.framework.codegen.model.tree.CgComparison
import org.utbot.framework.codegen.model.tree.CgConstructorCall
import org.utbot.framework.codegen.model.tree.CgDeclaration
import org.utbot.framework.codegen.model.tree.CgEqualTo
import org.utbot.framework.codegen.model.tree.CgErrorTestMethod
import org.utbot.framework.codegen.model.tree.CgErrorWrapper
import org.utbot.framework.codegen.model.tree.CgExecutableCall
import org.utbot.framework.codegen.model.tree.CgExpression
import org.utbot.framework.codegen.model.tree.CgFieldAccess
import org.utbot.framework.codegen.model.tree.CgForLoop
import org.utbot.framework.codegen.model.tree.CgGetJavaClass
import org.utbot.framework.codegen.model.tree.CgGetKotlinClass
import org.utbot.framework.codegen.model.tree.CgGetLength
import org.utbot.framework.codegen.model.tree.CgInnerBlock
import org.utbot.framework.codegen.model.tree.CgMethod
import org.utbot.framework.codegen.model.tree.CgNotNullAssertion
import org.utbot.framework.codegen.model.tree.CgParameterDeclaration
import org.utbot.framework.codegen.model.tree.CgParameterizedTestDataProviderMethod
import org.utbot.framework.codegen.model.tree.CgRegularClass
import org.utbot.framework.codegen.model.tree.CgSimpleRegion
import org.utbot.framework.codegen.model.tree.CgSpread
import org.utbot.framework.codegen.model.tree.CgStaticsRegion
import org.utbot.framework.codegen.model.tree.CgSwitchCase
import org.utbot.framework.codegen.model.tree.CgSwitchCaseLabel
import org.utbot.framework.codegen.model.tree.CgTestClass
import org.utbot.framework.codegen.model.tree.CgTestClassBody
import org.utbot.framework.codegen.model.tree.CgTestMethod
import org.utbot.framework.codegen.model.tree.CgTypeCast
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.codegen.model.util.CgPrinter
import org.utbot.framework.codegen.model.util.CgPrinterImpl
import org.utbot.framework.codegen.model.util.nullLiteral
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.TypeParameters
import org.utbot.framework.plugin.api.WildcardTypeParameter
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.isPrimitiveWrapper
import org.utbot.framework.plugin.api.util.kClass
import org.utbot.framework.plugin.api.util.voidClassId

//TODO rewrite using KtPsiFactory?
internal class CgKotlinRenderer(context: CgRendererContext, printer: CgPrinter = CgPrinterImpl()) : CgAbstractRenderer(context, printer) {
    override val statementEnding: String = ""

    override val logicalAnd: String
        get() = "and"

    override val logicalOr: String
        get() = "or"

    override val language: CodegenLanguage = CodegenLanguage.KOTLIN

    override val langPackage: String = "kotlin"

    override fun visit(element: AbstractCgClass<*>) {
        for (annotation in element.annotations) {
            annotation.accept(this)
        }

        renderClassVisibility(element.id)
        renderClassModality(element)
        if (!element.isStatic && element.isNested) {
            print("inner ")
        }
        print("class ")
        print(element.simpleName)

        if (element.superclass != null || element.interfaces.isNotEmpty()) {
            print(" :")
        }
        val supertypes = mutableListOf<String>()
            .apply {
                // Here we do not consider constructors with arguments, but for now they are not needed.
                // Also, we do not yet support type parameters in code generation, so generic
                // superclasses or interfaces are not supported. Although, they are not needed for now.
                val superclass = element.superclass
                if (superclass != null) {
                    add("${superclass.asString()}()")
                }
                element.interfaces.forEach {
                    add(it.asString())
                }
            }.joinToString()
        if (supertypes.isNotEmpty()) {
            print(" $supertypes")
        }
        println(" {")
        withIndent { element.body.accept(this) }
        println("}")
    }

    override fun visit(element: CgTestClassBody) {
        // render regions for test methods
        for ((i, region) in (element.testMethodRegions + element.nestedClassRegions).withIndex()) {
            if (i != 0) println()

            region.accept(this)
        }

        if (element.staticDeclarationRegions.isEmpty()) {
            return
        }
        // render static declaration regions inside a companion object
        println()

        // In Kotlin, we put static declarations in a companion object of the class,
        // but that **does not** apply to nested classes.
        // They must be located in the class itself, not its companion object.
        // That is why here we extract all the auxiliary classes from static regions
        // to form a separate region specifically for them.
        // See the docs on CgAuxiliaryClass for details on what they represent.
        val auxiliaryClassesRegion = element.staticDeclarationRegions
            .flatMap { it.content }
            .filterIsInstance<CgAuxiliaryClass>()
            .let { classes -> CgSimpleRegion("Util classes", classes) }

        if (auxiliaryClassesRegion.content.isNotEmpty()) {
            auxiliaryClassesRegion.accept(this)
            println()
        }

        // Here we update the static regions by removing all the auxiliary classes from them.
        // The remaining content of regions will be rendered inside a companion object.
        val updatedStaticRegions = element.staticDeclarationRegions.map { region ->
            val updatedContent = region.content.filterNot { it is CgAuxiliaryClass }
            CgStaticsRegion(region.header, updatedContent)
        }

        renderCompanionObject {
            for ((i, staticsRegion) in updatedStaticRegions.withIndex()) {
                if (i != 0) println()

                staticsRegion.accept(this)
            }
        }
    }

    /**
     * Build a companion object.
     * @param body a lambda that contains the logic of construction of a companion object's body
     */
    private fun renderCompanionObject(body: () -> Unit) {
        println("companion object {")
        withIndent(body)
        println("}")
    }

    override fun visit(element: CgStaticsRegion) {
        if (element.content.isEmpty()) return

        print(regionStart)
        element.header?.let { print(" $it") }
        println()

        for (item in element.content) {
            println()
            println("@JvmStatic")
            item.accept(this)
        }

        println(regionEnd)
    }


    // Property access

    override fun visit(element: CgFieldAccess) {
        element.caller.accept(this)
        renderAccess(element.caller)
        print(element.fieldId.name)
    }

    override fun visit(element: CgArrayElementAccess) {
        if (element.array.type.isNullable) {
            element.array.accept(this)
            print("?.get(")
            element.index.accept(this)
            print(")")
        } else {
            super.visit(element)
        }
    }

    override fun renderAccess(caller: CgExpression) {
        if (caller.type.isNullable) print("?")
        print(".")
    }

    override fun visit(element: CgParameterDeclaration) {
        if (element.isVararg) {
            print("vararg ")
        }
        print("${element.name.escapeNamePossibleKeyword()}: ")
        print(getKotlinClassString(element.type))
        if (element.isReferenceType) {
            print("?")
        }
    }

    // TODO: probably rewrite to use better syntax if there is one
    override fun visit(element: CgArrayAnnotationArgument) {
        print("value = [")
        element.values.renderSeparated()
        print("]")
    }

    override fun visit(element: CgSpread) {
        print("*")
        element.array.accept(this)
    }

    override fun visit(element: CgAnonymousFunction) {
        print("{ ")
        element.parameters.renderSeparated(true)
        if (element.parameters.isNotEmpty()) {
            print(" -> ")
        } else {
            println()
        }
        // cannot use visit(element.body) here because { was already printed
        withIndent {
            for (statement in element.body) {
                statement.accept(this)
            }
        }
        print("}")
    }

    override fun visit(element: CgEqualTo) {
        element.left.accept(this)
        print(" === ")
        element.right.accept(this)
    }

    /**
     * Sometimes we can omit rendering type cast and simply render its [CgTypeCast.expression] instead.
     * This method checks if the type cast can be omitted.
     *
     * For example, type cast can be omitted when a primitive wrapper is cast to its corresponding primitive (or vice versa),
     * because in Kotlin there are no primitive types as opposed to Java.
     *
     * Also, sometimes we can omit type cast when the [CgTypeCast.targetType] is the same as the type of [CgTypeCast.expression].
     */
    private fun isCastNeeded(element: CgTypeCast): Boolean {
        val targetType = element.targetType
        val expressionType = element.expression.type

        val isPrimitiveToWrapperCast = targetType.isPrimitiveWrapper && expressionType.isPrimitive
        val isWrapperToPrimitiveCast = targetType.isPrimitive && expressionType.isPrimitiveWrapper
        val isNullLiteral = element.expression == nullLiteral()

        if (!isNullLiteral && element.isSafetyCast && (isPrimitiveToWrapperCast || isWrapperToPrimitiveCast)) {
            return false
        }

        // perform type cast only if target type is not equal to expression type
        // but cast from nullable to not nullable should be performed
        // TODO SAT-1445 actually this safetyCast check looks like hack workaround and possibly does not work
        //  so it should be carefully tested one day
        return !element.isSafetyCast || expressionType != targetType
    }

    override fun visit(element: CgTypeCast) {
        if (!isCastNeeded(element)) {
            element.expression.accept(this)
        } else {
            print("(")

            element.expression.accept(this)

            if (element.isSafetyCast) print(" as? ") else print(" as ")
            print(getKotlinClassString(element.targetType))
            renderTypeParameters(element.targetType.typeParameters)
            val initNullable = element.type.isNullable
            if (element.targetType.isNullable || initNullable) print("?")

            print(")")
        }
    }

    override fun visit(element: CgErrorWrapper) {
        element.expression.accept(this)
        print(" ?: error(\"${element.message}\")")
    }

    override fun visit(element: CgGetJavaClass) {
        // TODO: check how it works on ref types, primitives, ref arrays, primitive arrays, etc.
        print(getKotlinClassString(element.classId))
        print("::class.java")
    }

    override fun visit(element: CgGetKotlinClass) {
        // TODO: check how it works on ref types, primitives, ref arrays, primitive arrays, etc.
        print(getKotlinClassString(element.classId))
        print("::class")
    }

    override fun visit(element: CgNotNullAssertion) {
        element.expression.accept(this)
        print("!!")
    }

    override fun visit(element: CgAllocateArray) {
        // TODO think about void as primitive
        print(getKotlinClassString(element.type))
        print("(${element.size})")
        if (!element.elementType.isPrimitive) {
            print(" { null }")
        }
    }

    override fun visit(element: CgAllocateInitializedArray) {
        print(getKotlinClassString(element.type))
        print("(${element.size})")
        print(" {")
        element.initializer.accept(this)
        print(" }")
    }

    override fun visit(element: CgArrayInitializer) {
        val elementType = element.elementType
        val elementsInLine = arrayElementsInLine(elementType)

        if (elementType.isPrimitive) {
            val prefix = elementType.name.toLowerCase()
            print("${prefix}ArrayOf(")
            element.values.renderElements(elementsInLine)
            print(")")
        } else {
            print(getKotlinClassString(element.arrayType))
            print("(${element.size})")
            print(" { null }")
        }
    }

    override fun visit(element: CgGetLength) {
        element.variable.accept(this)
        print(".size")
    }

    override fun visit(element: CgConstructorCall) {
        print(getKotlinClassString(element.executableId.classId))
        print("(")
        element.arguments.renderSeparated()
        print(")")
    }

    override fun renderRegularImport(regularImport: RegularImport) {
        val escapedImport = getEscapedImportRendering(regularImport)
        println("import $escapedImport$statementEnding")
    }

    override fun renderStaticImport(staticImport: StaticImport) {
        val escapedImport = getEscapedImportRendering(staticImport)
        println("import $escapedImport$statementEnding")
    }

    override fun renderMethodSignature(element: CgTestMethod) {
        print("fun ")
        // TODO resolve $ in name
        print(element.name)
        print("(")
        val newLines = element.parameters.size > maxParametersAmountInOneLine
        element.parameters.renderSeparated(newLines)
        print(")")
        renderMethodReturnType(element)
    }

    override fun renderMethodSignature(element: CgErrorTestMethod) {
        // error test methods always have void return type
        print("fun ")
        print(element.name)
        println("()")
    }

    override fun renderMethodSignature(element: CgParameterizedTestDataProviderMethod) {
        val returnType = getKotlinClassString(element.returnType)
        println("fun ${element.name}(): $returnType")
    }

    private fun renderMethodReturnType(method: CgMethod) {
        if (method.returnType != voidClassId) {
            print(": ")
            print(getKotlinClassString(method.returnType))
        }
    }

    override fun visit(element: CgInnerBlock) {
        println("run {")
        withIndent {
            for (statement in element.statements) {
                statement.accept(this)
            }
        }
        println("}")
    }

    override fun renderForLoopVarControl(element: CgForLoop) {
        print("for (")
        with(element.initialization) {
            visit(variable)
            print(" in ")
            initializer?.accept(this@CgKotlinRenderer)
        }
        print(" until ")
        (element.condition as CgComparison).right.accept(this) // TODO it is comparison just now
        // TODO is step always increment?
    }

    private fun renderKotlinTypeInDeclaration(element: CgDeclaration) {
        // TODO consider moving to getKotlinClassString
        print(": ")
        print(getKotlinClassString(element.variableType))
        renderTypeParameters(element.variableType.typeParameters)
        val initNullable = element.initializer?.run { type.isNullable } ?: false
        if (element.variableType.isNullable || initNullable) print("?")
    }

    override fun renderDeclarationLeftPart(element: CgDeclaration) {
        if (element.isMutable) print("var ") else print("val ")
        visit(element.variable)
        // TODO SAT-1683 enable explicit types
        // renderKotlinTypeInDeclaration(element)
    }

    override fun visit(element: CgSwitchCaseLabel) {
        if (element.label != null) {
            element.label.accept(this)
        } else {
            print("else")
        }
        print(" -> ")
        visit(element.statements, printNextLine = true)
    }

    override fun visit(element: CgSwitchCase) {
        print("when (")
        element.value.accept(this)
        println(") {")
        withIndent {
            for (caseLabel in element.labels) {
                caseLabel.accept(this)
            }
            element.defaultLabel?.accept(this)
        }
        println("}")
    }

    override fun toStringConstantImpl(byte: Byte): String = buildString {
        if (byte < 0) {
            append("(")
            append("$byte")
            append(")")
        } else {
            append("$byte")
        }
        append(".toByte()")
    }

    override fun toStringConstantImpl(short: Short): String = buildString {
        if (short < 0) {
            append("(")
            append("$short")
            append(")")
        } else {
            append("$short")
        }
        append(".toShort()")
    }

    override fun toStringConstantImpl(long: Long): String = "${long}L"

    override fun toStringConstantImpl(float: Float): String = "${float}f"

    override fun toStringConstantImpl(int: Int): String = when (int) {
        Int.MAX_VALUE -> "Int.MAX_VALUE"
        Int.MIN_VALUE -> "Int.MIN_VALUE"
        else -> "$int"
    }

    /**
     * See [Escaping in Kotlin](https://stackoverflow.com/questions/44170959/kotlin-form-feed-character-illegal-escape-f/)
     */
    override fun String.escapeCharacters(): String =
        StringEscapeUtils.escapeJava(this)
            .replace("$", "\\$")
            .replace("\\f", "\\u000C")
            .replace("\\xxx", "\\\u0058\u0058\u0058")

    override fun renderExecutableCallArguments(executableCall: CgExecutableCall) {
        print("(")
        val lastArgument = executableCall.arguments.lastOrNull()
        if (lastArgument != null && lastArgument is CgAnonymousFunction) {
            executableCall.arguments.dropLast(1).renderSeparated()
            print(") ")
            executableCall.arguments.last().accept(this)
        } else {
            executableCall.arguments.renderSeparated()
            print(")")
        }
    }

    override fun renderExceptionCatchVariable(exception: CgVariable) {
        print("${exception.name.escapeNamePossibleKeyword()}: ${exception.type.kClass.simpleName}")
    }

    override fun isAccessibleBySimpleNameImpl(classId: ClassId): Boolean {
        return super.isAccessibleBySimpleNameImpl(classId) || classId.packageName == "kotlin"
    }

    override fun escapeNamePossibleKeywordImpl(s: String): String =
        if (isLanguageKeyword(s, context.codegenLanguage)) "`$s`" else s

    override fun renderClassVisibility(classId: ClassId) {
        when {
            // Kotlin classes are public by default
            classId.isPublic -> Unit
            classId.isProtected -> print("protected ")
            classId.isPrivate -> print("private ")
        }
    }

    override fun renderClassModality(aClass: AbstractCgClass<*>) {
        when (aClass) {
            is CgTestClass -> Unit
            // Kotlin classes are final by default
            is CgRegularClass -> if (!aClass.id.isFinal) print("open ")
        }
    }

    private fun getKotlinClassString(id: ClassId): String =
        if (id.isArray) {
            getKotlinArrayClassOfString(id)
        } else {
            when (id.jvmName) {
                "Ljava/lang/Object;" -> Any::class.simpleName!!
                "B", "Ljava/lang/Byte;" -> Byte::class.simpleName!!
                "S", "Ljava/lang/Short;" -> Short::class.simpleName!!
                "C", "Ljava/lang/Character;" -> Char::class.simpleName!!
                "I", "Ljava/lang/Integer;" -> Int::class.simpleName!!
                "J", "Ljava/lang/Long;" -> Long::class.simpleName!!
                "F", "Ljava/lang/Float;" -> Float::class.simpleName!!
                "D", "Ljava/lang/Double;" -> Double::class.simpleName!!
                "Z", "Ljava/lang/Boolean;" -> Boolean::class.simpleName!!
                "Ljava/lang/CharSequence;" -> CharSequence::class.simpleName!!
                "Ljava/lang/String;" -> String::class.simpleName!!
                else -> {
                    // we cannot access kClass for BuiltinClassId
                    // we cannot use simple name here because this class can be not imported
                    if (id is BuiltinClassId) id.canonicalName else id.kClass.id.asString()
                }
            }
        }

    private fun getKotlinArrayClassOfString(classId: ClassId): String =
        if (!classId.elementClassId!!.isPrimitive) {
            if (classId.elementClassId != java.lang.Object::class.id) {
                workaround(WorkaroundReason.ARRAY_ELEMENT_TYPES_ALWAYS_NULLABLE) {
                    // for now all element types are nullable
                    // Use kotlin.Array, because Array becomes java.lang.reflect.Array
                    // when passed as a type parameter
                    "kotlin.Array<${getKotlinClassString(classId.elementClassId!!)}?>"
                }
            } else {
                "kotlin.Array<Any?>"
            }
        } else {
            when (classId.jvmName) {
                "[B" -> ByteArray::class.simpleName!!
                "[S" -> ShortArray::class.simpleName!!
                "[C" -> CharArray::class.simpleName!!
                "[I" -> IntArray::class.simpleName!!
                "[J" -> LongArray::class.simpleName!!
                "[F" -> FloatArray::class.simpleName!!
                "[D" -> DoubleArray::class.simpleName!!
                "[Z" -> BooleanArray::class.simpleName!!
                else -> classId.kClass.id.asString()
            }
        }

    override fun renderTypeParameters(typeParameters: TypeParameters) {
        if (typeParameters.parameters.isNotEmpty()) {
            print("<")
            if (typeParameters is WildcardTypeParameter) {
                print("*")
            } else {
                print(typeParameters.parameters.joinToString { getKotlinClassString(it)})
            }
            print(">")
        }
    }
}
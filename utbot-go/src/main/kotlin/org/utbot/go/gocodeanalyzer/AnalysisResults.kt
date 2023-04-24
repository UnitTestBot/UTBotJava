package org.utbot.go.gocodeanalyzer

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import org.utbot.go.api.*
import org.utbot.go.api.util.goPrimitives
import org.utbot.go.framework.api.go.GoFieldId
import org.utbot.go.framework.api.go.GoImport
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoTypeId
import kotlin.reflect.KClass

data class AnalyzedPrimitiveType(
    override val name: String
) : AnalyzedType(name) {
    override fun toGoTypeId(): GoTypeId = GoPrimitiveTypeId(name = name)
}

data class AnalyzedStructType(
    override val name: String,
    val fields: List<AnalyzedField>
) : AnalyzedType(name) {
    data class AnalyzedField(
        val name: String,
        val type: AnalyzedType,
        val isExported: Boolean
    )

    override fun toGoTypeId(): GoTypeId = GoStructTypeId(
        name = name,
        fields = fields.map { field -> GoFieldId(field.type.toGoTypeId(), field.name, field.isExported) }
    )
}

data class AnalyzedArrayType(
    override val name: String,
    val elementType: AnalyzedType,
    val length: Int
) : AnalyzedType(name) {
    override fun toGoTypeId(): GoTypeId = GoArrayTypeId(
        name = name,
        elementTypeId = elementType.toGoTypeId(),
        length = length
    )
}

data class AnalyzedSliceType(
    override val name: String,
    val elementType: AnalyzedType,
) : AnalyzedType(name) {
    override fun toGoTypeId(): GoTypeId = GoSliceTypeId(
        name = name,
        elementTypeId = elementType.toGoTypeId(),
    )
}

data class AnalyzedMapType(
    override val name: String,
    val keyType: AnalyzedType,
    val elementType: AnalyzedType,
) : AnalyzedType(name) {
    override fun toGoTypeId(): GoTypeId = GoMapTypeId(
        name = name,
        keyTypeId = keyType.toGoTypeId(),
        elementTypeId = elementType.toGoTypeId(),
    )
}

data class AnalyzedInterfaceType(
    override val name: String,
) : AnalyzedType(name) {
    override fun toGoTypeId(): GoTypeId = GoInterfaceTypeId(name = name)
}

data class AnalyzedNamedType(
    override val name: String,
    val sourcePackage: GoPackage,
    val implementsError: Boolean,
    val underlyingType: AnalyzedType
) : AnalyzedType(name) {
    override fun toGoTypeId(): GoTypeId = GoNamedTypeId(
        name = name,
        sourcePackage = sourcePackage,
        implementsError = implementsError,
        underlyingTypeId = underlyingType.toGoTypeId(),
    )
}

@TypeFor(field = "name", adapter = AnalyzedTypeAdapter::class)
abstract class AnalyzedType(open val name: String) {
    abstract fun toGoTypeId(): GoTypeId
}

class AnalyzedTypeAdapter : TypeAdapter<AnalyzedType> {
    override fun classFor(type: Any): KClass<out AnalyzedType> {
        val typeName = type as String
        return when {
            typeName == "interface{}" -> AnalyzedInterfaceType::class
            typeName == "struct{}" -> AnalyzedStructType::class
            typeName.startsWith("map[") -> AnalyzedMapType::class
            typeName.startsWith("[]") -> AnalyzedSliceType::class
            typeName.startsWith("[") -> AnalyzedArrayType::class
            goPrimitives.map { it.name }.contains(typeName) -> AnalyzedPrimitiveType::class
            else -> AnalyzedNamedType::class
        }
    }
}

internal data class AnalyzedFunctionParameter(val name: String, val type: AnalyzedType)

internal data class AnalyzedFunction(
    val name: String,
    val modifiedName: String,
    val parameters: List<AnalyzedFunctionParameter>,
    val resultTypes: List<AnalyzedType>,
    val requiredImports: List<GoImport>,
    val constants: Map<String, List<String>>,
    val modifiedFunctionForCollectingTraces: String,
    val numberOfAllStatements: Int
)

internal data class AnalysisResult(
    val absoluteFilePath: String,
    val sourcePackage: GoPackage,
    val analyzedFunctions: List<AnalyzedFunction>,
    val notSupportedFunctionsNames: List<String>,
    val notFoundFunctionsNames: List<String>
)

internal data class AnalysisResults(val results: List<AnalysisResult>, val intSize: Int, val maxTraceLength: Int)

class GoParsingSourceCodeAnalysisResultException(s: String, t: Throwable) : Exception(s, t)
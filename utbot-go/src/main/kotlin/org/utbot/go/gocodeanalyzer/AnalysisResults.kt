package org.utbot.go.gocodeanalyzer

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import org.utbot.framework.plugin.api.FieldId
import org.utbot.go.api.GoArrayTypeId
import org.utbot.go.api.GoInterfaceTypeId
import org.utbot.go.api.GoStructTypeId
import org.utbot.go.api.GoTypeId
import org.utbot.go.api.util.goPrimitives
import kotlin.reflect.KClass

data class AnalyzedInterfaceType(
    override val name: String,
    val implementsError: Boolean
) : AnalyzedType(name) {
    override fun toGoTypeId(): GoTypeId = GoInterfaceTypeId(simpleName, implementsError)

    val simpleName: String = name.replaceFirst("interface ", "")
}

data class AnalyzedPrimitiveType(
    override val name: String
) : AnalyzedType(name) {
    override fun toGoTypeId(): GoTypeId = GoTypeId(name = this.name)
}

data class AnalyzedStructType(
    override val name: String,
    val implementsError: Boolean,
    val fields: List<AnalyzedField>
) : AnalyzedType(name) {
    data class AnalyzedField(
        val name: String,
        val type: AnalyzedType
    )

    override fun toGoTypeId(): GoTypeId = GoStructTypeId(
        this.name,
        this.implementsError,
        this.fields.map { FieldId(it.type.toGoTypeId(), it.name) }
    )
}

data class AnalyzedArrayType(
    override val name: String,
    val elementType: AnalyzedType,
    val length: Int
) : AnalyzedType(name) {
    override fun toGoTypeId(): GoTypeId = GoArrayTypeId(
        this.name,
        this.elementType.toGoTypeId(),
        this.length
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
            typeName.startsWith("interface ") -> AnalyzedInterfaceType::class
            typeName.startsWith("map[") -> error("Map type not yet supported")
            typeName.startsWith("[]") -> error("Slice type not yet supported")
            typeName.startsWith("[") -> AnalyzedArrayType::class
            goPrimitives.map { it.name }.contains(typeName) -> AnalyzedPrimitiveType::class
            else -> AnalyzedStructType::class
        }
    }
}

internal data class AnalyzedFunctionParameter(val name: String, val type: AnalyzedType)

internal data class AnalyzedFunction(
    val name: String,
    val modifiedName: String,
    val parameters: List<AnalyzedFunctionParameter>,
    val resultTypes: List<AnalyzedType>,
    val modifiedFunctionForCollectingTraces: String,
    val numberOfAllStatements: Int
)

internal data class AnalysisResult(
    val absoluteFilePath: String,
    val packageName: String,
    val analyzedFunctions: List<AnalyzedFunction>,
    val notSupportedFunctionsNames: List<String>,
    val notFoundFunctionsNames: List<String>
)

internal data class AnalysisResults(val results: List<AnalysisResult>)
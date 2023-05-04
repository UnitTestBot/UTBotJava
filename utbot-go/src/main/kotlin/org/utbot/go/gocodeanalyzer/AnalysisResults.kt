package org.utbot.go.gocodeanalyzer

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import org.utbot.go.api.*
import org.utbot.go.api.util.goPrimitives
import org.utbot.go.framework.api.go.GoFieldId
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoTypeId
import kotlin.reflect.KClass

data class AnalyzedPrimitiveType(
    override val name: String
) : AnalyzedType(name) {
    override fun toGoTypeId(
        index: String,
        analyzedTypes: MutableMap<String, GoTypeId>,
        typesToAnalyze: Map<String, AnalyzedType>
    ): GoTypeId {
        var result = analyzedTypes[index]
        if (result == null) {
            result = GoPrimitiveTypeId(name = name)
            analyzedTypes[index] = result
        }
        return result
    }
}

data class AnalyzedStructType(
    override val name: String,
    val fields: List<AnalyzedField>
) : AnalyzedType(name) {
    data class AnalyzedField(
        val name: String,
        val type: String,
        val isExported: Boolean
    )

    override fun toGoTypeId(
        index: String,
        analyzedTypes: MutableMap<String, GoTypeId>,
        typesToAnalyze: Map<String, AnalyzedType>
    ): GoTypeId {
        var result = analyzedTypes[index]
        if (result == null) {
            result = GoStructTypeId(
                name = name,
                fields = emptyList()
            )
            analyzedTypes[index] = result
            result.fields = fields.map { (name, type, isExported) ->
                val fieldType = typesToAnalyze[type]!!.toGoTypeId(type, analyzedTypes, typesToAnalyze)
                GoFieldId(fieldType, name, isExported)
            }
        }
        return result
    }
}

data class AnalyzedArrayType(
    override val name: String,
    val elementType: String,
    val length: Int
) : AnalyzedType(name) {
    override fun toGoTypeId(
        index: String,
        analyzedTypes: MutableMap<String, GoTypeId>,
        typesToAnalyze: Map<String, AnalyzedType>
    ): GoTypeId {
        var result = analyzedTypes[index]
        if (result == null) {
            result = GoArrayTypeId(
                name = name,
                elementTypeId = typesToAnalyze[elementType]!!.toGoTypeId(elementType, analyzedTypes, typesToAnalyze),
                length = length
            )
            analyzedTypes[index] = result
        }
        return result
    }
}

data class AnalyzedSliceType(
    override val name: String,
    val elementType: String,
) : AnalyzedType(name) {
    override fun toGoTypeId(
        index: String,
        analyzedTypes: MutableMap<String, GoTypeId>,
        typesToAnalyze: Map<String, AnalyzedType>
    ): GoTypeId {
        var result = analyzedTypes[index]
        if (result == null) {
            result = GoSliceTypeId(
                name = name,
                elementTypeId = typesToAnalyze[elementType]!!.toGoTypeId(elementType, analyzedTypes, typesToAnalyze),
            )
            analyzedTypes[index] = result
        }
        return result
    }
}

data class AnalyzedMapType(
    override val name: String,
    val keyType: String,
    val elementType: String,
) : AnalyzedType(name) {
    override fun toGoTypeId(
        index: String,
        analyzedTypes: MutableMap<String, GoTypeId>,
        typesToAnalyze: Map<String, AnalyzedType>
    ): GoTypeId {
        var result = analyzedTypes[index]
        if (result == null) {
            result = GoMapTypeId(
                name = name,
                keyTypeId = typesToAnalyze[keyType]!!.toGoTypeId(keyType, analyzedTypes, typesToAnalyze),
                elementTypeId = typesToAnalyze[elementType]!!.toGoTypeId(elementType, analyzedTypes, typesToAnalyze),
            )
            analyzedTypes[index] = result
        }
        return result
    }
}

data class AnalyzedChanType(
    override val name: String,
    val elementType: String,
    val direction: GoChanTypeId.Direction,
) : AnalyzedType(name) {
    override fun toGoTypeId(
        index: String,
        analyzedTypes: MutableMap<String, GoTypeId>,
        typesToAnalyze: Map<String, AnalyzedType>
    ): GoTypeId {
        var result = analyzedTypes[index]
        if (result == null) {
            result = GoChanTypeId(
                name = name,
                elementTypeId = typesToAnalyze[elementType]!!.toGoTypeId(elementType, analyzedTypes, typesToAnalyze),
                direction = direction
            )
            analyzedTypes[index] = result
        }
        return result
    }
}

data class AnalyzedInterfaceType(
    override val name: String,
) : AnalyzedType(name) {
    override fun toGoTypeId(
        index: String,
        analyzedTypes: MutableMap<String, GoTypeId>,
        typesToAnalyze: Map<String, AnalyzedType>
    ): GoTypeId {
        var result = analyzedTypes[index]
        if (result == null) {
            result = GoInterfaceTypeId(name = name)
            analyzedTypes[index] = result
        }
        return result
    }
}

data class AnalyzedNamedType(
    override val name: String,
    val sourcePackage: GoPackage,
    val implementsError: Boolean,
    val underlyingType: String
) : AnalyzedType(name) {
    override fun toGoTypeId(
        index: String,
        analyzedTypes: MutableMap<String, GoTypeId>,
        typesToAnalyze: Map<String, AnalyzedType>
    ): GoTypeId {
        var result = analyzedTypes[index]
        if (result == null) {
            result = GoNamedTypeId(
                name = name,
                sourcePackage = sourcePackage,
                implementsError = implementsError,
                underlyingTypeId = typesToAnalyze[underlyingType]!!.toGoTypeId(
                    underlyingType,
                    analyzedTypes,
                    typesToAnalyze
                ),
            )
            analyzedTypes[index] = result
        }
        return result
    }
}

data class AnalyzedPointerType(
    override val name: String,
    val elementType: String
) : AnalyzedType(name) {
    override fun toGoTypeId(
        index: String,
        analyzedTypes: MutableMap<String, GoTypeId>,
        typesToAnalyze: Map<String, AnalyzedType>
    ): GoTypeId {
        var result = analyzedTypes[index]
        if (result == null) {
            result = GoPointerTypeId(
                name = name,
                elementTypeId = typesToAnalyze[elementType]!!.toGoTypeId(elementType, analyzedTypes, typesToAnalyze),
            )
            analyzedTypes[index] = result
        }
        return result
    }
}

@TypeFor(field = "name", adapter = AnalyzedTypeAdapter::class)
abstract class AnalyzedType(open val name: String) {
    abstract fun toGoTypeId(
        index: String,
        analyzedTypes: MutableMap<String, GoTypeId>,
        typesToAnalyze: Map<String, AnalyzedType>
    ): GoTypeId
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
            typeName.startsWith("<-chan") || typeName.startsWith("chan") -> AnalyzedChanType::class
            typeName.startsWith("*") -> AnalyzedPointerType::class
            goPrimitives.map { it.name }.contains(typeName) -> AnalyzedPrimitiveType::class
            else -> AnalyzedNamedType::class
        }
    }
}

internal data class AnalyzedFunctionParameter(val name: String, val type: String)

internal data class AnalyzedFunction(
    val name: String,
    val types: Map<String, AnalyzedType>,
    val parameters: List<AnalyzedFunctionParameter>,
    val resultTypes: List<String>,
    val constants: Map<String, List<String>>,
)

internal data class AnalysisResult(
    val absoluteFilePath: String,
    val sourcePackage: GoPackage,
    val analyzedFunctions: List<AnalyzedFunction>,
    val notSupportedFunctionsNames: List<String>,
    val notFoundFunctionsNames: List<String>
)

internal data class AnalysisResults(val results: List<AnalysisResult>, val intSize: Int)

class GoParsingSourceCodeAnalysisResultException(s: String, t: Throwable) : Exception(s, t)
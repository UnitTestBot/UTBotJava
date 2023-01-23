package org.utbot.python.newtyping.mypy

import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import org.utbot.python.newtyping.general.Type

class GlobalNamesStorage(private val mypyStorage: MypyAnnotationStorage) {
    fun resolveTypeName(module: String, name: String): Type? {
        val splitName = name.split(".")
        if (splitName.size == 1) {
            val nameFromStorage = mypyStorage.names[module]?.find { it.name == name } ?: return null
            return when (nameFromStorage) {
                is LocalTypeName -> mypyStorage.definitions[module]!![name]!!.getUtBotType()
                is ImportedTypeName -> {
                    val split = nameFromStorage.fullname.split(".")
                    resolveTypeName(split.dropLast(1).joinToString("."), split.last())
                }
                else -> null
            }
        }
        val withoutLast = splitName.dropLast(1)
        withoutLast.foldRight(Pair(withoutLast.joinToString("."), splitName.last())) { nextPart, (left, right) ->
            val next = Pair(left.dropLast(nextPart.length).removeSuffix("."), "$nextPart.$right")
            val nameFromStorage = (mypyStorage.names[module]?.find { it.name == left } as? ModuleName)
                ?: return@foldRight next
            val attempt = resolveTypeName(nameFromStorage.fullname, right)
            if (attempt != null)
                return attempt
            next
        }
        withoutLast.foldRight(Pair(withoutLast.joinToString("."), splitName.last())) { nextPart, (left, right) ->
            val next = Pair(left.dropLast(nextPart.length).removeSuffix("."), "$nextPart.$right")
            mypyStorage.names["$module.$left"] ?: return@foldRight next
            val attempt = resolveTypeName("$module.$left", right)
            if (attempt != null)
                return attempt
            next
        }
        return null
    }
}

sealed class Name(val name: String)
class ModuleName(name: String, val fullname: String): Name(name)
class LocalTypeName(name: String): Name(name)
class ImportedTypeName(name: String, val fullname: String): Name(name)
class OtherName(name: String): Name(name)

enum class NameType {
    Module,
    LocalType,
    ImportedType,
    Other
}

val namesAdapter: PolymorphicJsonAdapterFactory<Name> =
    PolymorphicJsonAdapterFactory.of(Name::class.java, "kind")
        .withSubtype(ModuleName::class.java, NameType.Module.name)
        .withSubtype(LocalTypeName::class.java, NameType.LocalType.name)
        .withSubtype(ImportedTypeName::class.java, NameType.ImportedType.name)
        .withSubtype(OtherName::class.java, NameType.Other.name)
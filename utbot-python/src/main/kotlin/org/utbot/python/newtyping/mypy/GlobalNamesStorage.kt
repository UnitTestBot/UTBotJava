package org.utbot.python.newtyping.mypy

class GlobalNamesStorage {
}

sealed class Name(val name: String)
class ModuleName(name: String, val fullname: String): Name(name)
class LocalTypeName(name: String): Name(name)
class ImportedTypeName(name: String, val fullname: String): Name(name)
class OtherName(name: String): Name(name)
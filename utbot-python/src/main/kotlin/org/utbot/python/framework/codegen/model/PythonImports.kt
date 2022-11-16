package org.utbot.framework.codegen

import org.utbot.framework.codegen.domain.Import

sealed class PythonImport(order: Int) : Import(order) {
    var importName: String = ""
    var moduleName: String? = null

    constructor(order: Int, importName: String, moduleName: String? = null) : this(order) {
        this.importName = importName
        this.moduleName = moduleName
    }

    override val qualifiedName: String
        get() = if (moduleName != null) "${moduleName}.${importName}" else importName

    val rootModuleName: String
        get() = qualifiedName.split(".")[0]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PythonImport
        return qualifiedName == other.qualifiedName
    }

    override fun hashCode(): Int {
        var result = importName.hashCode()
        result = 31 * result + (moduleName?.hashCode() ?: 0)
        return result
    }
}

data class PythonSysPathImport(val sysPath: String) : PythonImport(2) {
    override val qualifiedName: String
        get() = sysPath

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PythonSysPathImport
        return qualifiedName == other.qualifiedName
    }

    override fun hashCode(): Int {
        return sysPath.hashCode()
    }
}

data class PythonUserImport(val importName_: String, val moduleName_: String? = null) :
    PythonImport(3, importName_, moduleName_) {
    override val qualifiedName: String
        get() = if (moduleName != null) "${moduleName}.${importName}" else importName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PythonUserImport
        return qualifiedName == other.qualifiedName
    }

    override fun hashCode(): Int {
        var result = importName.hashCode()
        result = 31 * result + (moduleName?.hashCode() ?: 0)
        return result
    }
}

data class PythonSystemImport(val importName_: String, val moduleName_: String? = null) :
    PythonImport(1, importName_, moduleName_) {
    override val qualifiedName: String
        get() = if (moduleName != null) "${moduleName}.${importName}" else importName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PythonSystemImport
        return qualifiedName == other.qualifiedName
    }

    override fun hashCode(): Int {
        var result = importName.hashCode()
        result = 31 * result + (moduleName?.hashCode() ?: 0)
        return result
    }
}

package org.utbot.framework.codegen

data class PythonSysPathImport(val sysPath: String): Import(1) {
    override val qualifiedName: String
        get() = sysPath

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PythonImport
        return qualifiedName == other.qualifiedName
    }

    override fun hashCode(): Int {
        return sysPath.hashCode()
    }
}

data class PythonImport(val importName: String, val moduleName: String? = null): Import(2) {
    override val qualifiedName: String
        get() = if (moduleName != null) "${moduleName}.${importName}" else importName

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
package org.utbot.framework.plugin.api

import org.utbot.jcdb.api.ClassId
import org.utbot.jcdb.api.FieldId
import org.utbot.jcdb.api.MethodId
import java.lang.reflect.Executable
import java.lang.reflect.Field

interface ReflectionProvider {
    fun provideReflectionClass(classId: ClassId): Class<*>

    fun provideReflectionField(fieldId: FieldId): Field

    fun provideReflectionExecutable(methodId: MethodId): Executable
}

object DefaultReflectionProvider : ReflectionProvider {
    override fun provideReflectionClass(classId: ClassId): Class<*> {
        TODO("Not yet implemented")
    }

    override fun provideReflectionField(fieldId: FieldId): Field {
        TODO("Not yet implemented")
    }

    override fun provideReflectionExecutable(methodId: MethodId): Executable {
        TODO("Not yet implemented")
    }

}
package org.utbot.greyboxfuzzer.quickcheck.internal

import java.lang.reflect.AnnotatedArrayType
import java.lang.reflect.AnnotatedType
import java.lang.reflect.Type

object FakeAnnotatedTypeFactory {

    private class FakeAnnotatedArrayType(private val type: Class<*>) : AnnotatedArrayType {
        override fun getAnnotatedGenericComponentType(): AnnotatedType {
            return makeFrom(type.componentType)
        }

        // Not introduced until JDK 9 -- not marking as...
        // @Override
        override fun getAnnotatedOwnerType(): AnnotatedType? {
            return null
        }

        override fun getType(): Type {
            return type
        }

        override fun <T : Annotation?> getAnnotation(
            annotationClass: Class<T>
        ): T? {
            return null
        }

        override fun getAnnotations(): Array<Annotation?> {
            return arrayOfNulls(0)
        }

        override fun getDeclaredAnnotations(): Array<Annotation?> {
            return arrayOfNulls(0)
        }
    }

    private class FakeAnnotatedType(private val type: Class<*>) : AnnotatedType {
        override fun getType(): Type {
            return type
        }

        override fun <T : Annotation?> getAnnotation(
            annotationClass: Class<T>
        ): T? {
            return null
        }

        override fun getAnnotations(): Array<Annotation?> {
            return arrayOfNulls(0)
        }

        override fun getDeclaredAnnotations(): Array<Annotation?> {
            return arrayOfNulls(0)
        }
    }


    @JvmStatic
    fun makeFrom(clazz: Class<*>): AnnotatedType {
        return if (clazz.isArray) makeArrayType(clazz) else makePlainType(clazz)
    }

    private fun makeArrayType(type: Class<*>): AnnotatedArrayType {
        return FakeAnnotatedArrayType(type)
    }

    private fun makePlainType(type: Class<*>): AnnotatedType {
        return FakeAnnotatedType(type)
    }

}
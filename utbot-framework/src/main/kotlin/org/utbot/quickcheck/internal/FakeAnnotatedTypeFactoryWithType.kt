/*
 The MIT License

 Copyright (c) 2010-2021 Paul R. Holser, Jr.

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package org.utbot.quickcheck.internal

import java.lang.reflect.AnnotatedArrayType
import java.lang.reflect.AnnotatedType
import java.lang.reflect.Type

object FakeAnnotatedTypeFactoryWithType {


    private class FakeAnnotatedArrayType internal constructor(private val type: Type) : AnnotatedArrayType {
        override fun getAnnotatedGenericComponentType(): AnnotatedType {
            return makeFrom(type.javaClass.componentType)
        }

        // Not introduced until JDK 9 -- not marking as...
        // @Override
        override fun getAnnotatedOwnerType(): AnnotatedType? {
            return null
        }

        override fun getType(): Type {
            return type
        }

        override fun <T : Annotation?> getAnnotation(annotationClass: Class<T>): T? {
            return null
        }

        override fun getAnnotations(): Array<Annotation?> {
            return arrayOfNulls(0)
        }

        override fun getDeclaredAnnotations(): Array<Annotation?> {
            return arrayOfNulls(0)
        }
    }

    private class FakeAnnotatedType internal constructor(private val type: Type) : AnnotatedType {
        override fun getType(): Type {
            return type
        }

        override fun <T : Annotation?> getAnnotation(annotationClass: Class<T>): T? {
            return null
        }

        override fun getAnnotations(): Array<Annotation?> {
            return arrayOfNulls(0)
        }

        override fun getDeclaredAnnotations(): Array<Annotation?> {
            return arrayOfNulls(0)
        }
    }

    fun makeFrom(type: Type): AnnotatedType {
        return if (type.javaClass.isArray) makeArrayType(type) else makePlainType(type)
    }

    private fun makeArrayType(type: Type): AnnotatedArrayType {
        return FakeAnnotatedArrayType(type)
    }

    private fun makePlainType(type: Type): AnnotatedType {
        return FakeAnnotatedType(type)
    }

}
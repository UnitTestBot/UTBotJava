package org.utbot.quickcheck.internal

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Constructor
import java.lang.reflect.Method

class DefaultMethodHandleMaker {
    fun handleForSpecialMethod(method: Method): MethodHandle {
        return if (Reflection.jdk9OrBetter()) jdk9OrBetterMethodHandle(method) else jdk8MethodHandleForDefault(method)
    }

    private fun jdk9OrBetterMethodHandle(method: Method): MethodHandle {
        return MethodHandles.lookup()
            .findSpecial(
                method.declaringClass,
                method.name,
                MethodType.methodType(
                    method.returnType,
                    method.parameterTypes
                ),
                method.declaringClass
            )
    }

    private fun jdk8MethodHandleForDefault(method: Method): MethodHandle {
        val lookup = methodLookupCtorJDK8()!!
            .newInstance(
                method.declaringClass,
                MethodHandles.Lookup.PRIVATE
            )
        return lookup.unreflectSpecial(method, method.declaringClass)
    }

    companion object {
        @Volatile
        private var methodLookupCtorJDK8: Constructor<MethodHandles.Lookup>? = null
        private fun methodLookupCtorJDK8(): Constructor<MethodHandles.Lookup>? {
            if (methodLookupCtorJDK8 == null) {
                methodLookupCtorJDK8 = Reflection.findDeclaredConstructor(
                    MethodHandles.Lookup::class.java, Class::class.java, Int::class.javaPrimitiveType
                )
            }
            return methodLookupCtorJDK8
        }
    }
}
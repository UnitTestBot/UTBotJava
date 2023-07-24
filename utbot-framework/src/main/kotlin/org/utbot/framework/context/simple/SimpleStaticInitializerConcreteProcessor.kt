package org.utbot.framework.context.simple

import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.engine.classToWrapper
import org.utbot.engine.types.TypeResolver
import org.utbot.engine.util.statics.concrete.isEnumAffectingExternalStatics
import org.utbot.framework.context.StaticInitializerConcreteProcessor
import soot.jimple.StaticFieldRef

object SimpleStaticInitializerConcreteProcessor : StaticInitializerConcreteProcessor {
    override fun shouldProcessStaticFieldConcretely(fieldRef: StaticFieldRef, typeResolver: TypeResolver): Boolean =
        workaround(WorkaroundReason.HACK) {
            val className = fieldRef.field.declaringClass.name

            // We should process clinit sections for classes from these packages.
            // Note that this list is not exhaustive, so it may be supplemented in the future.
            val packagesToProcessConcretely = javaPackagesToProcessConcretely + sunPackagesToProcessConcretely

            val declaringClass = fieldRef.field.declaringClass

            val isFromPackageToProcessConcretely = packagesToProcessConcretely.any { className.startsWith(it) }
                    // it is required to remove classes we override, since
                    // we could accidentally initialize their final fields
                    // with values that will later affect our overridden classes
                    && fieldRef.field.declaringClass.type !in classToWrapper.keys
                    // because of the same reason we should not use
                    // concrete information from clinit sections for enums
                    && !fieldRef.field.declaringClass.isEnum
                    //hardcoded string for class name is used cause class is not public
                    //this is a hack to avoid crashing on code with Math.random()
                    && !className.endsWith("RandomNumberGeneratorHolder")

            // we can process concretely only enums that does not affect the external system
            val isEnumNotAffectingExternalStatics = declaringClass.let {
                it.isEnum && !it.isEnumAffectingExternalStatics(typeResolver)
            }

            return isEnumNotAffectingExternalStatics || isFromPackageToProcessConcretely
        }

    private val javaPackagesToProcessConcretely: List<String> = listOf(
        "applet", "awt", "beans", "io", "lang", "math", "net",
        "nio", "rmi", "security", "sql", "text", "time", "util"
    ).map { "java.$it" }

    private val sunPackagesToProcessConcretely: List<String> = listOf(
        "applet", "audio", "awt", "corba", "font", "instrument",
        "invoke", "io", "java2d", "launcher", "management", "misc",
        "net", "nio", "print", "reflect", "rmi", "security",
        "swing", "text", "tools.jar", "tracing", "util"
    ).map { "sun.$it" }
}

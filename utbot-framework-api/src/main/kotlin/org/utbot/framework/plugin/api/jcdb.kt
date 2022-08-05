package org.utbot.framework.plugin.api

import kotlinx.coroutines.runBlocking
import org.utbot.jcdb.api.*

val Accessible.access: Int
    get() = runBlocking { access() }

val Accessible.isPublic: Boolean
    get() = runBlocking { isPublic() }

val Accessible.isProtected: Boolean
    get() = runBlocking { isProtected() }

val Accessible.isPrivate: Boolean
    get() = runBlocking { isPrivate() }

val Accessible.isFinal: Boolean
    get() = runBlocking { isFinal() }

val Accessible.isStatic: Boolean
    get() = runBlocking { isStatic() }

val ClassId.isAbstract: Boolean
    get() = runBlocking { isAbstract() }

val ClassId.isAnonymous: Boolean
    get() = runBlocking { isAnonymous() }

val ClassId.isLocalClass: Boolean
    get() = runBlocking { isLocal() }

val ClassId.isInner: Boolean
    get() = isNested && !isStatic

val ClassId.isNested: Boolean
    get() = runBlocking { outerClass() != null }

val ClassId.isSynthetic: Boolean
    get() = runBlocking { isSynthetic() }

val ClassId.isMemberClass: Boolean
    get() = runBlocking { isMemberClass() }

val ClassId.superclass: ClassId?
    get() = runBlocking {
        superclass()
    }

val ClassId.isEnum: Boolean
    get() = runBlocking {
        isEnum()
    }

val ClassId.fields: List<FieldId>
    get() = runBlocking { fields() }

val ClassId.methods: List<MethodId>
    get() {
        return runBlocking {
            methods()
        }
    }

val ClassId.allConstructors: List<ConstructorExecutableId>
    get() = runBlocking { allConstructors().map { ConstructorExecutableId(it) } }

val ClassId.interfaces: List<ClassId>
    get() {
        return runBlocking {
            interfaces()
        }
    }

val ClassId.packageName: String
    get() = runBlocking {
        val clazz = outerClass()
        if (clazz != null) {
            name.substringBeforeLast(".").substringBeforeLast(".")
        } else {
            name.substringBeforeLast(".")
        }
    }


val FieldId.isPublic: Boolean
    get() = runBlocking { isPublic() }

val FieldId.isProtected: Boolean
    get() = runBlocking { isProtected() }

val FieldId.isPrivate: Boolean
    get() = runBlocking { isPrivate() }

val Accessible.isPackagePrivate: Boolean
    get() = runBlocking { !isPublic() && !isProtected() && !isPrivate() }

val FieldId.isFinal: Boolean
    get() = runBlocking { isFinal() }

val FieldId.isStatic: Boolean
    get() = runBlocking { isStatic() }

val FieldId.isSynthetic: Boolean
    get() = runBlocking { isSynthetic() }

val FieldId.type: ClassId
    get() = runBlocking { type() }

val MethodId.returnType: ClassId
    get() = runBlocking { returnType() }

val MethodId.parameters: List<ClassId>
    get() = runBlocking { parameters() }

val MethodId.signature: String
    get() = runBlocking { signature(false) }

val MethodId.internalSignature: String
    get() = runBlocking { signature(true) }


infix fun ClassId.blockingIsSubtypeOf(another: ClassId): Boolean = runBlocking {
    this@blockingIsSubtypeOf isSubtypeOf another
}

infix fun ClassId.blockingIsNotSubtypeOf(another: ClassId): Boolean = !this.blockingIsSubtypeOf(another)

/**
 * we will count item accessible if it is whether public
 * or package-private inside target package [packageName].
 *
 * @param packageName name of the package we check accessibility from
 */
suspend fun Accessible.isAccessibleFrom(packageName: String): Boolean {
    val classId = when (this) {
        is ClassId -> this
        is MethodId -> classId
        is FieldId -> classId
        else -> throw IllegalStateException("unknown type $this")
    }

    val isAccessibleFromPackageByModifiers =
        isPublic() || (classId.packageName == packageName && (isPackagePrivate() || isProtected()))

    return classId.isClassAccessibleFrom(packageName) && isAccessibleFromPackageByModifiers
}

suspend fun ClassId.isClassAccessibleFrom(packageName: String): Boolean {
    return isPublic() || (this.packageName == packageName && (isPackagePrivate() || isProtected()))
}

val ClassId.isInDefaultPackage: Boolean
    get() {
        return this is PredefinedPrimitive || packageName.isEmpty()
    }

package org.utbot.fuzzer.types

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.*

object JavaVoid : PrimitiveType(Atom("double")), WithClassId {
    override val classId: ClassId = voidClassId
}

object JavaBool : PrimitiveType(Atom("bool")), WithClassId {
    override val classId: ClassId = booleanClassId
}

object JavaChar : PrimitiveType(Atom("char")), WithClassId {
    override val classId: ClassId = charClassId
}

object JavaByte : PrimitiveType(Atom("byte")), WithClassId {
    override val classId: ClassId = byteClassId
}

object JavaShort : PrimitiveType(Atom("short")), WithClassId {
    override val classId: ClassId = shortClassId
}

object JavaInt : PrimitiveType(Atom("int")), WithClassId {
    override val classId: ClassId = intClassId
}

object JavaLong : PrimitiveType(Atom("long")), WithClassId {
    override val classId: ClassId = longClassId
}

object JavaFloat : PrimitiveType(Atom("float")), WithClassId {
    override val classId: ClassId = floatClassId
}

object JavaDouble : PrimitiveType(Atom("double")), WithClassId {
    override val classId: ClassId = doubleClassId
}

object JavaObject : JavaClass(
    FqName(listOf("java", "lang"), "Object"),
    libraryType = true,
    supertypes = emptySet(),
    fields = emptyList(),
    methods = emptyList(),
    classId = objectClassId
)

object JavaString : JavaClass(
    FqName(listOf("java", "lang"), "String"),
    libraryType = true,
    supertypes = emptySet(),
    fields = emptyList(),
    methods = emptyList(),
    classId = stringClassId
)

open class JavaArray(
    val elementType: Type,
    val arity: Int = 0,
    override val classId: ClassId
) : Type(emptyList()), WithClassId {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JavaArray

        if (classId != other.classId) return false

        return true
    }

    override fun hashCode(): Int {
        return classId.hashCode()
    }
}

open class JavaClass(
    fqName: FqName,
    libraryType: Boolean,
    supertypes: Set<Type>,
    fields: List<Field>,
    methods: List<Function>,
    override val classId: ClassId
) : CompositeType(fqName, libraryType, supertypes, fields, methods), WithClassId {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JavaClass

        if (classId != other.classId) return false

        return true
    }

    override fun hashCode(): Int {
        return classId.hashCode()
    }
}

fun Type.isJavaPrimitive() = when (this) {
    JavaVoid, JavaBool, JavaByte, JavaChar, JavaShort, JavaInt, JavaLong, JavaFloat, JavaDouble -> true
    else -> false
}

fun ClassId.toJavaType(): Type {
    return if (isPrimitive) {
        when (this) {
            voidClassId -> JavaVoid
            booleanClassId -> JavaBool
            byteClassId -> JavaByte
            charClassId -> JavaChar
            shortClassId -> JavaShort
            intClassId -> JavaInt
            longClassId -> JavaLong
            floatClassId -> JavaFloat
            doubleClassId -> JavaDouble
            else -> error("$this is not java primitive type")
        }
    } else if (this.isArray) {
        return JavaArray(
            elementType = elementClassId!!.toJavaType(),
            arity = name.count { it == '[' },
            classId = this
        )
    } else if (this == objectClassId) {
        return JavaObject
    } else if (this == stringClassId) {
        return JavaString
    } else {
        val nameParts = name.split('.')
        JavaClass(
            FqName(nameParts.subList(0, nameParts.size - 1), nameParts.last()),
            libraryType = false,
            emptySet(), // todo add superclasses
            emptyList(),
            emptyList(),
//            allDeclaredFieldIds.map { Field(it.name, it.type.toJavaType()) }.toList(),
//            allMethods.map {
//                Function(it.name, FunctionType(
//                    parameters = emptyList(),
//                    arguments = it.parameters.map(ClassId::toJavaType),
//                    returnValue = it.returnType.toJavaType())
//                )
//            }.toList(),
            classId = this
        )
    }
}
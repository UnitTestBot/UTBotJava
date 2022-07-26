package org.utbot.framework.codegen.model.visitor

import org.utbot.framework.codegen.StaticImport
import org.utbot.framework.codegen.model.constructor.builtin.arraysDeepEqualsMethodId
import org.utbot.framework.codegen.model.constructor.builtin.createArrayMethodId
import org.utbot.framework.codegen.model.constructor.builtin.createInstanceMethodId
import org.utbot.framework.codegen.model.constructor.builtin.deepEqualsMethodId
import org.utbot.framework.codegen.model.constructor.builtin.getArrayLengthMethodId
import org.utbot.framework.codegen.model.constructor.builtin.getEnumConstantByNameMethodId
import org.utbot.framework.codegen.model.constructor.builtin.getFieldValueMethodId
import org.utbot.framework.codegen.model.constructor.builtin.getStaticFieldValueMethodId
import org.utbot.framework.codegen.model.constructor.builtin.getUnsafeInstanceMethodId
import org.utbot.framework.codegen.model.constructor.builtin.hasCustomEqualsMethodId
import org.utbot.framework.codegen.model.constructor.builtin.iterablesDeepEqualsMethodId
import org.utbot.framework.codegen.model.constructor.builtin.mapsDeepEqualsMethodId
import org.utbot.framework.codegen.model.constructor.builtin.setFieldMethodId
import org.utbot.framework.codegen.model.constructor.builtin.setStaticFieldMethodId
import org.utbot.framework.codegen.model.constructor.builtin.streamsDeepEqualsMethodId
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.util.importIfNeeded
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.util.id
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Arrays
import java.util.Objects

internal fun ClassId.utilMethodById(id: MethodId, context: CgContext): String =
    with(context) {
        when (id) {
            getUnsafeInstanceMethodId -> getUnsafeInstance(codegenLanguage)
            createInstanceMethodId -> createInstance(codegenLanguage)
            createArrayMethodId -> createArray(codegenLanguage)
            setFieldMethodId -> setField(codegenLanguage)
            setStaticFieldMethodId -> setStaticField(codegenLanguage)
            getFieldValueMethodId -> getFieldValue(codegenLanguage)
            getStaticFieldValueMethodId -> getStaticFieldValue(codegenLanguage)
            getEnumConstantByNameMethodId -> getEnumConstantByName(codegenLanguage)
            deepEqualsMethodId -> deepEquals(codegenLanguage, mockFrameworkUsed, mockFramework)
            arraysDeepEqualsMethodId -> arraysDeepEquals(codegenLanguage)
            iterablesDeepEqualsMethodId -> iterablesDeepEquals(codegenLanguage)
            streamsDeepEqualsMethodId -> streamsDeepEquals(codegenLanguage)
            mapsDeepEqualsMethodId -> mapsDeepEquals(codegenLanguage)
            hasCustomEqualsMethodId -> hasCustomEquals(codegenLanguage)
            getArrayLengthMethodId -> getArrayLength(codegenLanguage)
            else -> error("Unknown util method for class $this: $id")
        }
    }

fun getEnumConstantByName(language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            private static Object getEnumConstantByName(Class<?> enumClass, String name) throws IllegalAccessException {
                java.lang.reflect.Field[] fields = enumClass.getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    String fieldName = field.getName();
                    if (field.isEnumConstant() && fieldName.equals(name)) {
                        field.setAccessible(true);
                        
                        return field.get(null);
                    }
                }
                
                return null;
            }
        """
        }
        CodegenLanguage.KOTLIN -> {
            """
            private fun getEnumConstantByName(enumClass: Class<*>, name: String): kotlin.Any? {
                val fields: kotlin.Array<java.lang.reflect.Field> = enumClass.declaredFields
                for (field in fields) {
                    val fieldName = field.name
                    if (field.isEnumConstant && fieldName == name) {
                        field.isAccessible = true
                        
                        return field.get(null)
                    }
                }
                
                return null
            }
        """
        }
    }.trimIndent()

fun getStaticFieldValue(language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            private static Object getStaticFieldValue(Class<?> clazz, String fieldName) throws IllegalAccessException, NoSuchFieldException {
                java.lang.reflect.Field field;
                Class<?> originClass = clazz;
                do {
                    try {
                        field = clazz.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
                        
                        return field.get(null);
                    } catch (NoSuchFieldException e) {
                        clazz = clazz.getSuperclass();
                    }
                } while (clazz != null);
        
                throw new NoSuchFieldException("Field '" + fieldName + "' not found on class " + originClass);
            }
        """
        }
        CodegenLanguage.KOTLIN -> {
            """
            private fun getStaticFieldValue(clazz: Class<*>, fieldName: String): kotlin.Any? {
                var currentClass: Class<*>? = clazz
                var field: java.lang.reflect.Field
                do {
                    try {
                        field = currentClass!!.getDeclaredField(fieldName)
                        field.isAccessible = true
                        val modifiersField: java.lang.reflect.Field = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
                        modifiersField.isAccessible = true
                        modifiersField.setInt(field, field.modifiers and java.lang.reflect.Modifier.FINAL.inv())
                        
                        return field.get(null)
                    } catch (e: NoSuchFieldException) {
                        currentClass = currentClass!!.superclass
                    }
                } while (currentClass != null)
                
                throw NoSuchFieldException("Field '" + fieldName + "' not found on class " + clazz)
            }
        """
        }
    }.trimIndent()

fun getFieldValue(language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            private static Object getFieldValue(Object obj, String fieldName) throws IllegalAccessException, NoSuchFieldException {
                Class<?> clazz = obj.getClass();
                java.lang.reflect.Field field;
                do {
                    try {
                        field = clazz.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
                        
                        return field.get(obj);
                    } catch (NoSuchFieldException e) {
                        clazz = clazz.getSuperclass();
                    }
                } while (clazz != null);
        
                throw new NoSuchFieldException("Field '" + fieldName + "' not found on class " + obj.getClass());
            }
        """
        }
        CodegenLanguage.KOTLIN -> {
            """
            private fun getFieldValue(any: kotlin.Any, fieldName: String): kotlin.Any? {
                var clazz: Class<*>? = any.javaClass
                var field: java.lang.reflect.Field
                do {
                    try {
                        field = clazz!!.getDeclaredField(fieldName)
                        field.isAccessible = true
                        val modifiersField: java.lang.reflect.Field = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
                        modifiersField.isAccessible = true
                        modifiersField.setInt(field, field.modifiers and java.lang.reflect.Modifier.FINAL.inv())
                        
                        return field.get(any)
                    } catch (e: NoSuchFieldException) {
                        clazz = clazz!!.superclass
                    }
                } while (clazz != null)
                
                throw NoSuchFieldException("Field '" + fieldName + "' not found on class " + any.javaClass)
            }
        """
        }
    }.trimIndent()

fun setStaticField(language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            private static void setStaticField(Class<?> clazz, String fieldName, Object fieldValue) throws NoSuchFieldException, IllegalAccessException {
                java.lang.reflect.Field field;
    
                do {
                    try {
                        field = clazz.getDeclaredField(fieldName);
                    } catch (Exception e) {
                        clazz = clazz.getSuperclass();
                        field = null;
                    }
                } while (field == null);
                
                java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
    
                field.setAccessible(true);
                field.set(null, fieldValue);
            }
        """
        }
        CodegenLanguage.KOTLIN -> {
            """
            private fun setStaticField(defaultClass: Class<*>, fieldName: String, fieldValue: kotlin.Any?) {
                var field: java.lang.reflect.Field?
                var clazz = defaultClass
        
                do {
                    try {
                        field = clazz.getDeclaredField(fieldName)
                    } catch (e: Exception) {
                        clazz = clazz.superclass
                        field = null
                    }
                } while (field == null)
        
                val modifiersField: java.lang.reflect.Field = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
                modifiersField.isAccessible = true
                modifiersField.setInt(field, field.modifiers and java.lang.reflect.Modifier.FINAL.inv())
        
                field.isAccessible = true
                field.set(null, fieldValue)
            }
        """
        }
    }.trimIndent()

fun setField(language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            private static void setField(Object object, String fieldName, Object fieldValue) throws NoSuchFieldException, IllegalAccessException {
                Class<?> clazz = object.getClass();
                java.lang.reflect.Field field;
    
                do {
                    try {
                        field = clazz.getDeclaredField(fieldName);
                    } catch (Exception e) {
                        clazz = clazz.getSuperclass();
                        field = null;
                    }
                } while (field == null);
                
                java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
    
                field.setAccessible(true);
                field.set(object, fieldValue);
            }
        """
        }
        CodegenLanguage.KOTLIN -> {
            """
            private fun setField(any: kotlin.Any, fieldName: String, fieldValue: kotlin.Any?) {
                var clazz: Class<*> = any.javaClass
                var field: java.lang.reflect.Field?
                do {
                    try {
                        field = clazz.getDeclaredField(fieldName)
                    } catch (e: Exception) {
                        clazz = clazz.superclass
                        field = null
                    }
                } while (field == null)
        
                val modifiersField: java.lang.reflect.Field = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
                modifiersField.isAccessible = true
                modifiersField.setInt(field, field.modifiers and java.lang.reflect.Modifier.FINAL.inv())
        
                field.isAccessible = true
                field.set(any, fieldValue)
            }
        """
        }
    }.trimIndent()

fun createArray(language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            private static Object[] createArray(String className, int length, Object... values) throws ClassNotFoundException {
                Object array = java.lang.reflect.Array.newInstance(Class.forName(className), length);
    
                for (int i = 0; i < values.length; i++) {
                    java.lang.reflect.Array.set(array, i, values[i]);
                }
                
                return (Object[]) array;
            }
        """
        }
        CodegenLanguage.KOTLIN -> {
            """
            private fun createArray(
                className: String, 
                length: Int, 
                vararg values: kotlin.Any
            ): kotlin.Array<kotlin.Any?> {
                val array: kotlin.Any = java.lang.reflect.Array.newInstance(Class.forName(className), length)
                
                for (i in values.indices) {
                    java.lang.reflect.Array.set(array, i, values[i])
                }
                
                return array as kotlin.Array<kotlin.Any?>
            }
        """
        }
    }.trimIndent()

fun createInstance(language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            private static Object createInstance(String className) throws Exception {
                Class<?> clazz = Class.forName(className);
                return Class.forName("sun.misc.Unsafe").getDeclaredMethod("allocateInstance", Class.class)
                    .invoke(getUnsafeInstance(), clazz);
            }
            """
        }
        CodegenLanguage.KOTLIN -> {
            """
            private fun createInstance(className: String): kotlin.Any? {
                val clazz: Class<*> = Class.forName(className)
                return Class.forName("sun.misc.Unsafe").getDeclaredMethod("allocateInstance", Class::class.java)
                    .invoke(getUnsafeInstance(), clazz)
            }
            """
        }
    }.trimIndent()

fun getUnsafeInstance(language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            private static Object getUnsafeInstance() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
                java.lang.reflect.Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
                f.setAccessible(true);
                return f.get(null);
            }
            """
        }
        CodegenLanguage.KOTLIN -> {
            """
            private fun getUnsafeInstance(): kotlin.Any? {
                val f: java.lang.reflect.Field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe")
                f.isAccessible = true
                return f[null]
            }
            """
        }
    }.trimIndent()

/**
 * Mockito mock uses its own equals which we cannot rely on
 */
private fun isMockCondition(mockFrameworkUsed: Boolean, mockFramework: MockFramework): String {
    if (!mockFrameworkUsed) return ""

    // TODO for now we have only Mockito but in can be changed in the future
    if (mockFramework != MockFramework.MOCKITO) return ""

    return " && !org.mockito.Mockito.mockingDetails(o1).isMock()"
}

fun deepEquals(language: CodegenLanguage, mockFrameworkUsed: Boolean, mockFramework: MockFramework): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            static class FieldsPair {
                final Object o1;
                final Object o2;
        
                public FieldsPair(Object o1, Object o2) {
                    this.o1 = o1;
                    this.o2 = o2;
                }
        
                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    FieldsPair that = (FieldsPair) o;
                    return Objects.equals(o1, that.o1) && Objects.equals(o2, that.o2);
                }
        
                @Override
                public int hashCode() {
                    return Objects.hash(o1, o2);
                }
            }
        
            private boolean deepEquals(Object o1, Object o2) {
                return deepEquals(o1, o2, new java.util.HashSet<>());
            }
        
            private boolean deepEquals(Object o1, Object o2, java.util.Set<FieldsPair> visited) {
                visited.add(new FieldsPair(o1, o2));

                if (o1 == o2) {
                    return true;
                }
        
                if (o1 == null || o2 == null) {
                    return false;
                }
        
                if (o1 instanceof Iterable) {
                    if (!(o2 instanceof Iterable)) {
                        return false;
                    }
        
                    return iterablesDeepEquals((Iterable<?>) o1, (Iterable<?>) o2, visited);
                }
                
                if (o2 instanceof Iterable) {
                    return false;
                }
                
                if (o1 instanceof java.util.stream.BaseStream) {
                    if (!(o2 instanceof java.util.stream.BaseStream)) {
                        return false;
                    }
        
                    return streamsDeepEquals((java.util.stream.BaseStream<?, ?>) o1, (java.util.stream.BaseStream<?, ?>) o2, visited);
                }
        
                if (o2 instanceof java.util.stream.BaseStream) {
                    return false;
                }
        
                if (o1 instanceof java.util.Map) {
                    if (!(o2 instanceof java.util.Map)) {
                        return false;
                    }
        
                    return mapsDeepEquals((java.util.Map<?, ?>) o1, (java.util.Map<?, ?>) o2, visited);
                }
                
                if (o2 instanceof java.util.Map) {
                    return false;
                }
        
                Class<?> firstClass = o1.getClass();
                if (firstClass.isArray()) {
                    if (!o2.getClass().isArray()) {
                        return false;
                    }
        
                    // Primitive arrays should not appear here
                    return arraysDeepEquals(o1, o2, visited);
                }
        
                // common classes

                // check if class has custom equals method (including wrappers and strings)
                // It is very important to check it here but not earlier because iterables and maps also have custom equals 
                // based on elements equals 
                if (hasCustomEquals(firstClass)${isMockCondition(mockFrameworkUsed, mockFramework)}) {
                    return o1.equals(o2);
                }
        
                // common classes without custom equals, use comparison by fields
                final java.util.List<java.lang.reflect.Field> fields = new java.util.ArrayList<>();
                while (firstClass != Object.class) {
                    fields.addAll(java.util.Arrays.asList(firstClass.getDeclaredFields()));
                    // Interface should not appear here
                    firstClass = firstClass.getSuperclass();
                }
        
                for (java.lang.reflect.Field field : fields) {
                    field.setAccessible(true);
                    try {
                        final Object field1 = field.get(o1);
                        final Object field2 = field.get(o2);
                        if (!visited.contains(new FieldsPair(field1, field2)) && !deepEquals(field1, field2, visited)) {
                            return false;
                        }
                    } catch (IllegalArgumentException e) {
                        return false;
                    } catch (IllegalAccessException e) {
                        // should never occur because field was set accessible
                        return false;
                    }
                }
        
                return true;
            }
            """.trimIndent()
        }
        CodegenLanguage.KOTLIN -> {
            """
            private fun deepEquals(o1: kotlin.Any?, o2: kotlin.Any?): Boolean = deepEquals(o1, o2, hashSetOf())
            
            private fun deepEquals(
                o1: kotlin.Any?, 
                o2: kotlin.Any?, 
                visited: kotlin.collections.MutableSet<kotlin.Pair<kotlin.Any?, kotlin.Any?>>
            ): Boolean {
                visited += o1 to o2
                
                if (o1 === o2) return true
        
                if (o1 == null || o2 == null) return false
        
                if (o1 is kotlin.collections.Iterable<*>) {
                    return if (o2 !is kotlin.collections.Iterable<*>) false else iterablesDeepEquals(o1, o2, visited)
                }
                
                if (o2 is kotlin.collections.Iterable<*>) return false
                
                if (o1 is java.util.stream.BaseStream<*, *>) {
                    return if (o2 !is java.util.stream.BaseStream<*, *>) false else streamsDeepEquals(o1, o2, visited)
                }
                
                if (o2 is java.util.stream.BaseStream<*, *>) return false
        
                if (o1 is kotlin.collections.Map<*, *>) {
                    return if (o2 !is kotlin.collections.Map<*, *>) false else mapsDeepEquals(o1, o2, visited)
                }
                
                if (o2 is kotlin.collections.Map<*, *>) return false
        
                var firstClass: Class<*> = o1.javaClass
                if (firstClass.isArray) {
                    return if (!o2.javaClass.isArray) { 
                        false
                    } else { 
                        arraysDeepEquals(o1, o2, visited)
                    }
                }
        
                // check if class has custom equals method (including wrappers and strings)
                // It is very important to check it here but not earlier because iterables and maps also have custom equals
                // based on elements equals
                if (hasCustomEquals(firstClass)${isMockCondition(mockFrameworkUsed, mockFramework)}) { 
                    return o1 == o2
                }
        
                // common classes without custom equals, use comparison by fields
                val fields: kotlin.collections.MutableList<java.lang.reflect.Field> = mutableListOf()
                while (firstClass != kotlin.Any::class.java) {
                    fields += listOf(*firstClass.declaredFields)
                    // Interface should not appear here
                    firstClass = firstClass.superclass
                }
        
                for (field in fields) {
                    field.isAccessible = true
                    try {
                        val field1 = field[o1]
                        val field2 = field[o2]
                        if ((field1 to field2) !in visited && !deepEquals(field1, field2, visited)) return false
                    } catch (e: IllegalArgumentException) {
                        return false
                    } catch (e: IllegalAccessException) {
                        // should never occur
                        return false
                    }
                }
        
                return true
            }
            """.trimIndent()
        }
    }

fun arraysDeepEquals(language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            private boolean arraysDeepEquals(Object arr1, Object arr2, java.util.Set<FieldsPair> visited) {
                final int length = java.lang.reflect.Array.getLength(arr1);
                if (length != java.lang.reflect.Array.getLength(arr2)) {
                    return false;
                }
        
                for (int i = 0; i < length; i++) {
                    if (!deepEquals(java.lang.reflect.Array.get(arr1, i), java.lang.reflect.Array.get(arr2, i), visited)) {
                        return false;
                    }
                }
        
                return true;
            }
            """.trimIndent()
        }
        CodegenLanguage.KOTLIN -> {
            """
            private fun arraysDeepEquals(
                arr1: kotlin.Any?, 
                arr2: kotlin.Any?, 
                visited: kotlin.collections.MutableSet<kotlin.Pair<kotlin.Any?, kotlin.Any?>>
            ): Boolean {
                val size = java.lang.reflect.Array.getLength(arr1)
                if (size != java.lang.reflect.Array.getLength(arr2)) return false
        
                for (i in 0 until size) {
                    if (!deepEquals(java.lang.reflect.Array.get(arr1, i), java.lang.reflect.Array.get(arr2, i), visited)) { 
                        return false
                    }
                }
        
                return true
            }
            """.trimIndent()
        }
    }

fun iterablesDeepEquals(language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            private boolean iterablesDeepEquals(Iterable<?> i1, Iterable<?> i2, java.util.Set<FieldsPair> visited) {
                final java.util.Iterator<?> firstIterator = i1.iterator();
                final java.util.Iterator<?> secondIterator = i2.iterator();
                while (firstIterator.hasNext() && secondIterator.hasNext()) {
                    if (!deepEquals(firstIterator.next(), secondIterator.next(), visited)) {
                        return false;
                    }
                }
        
                if (firstIterator.hasNext()) {
                    return false;
                }
        
                return !secondIterator.hasNext();
            }
            """.trimIndent()
        }
        CodegenLanguage.KOTLIN -> {
            """
            private fun iterablesDeepEquals(
                i1: Iterable<*>, 
                i2: Iterable<*>, 
                visited: kotlin.collections.MutableSet<kotlin.Pair<kotlin.Any?, kotlin.Any?>>
            ): Boolean {
                val firstIterator = i1.iterator()
                val secondIterator = i2.iterator()
                while (firstIterator.hasNext() && secondIterator.hasNext()) {
                    if (!deepEquals(firstIterator.next(), secondIterator.next(), visited)) return false
                }
        
                return if (firstIterator.hasNext()) false else !secondIterator.hasNext()
            }
            """.trimIndent()
        }
    }

fun streamsDeepEquals(language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            private boolean streamsDeepEquals(
                java.util.stream.BaseStream<?, ?> s1, 
                java.util.stream.BaseStream<?, ?> s2, 
                java.util.Set<FieldsPair> visited
            ) {
                final java.util.Iterator<?> firstIterator = s1.iterator();
                final java.util.Iterator<?> secondIterator = s2.iterator();
                while (firstIterator.hasNext() && secondIterator.hasNext()) {
                    if (!deepEquals(firstIterator.next(), secondIterator.next(), visited)) {
                        return false;
                    }
                }
        
                if (firstIterator.hasNext()) {
                    return false;
                }
        
                return !secondIterator.hasNext();
            }
            """.trimIndent()
        }
        CodegenLanguage.KOTLIN -> {
            """
            private fun streamsDeepEquals(
                s1: java.util.stream.BaseStream<*, *>, 
                s2: java.util.stream.BaseStream<*, *>, 
                visited: kotlin.collections.MutableSet<kotlin.Pair<kotlin.Any?, kotlin.Any?>>
            ): Boolean {
                val firstIterator = s1.iterator()
                val secondIterator = s2.iterator()
                while (firstIterator.hasNext() && secondIterator.hasNext()) {
                    if (!deepEquals(firstIterator.next(), secondIterator.next(), visited)) return false
                }
        
                return if (firstIterator.hasNext()) false else !secondIterator.hasNext()
            }
            """.trimIndent()
        }
    }

fun mapsDeepEquals(language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            private boolean mapsDeepEquals(
                java.util.Map<?, ?> m1, 
                java.util.Map<?, ?> m2, 
                java.util.Set<FieldsPair> visited
            ) {
                final java.util.Iterator<? extends java.util.Map.Entry<?, ?>> firstIterator = m1.entrySet().iterator();
                final java.util.Iterator<? extends java.util.Map.Entry<?, ?>> secondIterator = m2.entrySet().iterator();
                while (firstIterator.hasNext() && secondIterator.hasNext()) {
                    final java.util.Map.Entry<?, ?> firstEntry = firstIterator.next();
                    final java.util.Map.Entry<?, ?> secondEntry = secondIterator.next();
        
                    if (!deepEquals(firstEntry.getKey(), secondEntry.getKey(), visited)) {
                        return false;
                    }
        
                    if (!deepEquals(firstEntry.getValue(), secondEntry.getValue(), visited)) {
                        return false;
                    }
                }
        
                if (firstIterator.hasNext()) {
                    return false;
                }
        
                return !secondIterator.hasNext();
            }
            """.trimIndent()
        }
        CodegenLanguage.KOTLIN -> {
            """
            private fun mapsDeepEquals(
                m1: kotlin.collections.Map<*, *>, 
                m2: kotlin.collections.Map<*, *>, 
                visited: kotlin.collections.MutableSet<kotlin.Pair<kotlin.Any?, kotlin.Any?>>
            ): Boolean {
                val firstIterator = m1.entries.iterator()
                val secondIterator = m2.entries.iterator()
                while (firstIterator.hasNext() && secondIterator.hasNext()) {
                    val firstEntry = firstIterator.next()
                    val secondEntry = secondIterator.next()
        
                    if (!deepEquals(firstEntry.key, secondEntry.key, visited)) return false
        
                    if (!deepEquals(firstEntry.value, secondEntry.value, visited)) return false
                }
        
                return if (firstIterator.hasNext()) false else !secondIterator.hasNext()
            }
            """.trimIndent()
        }
    }

fun hasCustomEquals(language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            private boolean hasCustomEquals(Class<?> clazz) {
                while (!Object.class.equals(clazz)) {
                    try {
                        clazz.getDeclaredMethod("equals", Object.class);
                        return true;
                    } catch (Exception e) { 
                        // Interface should not appear here
                        clazz = clazz.getSuperclass();
                    }
                }
        
                return false;
            }
            """.trimIndent()
        }
        CodegenLanguage.KOTLIN -> {
            """
            private fun hasCustomEquals(clazz: Class<*>): Boolean {
                var c = clazz
                while (kotlin.Any::class.java != c) {
                    try {
                        c.getDeclaredMethod("equals", kotlin.Any::class.java)
                        return true
                    } catch (e: Exception) {
                        // Interface should not appear here
                        c = c.superclass
                    }
                }
                return false
            }
            """.trimIndent()
        }
    }

fun getArrayLength(codegenLanguage: CodegenLanguage) =
    when (codegenLanguage) {
        CodegenLanguage.JAVA ->
            """
            private static int getArrayLength(Object arr) {
                return java.lang.reflect.Array.getLength(arr);
            }
            """.trimIndent()
        CodegenLanguage.KOTLIN ->
            """
            private fun getArrayLength(arr: kotlin.Any?): Int = java.lang.reflect.Array.getLength(arr)
            """.trimIndent()
    }

internal fun CgContextOwner.importUtilMethodDependencies(id: MethodId) {
    for (classId in currentTestClass.regularImportsByUtilMethod(id, codegenLanguage)) {
        importIfNeeded(classId)
    }
    for (methodId in currentTestClass.staticImportsByUtilMethod(id)) {
        collectedImports += StaticImport(methodId.classId.canonicalName, methodId.name)
    }
}

private fun ClassId.regularImportsByUtilMethod(id: MethodId, codegenLanguage: CodegenLanguage): List<ClassId> {
    val fieldClassId = Field::class.id
    return when (id) {
        getUnsafeInstanceMethodId -> listOf(fieldClassId)
        createInstanceMethodId -> listOf(java.lang.reflect.InvocationTargetException::class.id)
        createArrayMethodId -> listOf(java.lang.reflect.Array::class.id)
        setFieldMethodId -> listOf(fieldClassId, Modifier::class.id)
        setStaticFieldMethodId -> listOf(fieldClassId, Modifier::class.id)
        getFieldValueMethodId -> listOf(fieldClassId, Modifier::class.id)
        getStaticFieldValueMethodId -> listOf(fieldClassId, Modifier::class.id)
        getEnumConstantByNameMethodId -> listOf(fieldClassId)
        deepEqualsMethodId -> when (codegenLanguage) {
            CodegenLanguage.JAVA -> listOf(
                Objects::class.id,
                Iterable::class.id,
                Map::class.id,
                List::class.id,
                ArrayList::class.id,
                Set::class.id,
                HashSet::class.id,
                fieldClassId,
                Arrays::class.id
            )
            CodegenLanguage.KOTLIN -> listOf(fieldClassId, Arrays::class.id)
        }
        arraysDeepEqualsMethodId -> when (codegenLanguage) {
            CodegenLanguage.JAVA -> listOf(java.lang.reflect.Array::class.id, Set::class.id)
            CodegenLanguage.KOTLIN -> listOf(java.lang.reflect.Array::class.id)
        }
        iterablesDeepEqualsMethodId -> when (codegenLanguage) {
            CodegenLanguage.JAVA -> listOf(Iterable::class.id, Iterator::class.id, Set::class.id)
            CodegenLanguage.KOTLIN -> emptyList()
        }
        streamsDeepEqualsMethodId -> when (codegenLanguage) {
            CodegenLanguage.JAVA -> listOf(java.util.stream.BaseStream::class.id, Set::class.id)
            CodegenLanguage.KOTLIN -> emptyList()
        }
        mapsDeepEqualsMethodId -> when (codegenLanguage) {
            CodegenLanguage.JAVA -> listOf(Map::class.id, Iterator::class.id, Set::class.id)
            CodegenLanguage.KOTLIN -> emptyList()
        }
        hasCustomEqualsMethodId -> emptyList()
        getArrayLengthMethodId -> listOf(java.lang.reflect.Array::class.id)
        else -> error("Unknown util method for class $this: $id")
    }
}

// Note: for now always returns an empty list, because no util method
// requires static imports, but this may change in the future
@Suppress("unused", "unused_parameter")
private fun ClassId.staticImportsByUtilMethod(id: MethodId): List<MethodId> = emptyList()
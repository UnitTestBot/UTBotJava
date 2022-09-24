package org.utbot.framework.codegen.model.visitor

import org.utbot.framework.codegen.StaticImport
import org.utbot.framework.codegen.model.constructor.builtin.TestClassUtilMethodProvider
import org.utbot.framework.codegen.model.constructor.builtin.UtilMethodProvider
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.util.importIfNeeded
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.util.fieldClassId
import org.utbot.framework.plugin.api.util.id
import java.lang.invoke.CallSite
import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Arrays
import java.util.Objects
import java.util.stream.Collectors

private enum class Visibility(val text: String) {
    PRIVATE("private"),
    @Suppress("unused")
    PROTECTED("protected"),
    PUBLIC("public");

    infix fun by(language: CodegenLanguage): String {
        if (this == PUBLIC && language == CodegenLanguage.KOTLIN) {
            // public is default in Kotlin
            return ""
        }
        return "$text "
    }
}

// TODO: This method may throw an exception that will crash rendering.
// TODO: Add isolation on rendering: https://github.com/UnitTestBot/UTBotJava/issues/853
internal fun UtilMethodProvider.utilMethodTextById(
    id: MethodId,
    mockFrameworkUsed: Boolean,
    mockFramework: MockFramework,
    codegenLanguage: CodegenLanguage
): String {
    // If util methods are declared in the test class, then they are private. Otherwise, they are public.
    val visibility = if (this is TestClassUtilMethodProvider) Visibility.PRIVATE else Visibility.PUBLIC
    return with(this) {
        when (id) {
            getUnsafeInstanceMethodId -> getUnsafeInstance(visibility, codegenLanguage)
            createInstanceMethodId -> createInstance(visibility, codegenLanguage)
            createArrayMethodId -> createArray(visibility, codegenLanguage)
            setFieldMethodId -> setField(visibility, codegenLanguage)
            setStaticFieldMethodId -> setStaticField(visibility, codegenLanguage)
            getFieldValueMethodId -> getFieldValue(visibility, codegenLanguage)
            getStaticFieldValueMethodId -> getStaticFieldValue(visibility, codegenLanguage)
            getEnumConstantByNameMethodId -> getEnumConstantByName(visibility, codegenLanguage)
            deepEqualsMethodId -> deepEquals(visibility, codegenLanguage, mockFrameworkUsed, mockFramework)
            arraysDeepEqualsMethodId -> arraysDeepEquals(visibility, codegenLanguage)
            iterablesDeepEqualsMethodId -> iterablesDeepEquals(visibility, codegenLanguage)
            streamsDeepEqualsMethodId -> streamsDeepEquals(visibility, codegenLanguage)
            mapsDeepEqualsMethodId -> mapsDeepEquals(visibility, codegenLanguage)
            hasCustomEqualsMethodId -> hasCustomEquals(visibility, codegenLanguage)
            getArrayLengthMethodId -> getArrayLength(visibility, codegenLanguage)
            buildStaticLambdaMethodId -> buildStaticLambda(visibility, codegenLanguage)
            buildLambdaMethodId -> buildLambda(visibility, codegenLanguage)
            // the following methods are used only by other util methods, so they can always be private
            getLookupInMethodId -> getLookupIn(codegenLanguage)
            getLambdaCapturedArgumentTypesMethodId -> getLambdaCapturedArgumentTypes(codegenLanguage)
            getLambdaCapturedArgumentValuesMethodId -> getLambdaCapturedArgumentValues(codegenLanguage)
            getInstantiatedMethodTypeMethodId -> getInstantiatedMethodType(codegenLanguage)
            getLambdaMethodMethodId -> getLambdaMethod(codegenLanguage)
            getSingleAbstractMethodMethodId -> getSingleAbstractMethod(codegenLanguage)
            else -> error("Unknown util method for class $this: $id")
        }
    }
}

// TODO: This method may throw an exception that will crash rendering.
// TODO: Add isolation on rendering: https://github.com/UnitTestBot/UTBotJava/issues/853
internal fun UtilMethodProvider.auxiliaryClassTextById(id: ClassId, codegenLanguage: CodegenLanguage): String =
    with(this) {
        when (id) {
            capturedArgumentClassId -> capturedArgumentClass(codegenLanguage)
            else -> error("Unknown auxiliary class: $id")
        }
    }

private fun getEnumConstantByName(visibility: Visibility, language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            ${visibility by language}static Object getEnumConstantByName(Class<?> enumClass, String name) throws IllegalAccessException {
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
            ${visibility by language}fun getEnumConstantByName(enumClass: Class<*>, name: String): kotlin.Any? {
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

private fun getStaticFieldValue(visibility: Visibility, language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            ${visibility by language}static Object getStaticFieldValue(Class<?> clazz, String fieldName) throws IllegalAccessException, NoSuchFieldException {
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
            ${visibility by language}fun getStaticFieldValue(clazz: Class<*>, fieldName: String): kotlin.Any? {
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

private fun getFieldValue(visibility: Visibility, language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            ${visibility by language}static Object getFieldValue(Object obj, String fieldClassName, String fieldName) throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
                Class<?> clazz = Class.forName(fieldClassName);
                java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                
                field.setAccessible(true);
                java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
                
                return field.get(obj);
            }
        """
        }
        CodegenLanguage.KOTLIN -> {
            """
            ${visibility by language}fun getFieldValue(any: kotlin.Any, fieldClassName: String, fieldName: String): kotlin.Any? {
                val clazz: Class<*> = Class.forName(fieldClassName)
                val field: java.lang.reflect.Field = clazz.getDeclaredField(fieldName)
                
                field.isAccessible = true
                val modifiersField: java.lang.reflect.Field = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
                modifiersField.isAccessible = true
                modifiersField.setInt(field, field.modifiers and java.lang.reflect.Modifier.FINAL.inv())
                
                return field.get(any)
            }
        """
        }
    }.trimIndent()

private fun setStaticField(visibility: Visibility, language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            ${visibility by language}static void setStaticField(Class<?> clazz, String fieldName, Object fieldValue) throws NoSuchFieldException, IllegalAccessException {
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
            ${visibility by language}fun setStaticField(defaultClass: Class<*>, fieldName: String, fieldValue: kotlin.Any?) {
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

private fun setField(visibility: Visibility, language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            ${visibility by language}static void setField(Object object, String fieldClassName, String fieldName, Object fieldValue) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
                Class<?> clazz = Class.forName(fieldClassName);
                java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                
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
            ${visibility by language}fun setField(any: kotlin.Any, fieldClassName: String, fieldName: String, fieldValue: kotlin.Any?) {
                val clazz: Class<*> = Class.forName(fieldClassName)
                val field: java.lang.reflect.Field = clazz.getDeclaredField(fieldName)
        
                val modifiersField: java.lang.reflect.Field = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
                modifiersField.isAccessible = true
                modifiersField.setInt(field, field.modifiers and java.lang.reflect.Modifier.FINAL.inv())
        
                field.isAccessible = true
                field.set(any, fieldValue)
            }
        """
        }
    }.trimIndent()

private fun createArray(visibility: Visibility, language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            ${visibility by language}static Object[] createArray(String className, int length, Object... values) throws ClassNotFoundException {
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
            ${visibility by language}fun createArray(
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

private fun createInstance(visibility: Visibility, language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            ${visibility by language}static Object createInstance(String className) throws Exception {
                Class<?> clazz = Class.forName(className);
                return Class.forName("sun.misc.Unsafe").getDeclaredMethod("allocateInstance", Class.class)
                    .invoke(getUnsafeInstance(), clazz);
            }
            """
        }
        CodegenLanguage.KOTLIN -> {
            """
            ${visibility by language}fun createInstance(className: String): kotlin.Any {
                val clazz: Class<*> = Class.forName(className)
                return Class.forName("sun.misc.Unsafe").getDeclaredMethod("allocateInstance", Class::class.java)
                    .invoke(getUnsafeInstance(), clazz)
            }
            """
        }
    }.trimIndent()

private fun getUnsafeInstance(visibility: Visibility, language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            ${visibility by language}static Object getUnsafeInstance() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
                java.lang.reflect.Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
                f.setAccessible(true);
                return f.get(null);
            }
            """
        }
        CodegenLanguage.KOTLIN -> {
            """
            ${visibility by language}fun getUnsafeInstance(): kotlin.Any? {
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

    return when (mockFramework) {
        MockFramework.MOCKITO -> " && !org.mockito.Mockito.mockingDetails(o1).isMock()"
        // in case we will add any other mock frameworks, newer Kotlin compiler versions
        // will report a non-exhaustive 'when', so we will not forget to support them here as well
    }
}

private fun deepEquals(
    visibility: Visibility,
    language: CodegenLanguage,
    mockFrameworkUsed: Boolean,
    mockFramework: MockFramework
): String =
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
                    return java.util.Objects.equals(o1, that.o1) && java.util.Objects.equals(o2, that.o2);
                }
        
                @Override
                public int hashCode() {
                    return java.util.Objects.hash(o1, o2);
                }
            }
        
            ${visibility by language}static boolean deepEquals(Object o1, Object o2) {
                return deepEquals(o1, o2, new java.util.HashSet<>());
            }
        
            private static boolean deepEquals(Object o1, Object o2, java.util.Set<FieldsPair> visited) {
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
                
                if (o1 instanceof java.util.stream.Stream) {
                    if (!(o2 instanceof java.util.stream.Stream)) {
                        return false;
                    }
        
                    return streamsDeepEquals((java.util.stream.Stream<?>) o1, (java.util.stream.Stream<?>) o2, visited);
                }
        
                if (o2 instanceof java.util.stream.Stream) {
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
            ${visibility by language}fun deepEquals(o1: kotlin.Any?, o2: kotlin.Any?): Boolean = deepEquals(o1, o2, hashSetOf())
            
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
                
                if (o1 is java.util.stream.Stream<*>) {
                    return if (o2 !is java.util.stream.Stream<*>) false else streamsDeepEquals(o1, o2, visited)
                }
                
                if (o2 is java.util.stream.Stream<*>) return false
        
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

private fun arraysDeepEquals(visibility: Visibility, language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            ${visibility by language}static boolean arraysDeepEquals(Object arr1, Object arr2, java.util.Set<FieldsPair> visited) {
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
            ${visibility by language}fun arraysDeepEquals(
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

private fun iterablesDeepEquals(visibility: Visibility, language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            ${visibility by language}static boolean iterablesDeepEquals(Iterable<?> i1, Iterable<?> i2, java.util.Set<FieldsPair> visited) {
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
            ${visibility by language}fun iterablesDeepEquals(
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

private fun streamsDeepEquals(visibility: Visibility, language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            ${visibility by language}static boolean streamsDeepEquals(
                java.util.stream.Stream<?> s1, 
                java.util.stream.Stream<?> s2, 
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
            ${visibility by language}fun streamsDeepEquals(
                s1: java.util.stream.Stream<*>, 
                s2: java.util.stream.Stream<*>, 
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

private fun mapsDeepEquals(visibility: Visibility, language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            ${visibility by language}static boolean mapsDeepEquals(
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
            ${visibility by language}fun mapsDeepEquals(
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

private fun hasCustomEquals(visibility: Visibility, language: CodegenLanguage): String =
    when (language) {
        CodegenLanguage.JAVA -> {
            """
            ${visibility by language}static boolean hasCustomEquals(Class<?> clazz) {
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
            ${visibility by language}fun hasCustomEquals(clazz: Class<*>): Boolean {
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

private fun getArrayLength(visibility: Visibility, language: CodegenLanguage) =
    when (language) {
        CodegenLanguage.JAVA ->
            """
            ${visibility by language}static int getArrayLength(Object arr) {
                return java.lang.reflect.Array.getLength(arr);
            }
            """.trimIndent()
        CodegenLanguage.KOTLIN ->
            """
            ${visibility by language}fun getArrayLength(arr: kotlin.Any?): Int = java.lang.reflect.Array.getLength(arr)
            """.trimIndent()
    }

private fun buildStaticLambda(visibility: Visibility, language: CodegenLanguage) =
    when (language) {
        CodegenLanguage.JAVA ->
            """
            /**
             * @param samType a class representing a functional interface.
             * @param declaringClass a class where the lambda is declared.
             * @param lambdaName a name of the synthetic method that represents a lambda.
             * @param capturedArguments a vararg containing {@link CapturedArgument} instances representing
             * values that the given lambda has captured.
             * @return an {@link Object} that represents an instance of the given functional interface {@code samType}
             * and implements its single abstract method with the behavior of the given lambda.
             */
            ${visibility by language}static Object buildStaticLambda(
                    Class<?> samType,
                    Class<?> declaringClass,
                    String lambdaName,
                    CapturedArgument... capturedArguments
            ) throws Throwable {
                // Create lookup for class where the lambda is declared in.
                java.lang.invoke.MethodHandles.Lookup caller = getLookupIn(declaringClass);
        
                // Obtain the single abstract method of a functional interface whose instance we are building.
                // For example, for `java.util.function.Predicate` it will be method `test`.
                java.lang.reflect.Method singleAbstractMethod = getSingleAbstractMethod(samType);
                String invokedName = singleAbstractMethod.getName();
                // Method type of single abstract method of the target functional interface.
                java.lang.invoke.MethodType samMethodType = java.lang.invoke.MethodType.methodType(singleAbstractMethod.getReturnType(), singleAbstractMethod.getParameterTypes());
        
                java.lang.reflect.Method lambdaMethod = getLambdaMethod(declaringClass, lambdaName);
                lambdaMethod.setAccessible(true);
                java.lang.invoke.MethodType lambdaMethodType = java.lang.invoke.MethodType.methodType(lambdaMethod.getReturnType(), lambdaMethod.getParameterTypes());
                java.lang.invoke.MethodHandle lambdaMethodHandle = caller.findStatic(declaringClass, lambdaName, lambdaMethodType);
                
                Class<?>[] capturedArgumentTypes = getLambdaCapturedArgumentTypes(capturedArguments);
                java.lang.invoke.MethodType invokedType = java.lang.invoke.MethodType.methodType(samType, capturedArgumentTypes);
                java.lang.invoke.MethodType instantiatedMethodType = getInstantiatedMethodType(lambdaMethod, capturedArgumentTypes);

                // Create a CallSite for the given lambda.
                java.lang.invoke.CallSite site = java.lang.invoke.LambdaMetafactory.metafactory(
                        caller,
                        invokedName,
                        invokedType,
                        samMethodType,
                        lambdaMethodHandle,
                        instantiatedMethodType);
        
                Object[] capturedValues = getLambdaCapturedArgumentValues(capturedArguments);
        
                // Get MethodHandle and pass captured values to it to obtain an object
                // that represents the target functional interface instance.
                java.lang.invoke.MethodHandle handle = site.getTarget();
                return handle.invokeWithArguments(capturedValues);
            }
            """.trimIndent()
        CodegenLanguage.KOTLIN ->
            """
            /**
             * @param samType a class representing a functional interface.
             * @param declaringClass a class where the lambda is declared.
             * @param lambdaName a name of the synthetic method that represents a lambda.
             * @param capturedArguments a vararg containing [CapturedArgument] instances representing
             * values that the given lambda has captured.
             * @return an [Any] that represents an instance of the given functional interface `samType`
             * and implements its single abstract method with the behavior of the given lambda.
             */
            ${visibility by language}fun buildStaticLambda(
                samType: Class<*>,
                declaringClass: Class<*>,
                lambdaName: String,
                vararg capturedArguments: CapturedArgument
            ): Any {
                // Create lookup for class where the lambda is declared in.
                val caller = getLookupIn(declaringClass)
    
                // Obtain the single abstract method of a functional interface whose instance we are building.
                // For example, for `java.util.function.Predicate` it will be method `test`.
                val singleAbstractMethod = getSingleAbstractMethod(samType)
                val invokedName = singleAbstractMethod.name
                // Method type of single abstract method of the target functional interface.
                val samMethodType = java.lang.invoke.MethodType.methodType(singleAbstractMethod.returnType, singleAbstractMethod.parameterTypes)
                
                val lambdaMethod = getLambdaMethod(declaringClass, lambdaName)
                lambdaMethod.isAccessible = true
                val lambdaMethodType = java.lang.invoke.MethodType.methodType(lambdaMethod.returnType, lambdaMethod.parameterTypes)
                val lambdaMethodHandle = caller.findStatic(declaringClass, lambdaName, lambdaMethodType)
                
                val capturedArgumentTypes = getLambdaCapturedArgumentTypes(*capturedArguments)
                val invokedType = java.lang.invoke.MethodType.methodType(samType, capturedArgumentTypes)
                val instantiatedMethodType = getInstantiatedMethodType(lambdaMethod, capturedArgumentTypes)
    
                // Create a CallSite for the given lambda.
                val site = java.lang.invoke.LambdaMetafactory.metafactory(
                    caller,
                    invokedName,
                    invokedType,
                    samMethodType,
                    lambdaMethodHandle,
                    instantiatedMethodType
                )
                val capturedValues = getLambdaCapturedArgumentValues(*capturedArguments)
    
                // Get MethodHandle and pass captured values to it to obtain an object
                // that represents the target functional interface instance.
                val handle = site.target
                return handle.invokeWithArguments(*capturedValues)
            }
            """.trimIndent()
    }

private fun buildLambda(visibility: Visibility, language: CodegenLanguage) =
    when (language) {
        CodegenLanguage.JAVA ->
            """
            /**
             * @param samType a class representing a functional interface.
             * @param declaringClass a class where the lambda is declared.
             * @param lambdaName a name of the synthetic method that represents a lambda.
             * @param capturedReceiver an object of {@code declaringClass} that is captured by the lambda.
             * When the synthetic lambda method is not static, it means that the lambda captures an instance
             * of the class it is declared in.
             * @param capturedArguments a vararg containing {@link CapturedArgument} instances representing
             * values that the given lambda has captured.
             * @return an {@link Object} that represents an instance of the given functional interface {@code samType}
             * and implements its single abstract method with the behavior of the given lambda.
             */
            ${visibility by language}static Object buildLambda(
                    Class<?> samType,
                    Class<?> declaringClass,
                    String lambdaName,
                    Object capturedReceiver,
                    CapturedArgument... capturedArguments
            ) throws Throwable {
                // Create lookup for class where the lambda is declared in.
                java.lang.invoke.MethodHandles.Lookup caller = getLookupIn(declaringClass);
        
                // Obtain the single abstract method of a functional interface whose instance we are building.
                // For example, for `java.util.function.Predicate` it will be method `test`.
                java.lang.reflect.Method singleAbstractMethod = getSingleAbstractMethod(samType);
                String invokedName = singleAbstractMethod.getName();
                // Method type of single abstract method of the target functional interface.
                java.lang.invoke.MethodType samMethodType = java.lang.invoke.MethodType.methodType(singleAbstractMethod.getReturnType(), singleAbstractMethod.getParameterTypes());
        
                java.lang.reflect.Method lambdaMethod = getLambdaMethod(declaringClass, lambdaName);
                lambdaMethod.setAccessible(true);
                java.lang.invoke.MethodType lambdaMethodType = java.lang.invoke.MethodType.methodType(lambdaMethod.getReturnType(), lambdaMethod.getParameterTypes());
                java.lang.invoke.MethodHandle lambdaMethodHandle = caller.findVirtual(declaringClass, lambdaName, lambdaMethodType);
        
                Class<?>[] capturedArgumentTypes = getLambdaCapturedArgumentTypes(capturedArguments);
                java.lang.invoke.MethodType invokedType = java.lang.invoke.MethodType.methodType(samType, capturedReceiver.getClass(), capturedArgumentTypes);
                java.lang.invoke.MethodType instantiatedMethodType = getInstantiatedMethodType(lambdaMethod, capturedArgumentTypes);
        
                // Create a CallSite for the given lambda.
                java.lang.invoke.CallSite site = java.lang.invoke.LambdaMetafactory.metafactory(
                        caller,
                        invokedName,
                        invokedType,
                        samMethodType,
                        lambdaMethodHandle,
                        instantiatedMethodType);
        
                Object[] capturedArgumentValues = getLambdaCapturedArgumentValues(capturedArguments);
        
                // This array will contain the value of captured receiver
                // (`this` instance of class where the lambda is declared)
                // and the values of captured arguments.
                Object[] capturedValues = new Object[capturedArguments.length + 1];
        
                // Setting the captured receiver value.
                capturedValues[0] = capturedReceiver;
        
                // Setting the captured argument values.
                System.arraycopy(capturedArgumentValues, 0, capturedValues, 1, capturedArgumentValues.length);
        
                // Get MethodHandle and pass captured values to it to obtain an object
                // that represents the target functional interface instance.
                java.lang.invoke.MethodHandle handle = site.getTarget();
                return handle.invokeWithArguments(capturedValues);
            }
            """.trimIndent()
        CodegenLanguage.KOTLIN ->
            """
            /**
             * @param samType a class representing a functional interface.
             * @param declaringClass a class where the lambda is declared.
             * @param lambdaName a name of the synthetic method that represents a lambda.
             * @param capturedReceiver an object of `declaringClass` that is captured by the lambda.
             * When the synthetic lambda method is not static, it means that the lambda captures an instance
             * of the class it is declared in.
             * @param capturedArguments a vararg containing [CapturedArgument] instances representing
             * values that the given lambda has captured.
             * @return an [Any] that represents an instance of the given functional interface `samType`
             * and implements its single abstract method with the behavior of the given lambda.
             */
            ${visibility by language}fun buildLambda(
                samType: Class<*>,
                declaringClass: Class<*>,
                lambdaName: String,
                capturedReceiver: Any,
                vararg capturedArguments: CapturedArgument
            ): Any {
                // Create lookup for class where the lambda is declared in.
                val caller = getLookupIn(declaringClass)
    
                // Obtain the single abstract method of a functional interface whose instance we are building.
                // For example, for `java.util.function.Predicate` it will be method `test`.
                val singleAbstractMethod = getSingleAbstractMethod(samType)
                val invokedName = singleAbstractMethod.name
                // Method type of single abstract method of the target functional interface.
                val samMethodType = java.lang.invoke.MethodType.methodType(singleAbstractMethod.returnType, singleAbstractMethod.parameterTypes)
                
                val lambdaMethod = getLambdaMethod(declaringClass, lambdaName)
                lambdaMethod.isAccessible = true
                val lambdaMethodType = java.lang.invoke.MethodType.methodType(lambdaMethod.returnType, lambdaMethod.parameterTypes)
                val lambdaMethodHandle = caller.findVirtual(declaringClass, lambdaName, lambdaMethodType)
                
                val capturedArgumentTypes = getLambdaCapturedArgumentTypes(*capturedArguments)
                val invokedType = java.lang.invoke.MethodType.methodType(samType, capturedReceiver.javaClass, *capturedArgumentTypes)
                val instantiatedMethodType = getInstantiatedMethodType(lambdaMethod, capturedArgumentTypes)
    
                // Create a CallSite for the given lambda.
                val site = java.lang.invoke.LambdaMetafactory.metafactory(
                    caller,
                    invokedName,
                    invokedType,
                    samMethodType,
                    lambdaMethodHandle,
                    instantiatedMethodType
                )
                val capturedValues = mutableListOf<Any?>()
                    .apply {
                        add(capturedReceiver)
                        addAll(getLambdaCapturedArgumentValues(*capturedArguments))
                    }.toTypedArray()
    
    
                // Get MethodHandle and pass captured values to it to obtain an object
                // that represents the target functional interface instance.
                val handle = site.target
                return handle.invokeWithArguments(*capturedValues)
            }
            """.trimIndent()
    }

private fun getLookupIn(language: CodegenLanguage) =
    when (language) {
        CodegenLanguage.JAVA ->
            """
            /**
             * @param clazz a class to create lookup instance for.
             * @return {@link java.lang.invoke.MethodHandles.Lookup} instance for the given {@code clazz}.
             * It can be used, for example, to search methods of this {@code clazz}, even the {@code private} ones.
             */
            private static java.lang.invoke.MethodHandles.Lookup getLookupIn(Class<?> clazz) throws IllegalAccessException, NoSuchFieldException {
                java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.lookup().in(clazz);
        
                // Allow lookup to access all members of declaringClass, including the private ones.
                // For example, it is useful to access private synthetic methods representing lambdas.
                java.lang.reflect.Field allowedModes = java.lang.invoke.MethodHandles.Lookup.class.getDeclaredField("allowedModes");
                allowedModes.setAccessible(true);
                allowedModes.setInt(lookup, java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.PROTECTED | java.lang.reflect.Modifier.PRIVATE | java.lang.reflect.Modifier.STATIC);
        
                return lookup;
            }
            """.trimIndent()
        CodegenLanguage.KOTLIN ->
            """
            /**
             * @param clazz a class to create lookup instance for.
             * @return [java.lang.invoke.MethodHandles.Lookup] instance for the given [clazz].
             * It can be used, for example, to search methods of this [clazz], even the `private` ones.
             */
            private fun getLookupIn(clazz: Class<*>): java.lang.invoke.MethodHandles.Lookup {
                val lookup = java.lang.invoke.MethodHandles.lookup().`in`(clazz)
    
                // Allow lookup to access all members of declaringClass, including the private ones.
                // For example, it is useful to access private synthetic methods representing lambdas.
                val allowedModes = java.lang.invoke.MethodHandles.Lookup::class.java.getDeclaredField("allowedModes")
                allowedModes.isAccessible = true
                allowedModes.setInt(lookup, java.lang.reflect.Modifier.PUBLIC or java.lang.reflect.Modifier.PROTECTED or java.lang.reflect.Modifier.PRIVATE or java.lang.reflect.Modifier.STATIC)
    
                return lookup
            }
            """.trimIndent()
    }

private fun getLambdaCapturedArgumentTypes(language: CodegenLanguage) =
    when (language) {
        CodegenLanguage.JAVA ->
            """
            /**
             * @param capturedArguments values captured by some lambda. Note that this argument does not contain
             * the possibly captured instance of the class where the lambda is declared.
             * It contains all the other captured values. They are represented as arguments of the synthetic method
             * that the lambda is compiled into. Hence, the name of the argument.
             * @return types of the given {@code capturedArguments}.
             * These types are required to build {@code invokedType}, which represents
             * the target functional interface with info about captured values' types.
             * See {@link java.lang.invoke.LambdaMetafactory#metafactory} method documentation for more details on what {@code invokedType} is.
             */
            private static Class<?>[] getLambdaCapturedArgumentTypes(CapturedArgument... capturedArguments) {
                Class<?>[] capturedArgumentTypes = new Class<?>[capturedArguments.length];
                for (int i = 0; i < capturedArguments.length; i++) {
                    capturedArgumentTypes[i] = capturedArguments[i].type;
                }
                return capturedArgumentTypes;
            }
            """.trimIndent()
        CodegenLanguage.KOTLIN ->
            """
            /**
             * @param capturedArguments values captured by some lambda. Note that this argument does not contain
             * the possibly captured instance of the class where the lambda is declared.
             * It contains all the other captured values. They are represented as arguments of the synthetic method
             * that the lambda is compiled into. Hence, the name of the argument.
             * @return types of the given `capturedArguments`.
             * These types are required to build `invokedType`, which represents
             * the target functional interface with info about captured values' types.
             * See [java.lang.invoke.LambdaMetafactory.metafactory] method documentation for more details on what `invokedType` is.
             */
            private fun getLambdaCapturedArgumentTypes(vararg capturedArguments: CapturedArgument): Array<Class<*>> {
                return capturedArguments
                    .map { it.type }
                    .toTypedArray()
            }
            """.trimIndent()
    }

private fun getLambdaCapturedArgumentValues(language: CodegenLanguage) =
    when (language) {
        CodegenLanguage.JAVA ->
            """
            /**
             * Obtain captured values to be used as captured arguments in the lambda call.
             */
            private static Object[] getLambdaCapturedArgumentValues(CapturedArgument... capturedArguments) {
                return java.util.Arrays.stream(capturedArguments)
                        .map(argument -> argument.value)
                        .toArray();
            }
            """.trimIndent()
        CodegenLanguage.KOTLIN ->
            """
            /**
             * Obtain captured values to be used as captured arguments in the lambda call.
             */
            private fun getLambdaCapturedArgumentValues(vararg capturedArguments: CapturedArgument): Array<Any?> {
                return capturedArguments
                    .map { it.value }
                    .toTypedArray()
            }
            """.trimIndent()
    }

private fun getInstantiatedMethodType(language: CodegenLanguage) =
    when (language) {
        CodegenLanguage.JAVA ->
            """
            /**
             * @param lambdaMethod {@link java.lang.reflect.Method} that represents a synthetic method for lambda.
             * @param capturedArgumentTypes types of values captured by lambda.
             * @return {@link java.lang.invoke.MethodType} that represents the value of argument {@code instantiatedMethodType}
             * of method {@link java.lang.invoke.LambdaMetafactory#metafactory}.
             */
            private static java.lang.invoke.MethodType getInstantiatedMethodType(java.lang.reflect.Method lambdaMethod, Class<?>[] capturedArgumentTypes) {
                // Types of arguments of synthetic method (representing lambda) without the types of captured values.
                java.util.List<Class<?>> instantiatedMethodParamTypeList = java.util.Arrays.stream(lambdaMethod.getParameterTypes())
                        .skip(capturedArgumentTypes.length)
                        .collect(java.util.stream.Collectors.toList());
        
                // The same types, but stored in an array.
                Class<?>[] instantiatedMethodParamTypes = new Class<?>[instantiatedMethodParamTypeList.size()];
                for (int i = 0; i < instantiatedMethodParamTypeList.size(); i++) {
                    instantiatedMethodParamTypes[i] = instantiatedMethodParamTypeList.get(i);
                }
        
                return java.lang.invoke.MethodType.methodType(lambdaMethod.getReturnType(), instantiatedMethodParamTypes);
            }
            """.trimIndent()
        CodegenLanguage.KOTLIN ->
            """
            /**
             * @param lambdaMethod [java.lang.reflect.Method] that represents a synthetic method for lambda.
             * @param capturedArgumentTypes types of values captured by lambda.
             * @return [java.lang.invoke.MethodType] that represents the value of argument `instantiatedMethodType`
             * of method [java.lang.invoke.LambdaMetafactory.metafactory].
             */
            private fun getInstantiatedMethodType(
                lambdaMethod: java.lang.reflect.Method,
                capturedArgumentTypes: Array<Class<*>>
            ): java.lang.invoke.MethodType {
                // Types of arguments of synthetic method (representing lambda) without the types of captured values.
                val instantiatedMethodParamTypes = lambdaMethod.parameterTypes
                    .drop(capturedArgumentTypes.size)
                    .toTypedArray()
    
                return java.lang.invoke.MethodType.methodType(lambdaMethod.returnType, instantiatedMethodParamTypes)
            }
            """.trimIndent()
    }

private fun getLambdaMethod(language: CodegenLanguage) =
    when (language) {
        CodegenLanguage.JAVA ->
            """
            /**
             * @param declaringClass class where a lambda is declared.
             * @param lambdaName name of synthetic method that represents a lambda.
             * @return {@link java.lang.reflect.Method} instance for the synthetic method that represent a lambda.
             */
            private static java.lang.reflect.Method getLambdaMethod(Class<?> declaringClass, String lambdaName) {
                return java.util.Arrays.stream(declaringClass.getDeclaredMethods())
                        .filter(method -> method.getName().equals(lambdaName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No lambda method named " + lambdaName + " was found in class: " + declaringClass.getCanonicalName()));
            }
            """.trimIndent()
        CodegenLanguage.KOTLIN ->
            """
            /**
             * @param declaringClass class where a lambda is declared.
             * @param lambdaName name of synthetic method that represents a lambda.
             * @return [java.lang.reflect.Method] instance for the synthetic method that represent a lambda.
             */
            private fun getLambdaMethod(declaringClass: Class<*>, lambdaName: String): java.lang.reflect.Method {
                return declaringClass.declaredMethods.firstOrNull { it.name == lambdaName }
                    ?: throw IllegalArgumentException("No lambda method named ${'$'}lambdaName was found in class: ${'$'}{declaringClass.canonicalName}")
            }
            """.trimIndent()
    }

private fun getSingleAbstractMethod(language: CodegenLanguage) =
    when (language) {
        CodegenLanguage.JAVA ->
            """
            private static java.lang.reflect.Method getSingleAbstractMethod(Class<?> clazz) {
                java.util.List<java.lang.reflect.Method> abstractMethods = java.util.Arrays.stream(clazz.getMethods())
                        .filter(method -> java.lang.reflect.Modifier.isAbstract(method.getModifiers()))
                        .collect(java.util.stream.Collectors.toList());
        
                if (abstractMethods.isEmpty()) {
                    throw new IllegalArgumentException("No abstract methods found in class: " + clazz.getCanonicalName());
                }
                if (abstractMethods.size() > 1) {
                    throw new IllegalArgumentException("More than one abstract method found in class: " + clazz.getCanonicalName());
                }
        
                return abstractMethods.get(0);
            }
            """.trimIndent()
        CodegenLanguage.KOTLIN ->
            """
            /**
             * @param clazz functional interface
             * @return a [java.lang.reflect.Method] for the single abstract method of the given functional interface `clazz`.
             */
            private fun getSingleAbstractMethod(clazz: Class<*>): java.lang.reflect.Method {
                val abstractMethods = clazz.methods.filter { java.lang.reflect.Modifier.isAbstract(it.modifiers) }
                require(abstractMethods.isNotEmpty()) { "No abstract methods found in class: " + clazz.canonicalName }
                require(abstractMethods.size <= 1) { "More than one abstract method found in class: " + clazz.canonicalName }
                return abstractMethods[0]
            }
            """.trimIndent()
    }

private fun capturedArgumentClass(language: CodegenLanguage) =
    when (language) {
        CodegenLanguage.JAVA ->
            """
            /**
             * This class represents the {@code type} and {@code value} of a value captured by lambda.
             * Captured values are represented as arguments of a synthetic method that lambda is compiled into,
             * hence the name of the class.
             */
            public static class CapturedArgument {
                private Class<?> type;
                private Object value;
        
                public CapturedArgument(Class<?> type, Object value) {
                    this.type = type;
                    this.value = value;
                }
            }
            """.trimIndent()
        CodegenLanguage.KOTLIN -> {
            """
            /**
             * This class represents the `type` and `value` of a value captured by lambda.
             * Captured values are represented as arguments of a synthetic method that lambda is compiled into,
             * hence the name of the class.
             */
            data class CapturedArgument(val type: Class<*>, val value: Any?)
            """.trimIndent()
        }
    }

internal fun CgContextOwner.importUtilMethodDependencies(id: MethodId) {
    // if util methods come from a separate UtUtils class and not from the test class,
    // then we don't need to import any other methods, hence we return from method
    val utilMethodProvider = utilMethodProvider as? TestClassUtilMethodProvider ?: return
    for (classId in utilMethodProvider.regularImportsByUtilMethod(id, codegenLanguage)) {
        importIfNeeded(classId)
    }
    for (methodId in utilMethodProvider.staticImportsByUtilMethod(id)) {
        collectedImports += StaticImport(methodId.classId.canonicalName, methodId.name)
    }
}

private fun TestClassUtilMethodProvider.regularImportsByUtilMethod(
    id: MethodId,
    codegenLanguage: CodegenLanguage
): List<ClassId> {
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
            CodegenLanguage.JAVA -> listOf(java.util.stream.Stream::class.id, Set::class.id)
            CodegenLanguage.KOTLIN -> emptyList()
        }
        mapsDeepEqualsMethodId -> when (codegenLanguage) {
            CodegenLanguage.JAVA -> listOf(Map::class.id, Iterator::class.id, Set::class.id)
            CodegenLanguage.KOTLIN -> emptyList()
        }
        hasCustomEqualsMethodId -> emptyList()
        getArrayLengthMethodId -> listOf(java.lang.reflect.Array::class.id)
        buildStaticLambdaMethodId -> when (codegenLanguage) {
            CodegenLanguage.JAVA -> listOf(
                MethodHandles::class.id, Method::class.id, MethodType::class.id,
                MethodHandle::class.id, CallSite::class.id, LambdaMetafactory::class.id
            )
            CodegenLanguage.KOTLIN -> listOf(MethodType::class.id, LambdaMetafactory::class.id)
        }
        buildLambdaMethodId -> when (codegenLanguage) {
            CodegenLanguage.JAVA -> listOf(
                MethodHandles::class.id, Method::class.id, MethodType::class.id,
                MethodHandle::class.id, CallSite::class.id, LambdaMetafactory::class.id
            )
            CodegenLanguage.KOTLIN -> listOf(MethodType::class.id, LambdaMetafactory::class.id)
        }
        getLookupInMethodId -> when (codegenLanguage) {
            CodegenLanguage.JAVA -> listOf(MethodHandles::class.id, Field::class.id, Modifier::class.id)
            CodegenLanguage.KOTLIN -> listOf(MethodHandles::class.id, Modifier::class.id)
        }
        getLambdaCapturedArgumentTypesMethodId -> when (codegenLanguage) {
            CodegenLanguage.JAVA -> listOf(LambdaMetafactory::class.id)
            CodegenLanguage.KOTLIN -> listOf(LambdaMetafactory::class.id)
        }
        getLambdaCapturedArgumentValuesMethodId -> when (codegenLanguage) {
            CodegenLanguage.JAVA -> listOf(Arrays::class.id)
            CodegenLanguage.KOTLIN -> emptyList()
        }
        getInstantiatedMethodTypeMethodId -> when (codegenLanguage) {
            CodegenLanguage.JAVA -> listOf(
                Method::class.id, MethodType::class.id, LambdaMetafactory::class.id,
                java.util.List::class.id, Arrays::class.id, Collectors::class.id
            )
            CodegenLanguage.KOTLIN -> listOf(Method::class.id, MethodType::class.id, LambdaMetafactory::class.id)
        }
        getLambdaMethodMethodId -> when (codegenLanguage) {
            CodegenLanguage.JAVA -> listOf(Method::class.id, Arrays::class.id)
            CodegenLanguage.KOTLIN -> listOf(Method::class.id)
        }
        getSingleAbstractMethodMethodId -> listOf(
            Method::class.id, java.util.List::class.id, Arrays::class.id,
            Modifier::class.id, Collectors::class.id
        )
        else -> error("Unknown util method for class $this: $id")
    }
}

// Note: for now always returns an empty list, because no util method
// requires static imports, but this may change in the future
@Suppress("unused", "unused_parameter")
private fun TestClassUtilMethodProvider.staticImportsByUtilMethod(id: MethodId): List<MethodId> = emptyList()
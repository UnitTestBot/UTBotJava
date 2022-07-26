package org.utbot.framework.codegen.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Objects;

public final class UtUtils {
    private UtUtils() {}

    public static Object getEnumConstantByName(Class<?> enumClass, String name) throws IllegalAccessException {
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

    public static Object getStaticFieldValue(Class<?> clazz, String fieldName) throws IllegalAccessException, NoSuchFieldException {
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

    public static Object getFieldValue(Object obj, String fieldName) throws IllegalAccessException, NoSuchFieldException {
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

    public static void setStaticField(Class<?> clazz, String fieldName, Object fieldValue) throws NoSuchFieldException, IllegalAccessException {
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

    public static void setField(Object object, String fieldName, Object fieldValue) throws NoSuchFieldException, IllegalAccessException {
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

    public static Object[] createArray(String className, int length, Object... values) throws ClassNotFoundException {
        Object array = java.lang.reflect.Array.newInstance(Class.forName(className), length);

        for (int i = 0; i < values.length; i++) {
            java.lang.reflect.Array.set(array, i, values[i]);
        }

        return (Object[]) array;
    }

    public static Object createInstance(String className)
            throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException {
        Class<?> clazz = Class.forName(className);
        return Class.forName("sun.misc.Unsafe").getDeclaredMethod("allocateInstance", Class.class)
                .invoke(getUnsafeInstance(), clazz);
    }


    public static Object getUnsafeInstance() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        java.lang.reflect.Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return f.get(null);
    }

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

    public static boolean deepEquals(Object o1, Object o2, boolean mockFrameworkUsed) {
        return deepEquals(o1, o2, new java.util.HashSet<>(), mockFrameworkUsed);
    }

    private static boolean deepEquals(Object o1, Object o2, java.util.Set<FieldsPair> visited, boolean mockFrameworkUsed) {
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

            return iterablesDeepEquals((Iterable<?>) o1, (Iterable<?>) o2, visited, mockFrameworkUsed);
        }

        if (o2 instanceof Iterable) {
            return false;
        }

        if (o1 instanceof java.util.stream.Stream) {
            if (!(o2 instanceof java.util.stream.Stream)) {
                return false;
            }

            return streamsDeepEquals((java.util.stream.Stream<?>) o1, (java.util.stream.Stream<?>) o2, visited, mockFrameworkUsed);
        }

        if (o2 instanceof java.util.stream.Stream) {
            return false;
        }

        if (o1 instanceof java.util.Map) {
            if (!(o2 instanceof java.util.Map)) {
                return false;
            }

            return mapsDeepEquals((java.util.Map<?, ?>) o1, (java.util.Map<?, ?>) o2, visited, mockFrameworkUsed);
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
            return arraysDeepEquals(o1, o2, visited, mockFrameworkUsed);
        }

        // common classes

        // Check if class has custom equals method (including wrappers and strings)
        // It is very important to check it here but not earlier because iterables and maps also have custom equals
        // based on elements equals.
        //
        // Class MockUtils uses Mockito and will only be loaded if mockFrameworkUsed == true.
        // In this case we know that mockito-core is on the runtime classpath, so MockUtils#isMock will work fine.
        // Otherwise, call to MockUtils#isMock will not be performed, so MockUtils class will not be loaded at all.
        if (hasCustomEquals(firstClass) && (!mockFrameworkUsed || !MockUtils.isMock(o1))) {
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
                if (!visited.contains(new FieldsPair(field1, field2)) && !deepEquals(field1, field2, visited, mockFrameworkUsed)) {
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

    public static boolean arraysDeepEquals(Object arr1, Object arr2, boolean mockFrameworkUsed) {
        return arraysDeepEquals(arr1, arr2, new HashSet<>(), mockFrameworkUsed);
    }

    private static boolean arraysDeepEquals(Object arr1, Object arr2, java.util.Set<FieldsPair> visited, boolean mockFrameworkUsed) {
        final int length = java.lang.reflect.Array.getLength(arr1);
        if (length != java.lang.reflect.Array.getLength(arr2)) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            Object item1 = java.lang.reflect.Array.get(arr1, i);
            Object item2 = java.lang.reflect.Array.get(arr2, i);
            if (!deepEquals(item1, item2, visited, mockFrameworkUsed)) {
                return false;
            }
        }

        return true;
    }

    public static boolean iterablesDeepEquals(Iterable<?> i1, Iterable<?> i2, boolean mockFrameworkUsed) {
        return iterablesDeepEquals(i1, i2, new HashSet<>(), mockFrameworkUsed);
    }

    private static boolean iterablesDeepEquals(Iterable<?> i1, Iterable<?> i2, java.util.Set<FieldsPair> visited, boolean mockFrameworkUsed) {
        final java.util.Iterator<?> firstIterator = i1.iterator();
        final java.util.Iterator<?> secondIterator = i2.iterator();
        while (firstIterator.hasNext() && secondIterator.hasNext()) {
            if (!deepEquals(firstIterator.next(), secondIterator.next(), visited, mockFrameworkUsed)) {
                return false;
            }
        }

        if (firstIterator.hasNext()) {
            return false;
        }

        return !secondIterator.hasNext();
    }

    public static boolean streamsDeepEquals(
            java.util.stream.Stream<?> s1,
            java.util.stream.Stream<?> s2,
            boolean mockFrameworkUsed
    ) {
        return streamsDeepEquals(s1, s2, new HashSet<>(), mockFrameworkUsed);
    }

    private static boolean streamsDeepEquals(
            java.util.stream.Stream<?> s1,
            java.util.stream.Stream<?> s2,
            java.util.Set<FieldsPair> visited,
            boolean mockFrameworkUsed
    ) {
        final java.util.Iterator<?> firstIterator = s1.iterator();
        final java.util.Iterator<?> secondIterator = s2.iterator();
        while (firstIterator.hasNext() && secondIterator.hasNext()) {
            if (!deepEquals(firstIterator.next(), secondIterator.next(), visited, mockFrameworkUsed)) {
                return false;
            }
        }

        if (firstIterator.hasNext()) {
            return false;
        }

        return !secondIterator.hasNext();
    }

    public static boolean mapsDeepEquals(
            java.util.Map<?, ?> m1,
            java.util.Map<?, ?> m2,
            boolean mockFrameworkUsed
    ) {
        return mapsDeepEquals(m1, m2, new HashSet<>(), mockFrameworkUsed);
    }

    private static boolean mapsDeepEquals(
            java.util.Map<?, ?> m1,
            java.util.Map<?, ?> m2,
            java.util.Set<FieldsPair> visited,
            boolean mockFrameworkUsed
    ) {
        final java.util.Iterator<? extends java.util.Map.Entry<?, ?>> firstIterator = m1.entrySet().iterator();
        final java.util.Iterator<? extends java.util.Map.Entry<?, ?>> secondIterator = m2.entrySet().iterator();
        while (firstIterator.hasNext() && secondIterator.hasNext()) {
            final java.util.Map.Entry<?, ?> firstEntry = firstIterator.next();
            final java.util.Map.Entry<?, ?> secondEntry = secondIterator.next();

            if (!deepEquals(firstEntry.getKey(), secondEntry.getKey(), visited, mockFrameworkUsed)) {
                return false;
            }

            if (!deepEquals(firstEntry.getValue(), secondEntry.getValue(), visited, mockFrameworkUsed)) {
                return false;
            }
        }

        if (firstIterator.hasNext()) {
            return false;
        }

        return !secondIterator.hasNext();
    }

    public static boolean hasCustomEquals(Class<?> clazz) {
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

    public static int getArrayLength(Object arr) {
        return java.lang.reflect.Array.getLength(arr);
    }
}
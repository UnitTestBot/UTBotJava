package org.utbot.fuzzer.baseline.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 *
 */
public class ValueGen {
    private final static Random rnd = new Random();

    private Type type;
    private TypeVariable<Method>[] methodTypeParameters;
    private Type[] methodTypeValues;

    private final String LIST_TYPE = "java.util.List";
    private final String ARRAY_LIST_TYPE = "java.util.ArrayList";
    private final String LINKED_LIST_TYPE = "java.util.LinkedList";

    private final Set<String> LIST_TYPES = new HashSet<>(
            Arrays.asList(
                    LIST_TYPE,
                    ARRAY_LIST_TYPE,
                    LINKED_LIST_TYPE
            )
    );

    public ValueGen(Type type, TypeVariable<Method>[] methodTypeParameters,
                    Type[] methodTypeValues) {
        this.type = type;
        this.methodTypeParameters = methodTypeParameters;
        this.methodTypeValues = methodTypeValues;
    }

    private Object genPrimitive(Class<?> clazz) {
        String dataType = getTypeName(clazz);
        switch (dataType) {
            case "boolean":
            case "java.lang.Boolean":
                return rnd.nextInt(2) == 1;
            case "byte":
            case "java.lang.Byte":
                return (byte) (-128 + rnd.nextInt(256));
            case "char":
            case "java.lang.Character":
                return (char) (rnd.nextInt(127 - 32) + 32);
            case "double":
            case "java.lang.Double":
                return rnd.nextDouble();
            case "float":
            case "java.lang.Float":
                return rnd.nextFloat();
            case "int":
            case "java.lang.Integer":
                return rnd.nextInt();
            case "long":
            case "java.lang.Long":
                return rnd.nextLong();
            case "short":
            case "java.lang.Short":
                return (short) (-32768 + rnd.nextInt(65536));
            default:
                return null;
        }
    }

    private String genString() {
        int len = Util.rndRange(1, 4);
        String str = "";
        for (int i = 0; i < len; i++) {
            char rndChar = (char) (rnd.nextInt(123 - 97) + 97);
            str += rndChar;
        }
        return str;
    }

//    TODO: rewrite so that it can be used with our plugin's API
    /*private String genArrayInitializer(Type t) throws IOException, ClassNotFoundException {
        boolean noBraces = false;
        int elementsCount = Util.rndRange(1, 3);
        Type subtype;
        if (t instanceof GenericArrayType) {
            // e.g. t is "List<String>[][]" or "List<String>[]"
            GenericArrayType gat = (GenericArrayType) t;
            // e.g. subtype is "List<String>[]" or "List<String>" respectively
            subtype = gat.getGenericComponentType();
        } else {
            // e.g. t is "String[][]" or "int[]"
            Class<?> c = (Class<?>) t;
            // e.g. subtype is "String[]" or "int"
            subtype = c.getComponentType();
        }
        List<String> values = new LinkedList<String>();
        for (int i = 0; i < elementsCount; i++) {
            values.add(genValue(subtype));
        }
        String result = String.join(", ", values);
        if (noBraces) {
            return result;
        }
        return "{ " + result + " }";
    }*/

    private boolean isListType(String typeName) {
        return LIST_TYPES.contains(typeName);
    }

    private List<Object> emptyList(String listTypeName) {
        switch (listTypeName) {
            case LINKED_LIST_TYPE:
                return new LinkedList<>();
            case LIST_TYPE:
            case ARRAY_LIST_TYPE:
                return new ArrayList<>();
            default:
                throw new RuntimeException("Unknown list type: " + listTypeName);
        }
    }

    // "Class<String>, Type"
    private boolean isType(ParameterizedType pt) {
        Type baseType = pt.getRawType();
        if (baseType == null) {
            return false;
        }
        String baseTypeName = getTypeName(baseType);
        return baseTypeName.equals("java.lang.reflect.Type")
                || baseTypeName.equals("java.lang.Class");
    }

//    TODO: rewrite so that it can be used with our plugin's API
    // "Class<String>, Type"
//    private String genValueForType(ParameterizedType pt) {
//        Type baseType = pt.getRawType();
//        if (baseType == null) {
//            return "?";
//        }
//        String baseTypeName = getTypeName(baseType);
//        String className = "?";
//        if (baseTypeName.equals("java.lang.reflect.Type")) {
//            className = "java.lang.Object";
//        } else if (baseTypeName.equals("java.lang.Class")) {
//            className = getTypeName(pt.getActualTypeArguments()[0]);
//        }
//        return "(" + getTypeName(pt) + ") java.lang.Class.forName(\"" + className + "\")";
//    }

//    TODO: rewrite so that it can be used with our plugin's API
//    @SneakyThrows
//    private String genArrayValue(Type t) {
//        if (t instanceof GenericArrayType) {
//            int arrayDepth = Util.getArrayDepth(t);
//            Type childType = Util.getArrayChildType(t);
//            Type rawType = childType;
//            if (childType instanceof ParameterizedType) {
//                rawType = ((ParameterizedType)childType).getRawType();
//            }
//            String castingPart = "(" + getTypeName(t) + ") ";
//            String mainPart = "new " + getTypeName(rawType) + Util.repeat("[]", arrayDepth);
//            return castingPart + mainPart + genArrayInitializer(t);
//        }
//        return "new " + getTypeName(t) + genArrayInitializer(t);
//    }

    private Constructor<?> getPublicConstructor(Class<?> c) {
        Constructor<?>[] constructorArray = c.getConstructors();
        for (int i = 0; i < constructorArray.length; i++) {
            int modifiers = constructorArray[i].getModifiers();
            if (Modifier.isPublic(modifiers)) {
                return constructorArray[i];
            }
        }
        return null;
    }


    private Object genClassValue(Class<?> c) {
        if (c.isInterface()) {
            return null; // interface cannot be instantiated
        }
        if (Modifier.isAbstract(c.getModifiers())) {
            return null; //abstract classes cannot be instantiated
        }
        List<Object> arguments = new LinkedList<>();
        Constructor<?> ctor = getPublicConstructor(c);
        if (ctor == null) {
            return null; //no public constructor available
        }
        TypeVariable[] typeParameters = ctor.getTypeParameters();
        Type[] typeValues = TypeChooser.chooseTypeParameterValues(typeParameters);
        int parCnt = ctor.getParameterCount();
        for (int i = 0; i < parCnt; i++) {
            Type parType = ctor.getGenericParameterTypes()[i];
            ValueGen gen = new ValueGen(parType, typeParameters, typeValues);
            Object value = gen.generate();
            arguments.add(value);
        }
        try {
            return ctor.newInstance(arguments.toArray());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Could not instantiate class " + c.getName());
        }
    }

    private boolean isPrimitive(Class<?> c) {
        return c.isPrimitive() ||
                c.getTypeName().equals("java.lang.Short") ||
                c.getTypeName().equals("java.lang.Boolean") ||
                c.getTypeName().equals("java.lang.Byte") ||
                c.getTypeName().equals("java.lang.Character") ||
                c.getTypeName().equals("java.lang.Double") ||
                c.getTypeName().equals("java.lang.Float") ||
                c.getTypeName().equals("java.lang.Integer") ||
                c.getTypeName().equals("java.lang.Long") ||
                c.getTypeName().equals("java.lang.Short");
    }

    private Object genValue(Type t) {
        if (t instanceof TypeVariable) {
            // e.g. T for: "<T> void foo(List<T> some) {}"
            TypeVariable tv = (TypeVariable) t;
            int typeIndex = Util.findType(methodTypeParameters, tv);
            return genValue(methodTypeValues[typeIndex]);
        } else if (t instanceof ParameterizedType) {
            // e.g. List<Integer>, Foo<Object>"
            ParameterizedType pt = (ParameterizedType) t;
            Type rawType = pt.getRawType();
            String rawTypeName = getTypeName(rawType);
            if (isListType(rawTypeName)) {
                Type childType = pt.getActualTypeArguments()[0];
                List<Object> list = emptyList(rawTypeName);
                int elemCnt = Util.rndRange(1, 3);
                for (int i = 0; i < elemCnt; i++) {
                    list.add(genValue(childType));
                }
                return list;
            }
            if (isType(pt)) {
                // TODO
                return null; // genValueForType(pt);
            }
            Type theRawType = pt.getRawType();
            if (theRawType instanceof Class<?>) {
                Class<?> cc = (Class<?>) theRawType;
                Constructor<?> pc = getPublicConstructor(cc);
                if (pc == null || cc.isInterface() || Modifier.isAbstract(cc.getModifiers())) {
                    return null; // abstract generic class/interface cannot be instantiated
                }
                // TODO
                return null; // genClassValue(cc, true);
            }
            return null; // failed to instantiate complex generic class or interface
        } else if (t instanceof GenericArrayType) {
            // TODO
            return null; // genArrayValue(t);
        } else if (t instanceof Class<?>) {
            Class<?> c = (Class<?>) t;
            if (isPrimitive(c)) {
                if (getTypeName(c).equals("void")) {
                    throw new RuntimeException("Cannot generate value for void-type");
                }
                return genPrimitive(c);
            }
            if (getTypeName(c).equals("java.lang.String")) {
                return genString();
            }
            if (c.isArray()) {
                // It's important to use "t" here, because array can be "List<Integer>[]",
                // which is GenericArrayType
                // TODO
                return null; //genArrayValue(t);
            }
            // Usual class
            return genClassValue(c);
        }
        return null; // unknown instance type
    }

    public Object generate() {
        return genValue(type);
    }

    private String getTypeName(Type t) {
        return Util.getTypeName(t, methodTypeParameters, methodTypeValues);
    }
}

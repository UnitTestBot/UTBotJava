package org.utbot.examples.types;

import java.util.Map;

public class Generics {
    public boolean genericAsField(CollectionAsField<String> object) {
        if (object != null && object.field != null) {
            return object.field.equals("abc");
        }

        return false;
    }

    public String mapAsStaticField() {
        CollectionAsField.staticMap.put("key", "value");
        return CollectionAsField.staticMap.get("key");
    }

    public String mapAsParameter(Map<String, String> map) {
        map.put("key", "value");
        return map.get("key");
    }

    public String mapAsNonStaticField(CollectionAsField<String> object) {
        object.nonStaticMap.put("key", "value");
        return object.nonStaticMap.get("key");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public String methodWithRawType(Map map) {
        nestedMethodWithGenericInfo(map);
        map.put("key", "value");

        return (String) map.get("key");
    }

    @SuppressWarnings("UnusedReturnValue")
    private Map<String, String> nestedMethodWithGenericInfo(Map<String, String> map) {
        return map;
    }
}

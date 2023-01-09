package org.utbot.examples.types;

import java.util.HashMap;
import java.util.Map;

public class CollectionAsField<T> {
    public static Map<String, String> staticMap = new HashMap<>();
    public Map<String, String> nonStaticMap = new HashMap<>();
    public T field;
}

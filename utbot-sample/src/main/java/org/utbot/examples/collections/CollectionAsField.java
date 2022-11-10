package org.utbot.examples.collections;

import java.util.HashMap;
import java.util.Map;

public class CollectionAsField {
    public static Map<String, String> staticMap = new HashMap<>();
    public Map<String, String> nonStaticMap = new HashMap<>();
}

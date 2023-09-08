package org.utbot.fuzzing.samples;

import java.util.*;

public class StringList extends ArrayList<String> {
    public static StringList create() {
        return new StringList();
    }

    public static List<String> createAndUpcast() {
        return new StringList();
    }

    public static List<StringList> createListOfLists() {
        return new ArrayList<>();
    }

    public static List<List<String>> createListOfUpcastedLists() {
        return new ArrayList<>();
    }

    public static List<? extends List<? extends String>> createReadOnlyListOfReadOnlyLists() {
        return new ArrayList<>();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> List<? extends List<T>> createListOfParametrizedLists(Optional<? extends T> elm) {
        return Collections.singletonList(Collections.singletonList(elm.orElse(null)));
    }
}

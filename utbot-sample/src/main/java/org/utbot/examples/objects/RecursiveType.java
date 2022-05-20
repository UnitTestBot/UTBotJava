package org.utbot.examples.objects;

public class RecursiveType {
    public RecursiveTypeClass nextValue(RecursiveTypeClass node, int value) {
        if (value == 0) {
            throw new IllegalArgumentException();
        }
        if (node.next.value == value) {
            return node.next;
        }
        return null;
    }

    public RecursiveTypeClass writeObjectField(RecursiveTypeClass node) {
        if (node.next == null) {
            node.next = new RecursiveTypeClass();
        }
        node.next.value = node.next.value + 1;
        return node;
    }
}

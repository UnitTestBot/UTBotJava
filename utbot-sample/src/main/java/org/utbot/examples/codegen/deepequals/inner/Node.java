package org.utbot.examples.codegen.deepequals.inner;

public class Node {
    public Node next;
    public int value;

    static int staticField;

    public Node(Node next, int value) {
        this.next = next;
        this.value = value;
    }

    public Node(int value) {
        this(null, value);
    }
}

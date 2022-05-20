package org.utbot.examples.assemble;

/**
 * A class that simulates an item in a list.
 */
public class ListItem {
    private int value;
    private ListItem next;

    public ListItem() {
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setNext(ListItem next) {
        this.next = next;
    }
}

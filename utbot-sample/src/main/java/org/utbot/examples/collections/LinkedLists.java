package org.utbot.examples.collections;

import java.util.LinkedList;

public class LinkedLists {
    public int set(LinkedList<Integer> list) {
        list.set(2, 1);
        return list.get(2);
    }

    public int peek(LinkedList<Integer> list) {
        int a = list.peek();
        return a;
    }

    public int peekFirst(LinkedList<Integer> list) {
        int a = list.peekFirst();
        return a;
    }

    public int getFirst(LinkedList<Integer> list) {
        int a = list.getFirst();
        return a;
    }

    public int element(LinkedList<Integer> list) {
        int a = list.element();
        return a;
    }

    public int peekLast(LinkedList<Integer> list) {
        int a = list.peekLast();
        return a;
    }

    public int getLast(LinkedList<Integer> list) {
        int a = list.getLast();
        return a;
    }

    public LinkedList<Integer> offer(LinkedList<Integer> list) {
        if (list.size() > 1) {
            list.offer(1);
        }
        return list;
    }


    public LinkedList<Integer> offerFirst(LinkedList<Integer> list) {
        if (list.size() > 1) {
            list.offerFirst(1);
        }
        return list;
    }


    public LinkedList<Integer> offerLast(LinkedList<Integer> list) {
        if (list.size() > 1) {
            list.offerLast(1);
        }
        return list;
    }

    public LinkedList<Integer> addLast(LinkedList<Integer> list) {
        if (list.size() > 1) {
            list.addLast(1);
        }
        return list;
    }

    public LinkedList<Integer> addFirst(LinkedList<Integer> list) {
        if (list.size() > 1) {
            list.addFirst(1);
        }
        return list;
    }

    public LinkedList<Integer> push(LinkedList<Integer> list) {
        if (list.size() > 1) {
            list.push(1);
        }
        return list;
    }

    public LinkedList<Integer> poll(LinkedList<Integer> list) {
        if (list.size() > 1) {
            int a = list.poll();
        } else if (list.isEmpty()) {
            int a = list.poll();
        }
        return list;
    }

    public LinkedList<Integer> pollFirst(LinkedList<Integer> list) {
        if (list.size() > 1) {
            int a = list.pollFirst();
        } else if (list.isEmpty()) {
            int a = list.pollFirst();
        }
        return list;
    }

    public LinkedList<Integer> pollLast(LinkedList<Integer> list) {
        if (list.size() > 1) {
            int a = list.pollLast();
        } else if (list.isEmpty()) {
            int a = list.pollLast();
        }
        return list;
    }

    public LinkedList<Integer> remove(LinkedList<Integer> list) {
        if (list.size() > 1) {
            int a = list.remove();
        } else if (list.isEmpty()) {
            int a = list.remove();
        }
        return list;
    }

    public LinkedList<Integer> removeFirst(LinkedList<Integer> list) {
        if (list.size() > 1) {
            int a = list.removeFirst();
        } else if (list.isEmpty()) {
            int a = list.removeFirst();
        }
        return list;
    }

    public LinkedList<Integer> removeLast(LinkedList<Integer> list) {
        if (list.size() > 1) {
            int a = list.removeLast();
        } else if (list.isEmpty()) {
            int a = list.removeLast();
        }
        return list;
    }
}

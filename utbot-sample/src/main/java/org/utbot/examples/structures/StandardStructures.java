package org.utbot.examples.structures;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StandardStructures {
    public List<Integer> getList(List<Integer> list) {
        if (list instanceof ArrayList) {
            return list;
        }

        if (list instanceof LinkedList) {
            return list;
        }

        if (list == null) {
            return null;
        }

        return list;
    }

    public Map<Integer, Integer> getMap(Map<Integer, Integer> map) {
        if (map instanceof TreeMap) {
            return map;
        }

        if (map == null) {
            return null;
        }

        return map;
    }

    public Deque<Integer> getDeque(Deque<Integer> deque) {
        if (deque instanceof ArrayDeque) {
            return deque;
        }

        if (deque instanceof LinkedList) {
            return deque;
        }

        if (deque == null) {
            return null;
        }

        return deque;
    }
}
package org.utbot.examples.codegen.deepequals;

import org.utbot.api.mock.UtMock;
import org.utbot.examples.codegen.deepequals.inner.GraphNode;
import org.utbot.examples.codegen.deepequals.inner.Node;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Very simple class, used only for testing codegen deep equals
public class DeepEqualsTestingClass {
    int x = 1;
    int y = 2;

    List<DeepEqualsTestingClass> returnList() {
        List<DeepEqualsTestingClass> list = new ArrayList<>();
        list.add(new DeepEqualsTestingClass());
        list.add(new DeepEqualsTestingClass());

        return list;
    }

    Set<DeepEqualsTestingClass> returnSet() {
        Set<DeepEqualsTestingClass> set = new HashSet<>();
        set.add(new DeepEqualsTestingClass());
        set.add(new DeepEqualsTestingClass());

        return set;
    }

    Map<Integer, DeepEqualsTestingClass> returnMap() {
        Map<Integer, DeepEqualsTestingClass> map = new HashMap<>();
        map.put(0, new DeepEqualsTestingClass());
        map.put(1, new DeepEqualsTestingClass());

        return map;
    }

    DeepEqualsTestingClass[] returnArray() {
        DeepEqualsTestingClass[] array = new DeepEqualsTestingClass[2];
        array[0] = new DeepEqualsTestingClass();
        array[1] = new DeepEqualsTestingClass();

        return array;
    }

    List<List<DeepEqualsTestingClass>> return2DList() {
        List<List<DeepEqualsTestingClass>> list = new ArrayList<>();
        list.add(returnList());
        list.add(returnList());

        return list;
    }

    Set<Set<DeepEqualsTestingClass>> return2DSet() {
        Set<Set<DeepEqualsTestingClass>> set = new HashSet<>();
        set.add(returnSet());
        set.add(returnSet());

        return set;
    }

    Map<Integer, Map<Integer, DeepEqualsTestingClass>> return2DMap() {
        Map<Integer, Map<Integer, DeepEqualsTestingClass>> map = new HashMap<>();
        map.put(0, returnMap());
        map.put(1, returnMap());

        return map;
    }

    DeepEqualsTestingClass[][] return2DArray() {
        DeepEqualsTestingClass[][] array = new DeepEqualsTestingClass[2][2];
        array[0] = returnArray();
        array[1] = returnArray();

        return array;
    }

    DeepEqualsTestingClass returnCommonClass() {
        return new DeepEqualsTestingClass();
    }

    // just for easy testing 2d lists
    List<List<Integer>> returnIntegers2DList() {
        List<List<Integer>> lists = new ArrayList<>();

        List<Integer> list1 = new ArrayList<>();
        list1.add(1);

        List<Integer> list2 = new ArrayList<>();
        list1.add(1);

        lists.add(list1);
        lists.add(list2);

        return lists;
    }

    Node returnTriangle(int firstValue, int secondValue) {
        Node first = new Node(firstValue);
        Node second = new Node(secondValue);
        Node third = new Node(firstValue + secondValue);

        first.next = second;
        second.next = third;
        third.next = first;

        return first;
    }

    // 3 -> 4 -> |
    // 1 -> 2    |
    // |         |
    // <- <- <-<-
    GraphNode returnQuadrilateralFromNode(GraphNode first) {
        UtMock.assume(first != null);
        UtMock.assume(first.nextNodes != null);
        UtMock.assume(first.nextNodes.isEmpty());
        GraphNode second = new GraphNode(2);
        GraphNode third = new GraphNode(3);
        GraphNode fourth = new GraphNode(Collections.singletonList(first), 4);

        first.nextNodes.add(second);
        first.nextNodes.add(third);
        second.nextNodes.add(fourth);
        third.nextNodes.add(fourth);

        return first;
    }

    public int[][][] fillIntMultiArrayWithConstValue(int length, int value) {
        UtMock.assume(length == 0 || length == 2);
        if (length <= 0) {
            return null;
        }

        int[][][] array = new int[length][length][length];
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                for (int k = 0; k < length; k++) {
                    array[i][j][k] = value;
                }
            }
        }

        if (array[0][1][0] == 10) {
            array[1][0][1] = 12;
        }

        return array;
    }

    public double[][][] fillDoubleMultiArrayWithConstValue(int length, double value) {
        UtMock.assume(length == 0 || length == 2);
        if (length <= 0) {
            return null;
        }

        double[][][] array = new double[length][length][length];
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                for (int k = 0; k < length; k++) {
                    array[i][j][k] = value;
                }
            }
        }

        if (array[0][1][0] == 10) {
            array[1][0][1] = 12;
        }

        return array;
    }

    public Object[][][] fillIntegerWrapperMultiArrayWithConstValue(int length, int value) {
        UtMock.assume(length == 0 || length == 2);
        if (length <= 0) {
            return null;
        }

        Integer[][][] array = new Integer[length][length][length];
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                for (int k = 0; k < length; k++) {
                    array[i][j][k] = value;
                }
            }
        }

        if (array[0][1][0] == 10) {
            array[1][0][1] = 12;
        }

        return array;
    }

    public Object[][][] fillDoubleWrapperMultiArrayWithConstValue(int length, double value) {
        UtMock.assume(length == 0 || length == 2);
        if (length <= 0) {
            return null;
        }

        Double[][][] array = new Double[length][length][length];
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                for (int k = 0; k < length; k++) {
                    array[i][j][k] = value;
                }
            }
        }

        if (array[0][1][0] == 10) {
            array[1][0][1] = 12.0;
        }

        return array;
    }
}

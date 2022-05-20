package org.utbot.examples.codegen.deepequals.inner;

import java.util.ArrayList;
import java.util.List;

public class GraphNode {
    public List<GraphNode> nextNodes;
    public int value;

    static int staticField;

    public GraphNode(List<GraphNode> nextNodes, int value) {
        this.nextNodes = nextNodes;
        this.value = value;
    }

    public GraphNode(int value) {
        this(new ArrayList<>(), value);
    }
}

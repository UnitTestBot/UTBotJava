package org.utbot.examples.algorithms;

import org.utbot.examples.structures.Pair;
import java.util.Arrays;

public class GraphExample {
    public boolean runFindCycle(Pair[] additionalEdges) {
        Graph graph = new Graph();

        int size = 10;
        graph.setSize(10);
        graph.setAdj(new int[size][size]);

        for (int i = 0; i < size; i++) {
            Arrays.fill(graph.getAdj()[i], 0);
        }

        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 2);
        graph.addEdge(2, 0);
        graph.addEdge(2, 3);
        graph.addEdge(3, 3);

        for (Pair additionalEdge : additionalEdges) {
            graph.addEdge((int) additionalEdge.getFirst(), (int) additionalEdge.getSecond());
        }


        return graph.isCyclic();
    }


    private int minDistance(int[] dist, boolean[] sptSet, int size) {
        int min = 0x7fffffff, min_index = -1; // INT.MAX_VALUE

        for (int v = 0; v < size; v++) {
            if (!sptSet[v] && dist[v] <= min) {
                min = dist[v];
                min_index = v;
            }
        }

        return min_index;
    }

    private int[] dijkstra(int[][] graph, int src, int size) {
        int[] dist = new int[size];

        boolean[] sptSet = new boolean[size];

        for (int i = 0; i < size; i++) {
            dist[i] = 0x7fffffff;
            sptSet[i] = false;
        }

        dist[src] = 0;

        for (int count = 0; count < size - 1; count++) {
            int u = minDistance(dist, sptSet, size);
            sptSet[u] = true;

            for (int v = 0; v < size; v++) {
                if (!sptSet[v] && graph[u][v] != 0 && dist[u] != 0x7fffffff && dist[u] + graph[u][v] < dist[v]) {
                    dist[v] = dist[u] + graph[u][v];
                }
            }
        }

        return dist;
    }

    public int[] runDijkstra(int ignored) {
        int[][] graph = new int[][]{{0, 4, 0, 0, 0, 0, 0, 8, 0},
                {4, 0, 8, 0, 0, 0, 0, 11, 0},
                {0, 8, 0, 7, 0, 4, 0, 0, 2},
                {0, 0, 7, 0, 9, 14, 0, 0, 0},
                {0, 0, 0, 9, 0, 10, 0, 0, 0},
                {0, 0, 4, 14, 10, 0, 2, 0, 0},
                {0, 0, 0, 0, 0, 2, 0, 1, 6},
                {8, 11, 0, 0, 0, 0, 1, 0, 7},
                {0, 0, 2, 0, 0, 0, 6, 7, 0}};
        return dijkstra(graph, 0, 9);
    }

    public int[] runDijkstraWithParameter(int[][] graph) {
        return dijkstra(graph, 0, graph.length);
    }
}

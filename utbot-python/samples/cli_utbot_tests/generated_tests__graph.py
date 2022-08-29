import sys
sys.path.append('samples')
import unittest
import builtins
import graph
import copyreg
import types


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable graph.bfs
    # region
    def test_bfs(self):
        actual = graph.bfs([graph.Node(str(1e+300 * 1e+300), []), graph.Node(str(id), []), graph.Node(str('unicode remains unicode'), [])])
        
        node = copyreg._reconstructor(graph.Node, builtins.object, None)
        node.name = 'unicode remains unicode'
        node.children = []
        node1 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node1.name = '<built-in function id>'
        node1.children = []
        node2 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node2.name = 'inf'
        node2.children = []
        self.assertEqual([node, node1, node2], actual)
    
    def test_bfs1(self):
        actual = graph.bfs([])
        
        self.assertEqual([], actual)
    
    # endregion
    
    # endregion
    


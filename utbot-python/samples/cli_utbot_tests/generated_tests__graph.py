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
        actual = graph.bfs([graph.Node(str(-1234567890), []), graph.Node(str('unicode remains unicode'), [])])
        
        node = copyreg._reconstructor(graph.Node, builtins.object, None)
        node.name = 'unicode remains unicode'
        node.children = []
        node1 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node1.name = '-1234567890'
        node1.children = []
        self.assertEqual([node, node1], actual)
    
    def test_bfs1(self):
        actual = graph.bfs([])
        
        self.assertEqual([], actual)
    
    # endregion
    
    # endregion
    


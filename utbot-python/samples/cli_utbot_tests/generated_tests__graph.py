import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import builtins
import graph
import copyreg
import types


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable graph.bfs
    # region
    def test_bfs(self):
        actual = graph.bfs([graph.Node(str(-1234567890)), graph.Node(str(-1234567890)), graph.Node(str(1e+300 * 1e+300))])
        
        node = copyreg._reconstructor(graph.Node, builtins.object, None)
        node.name = 'inf'
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
    


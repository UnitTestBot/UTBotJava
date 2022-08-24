import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import builtins
import samples.graph
import copyreg
import types


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable samples.graph.bfs
    # region
    def test_bfs(self):
        actual = samples.graph.bfs([samples.graph.Node(str(float('-nan'))), samples.graph.Node(str(b'xn--pythn-mua.org', 'idna')), samples.graph.Node(str(float('-nan')))])
        
        node = copyreg._reconstructor(samples.graph.Node, builtins.object, None)
        node.name = 'nan'
        node.children = []
        node1 = copyreg._reconstructor(samples.graph.Node, builtins.object, None)
        node1.name = 'pythön.org'
        node1.children = []
        self.assertEqual([node, node1], actual)
    
    def test_bfs1(self):
        actual = samples.graph.bfs([])
        
        self.assertEqual([], actual)
    
    # endregion
    
    # endregion
    


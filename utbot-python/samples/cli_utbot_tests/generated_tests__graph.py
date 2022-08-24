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
        actual = graph.bfs([graph.Node(str(b'\xf0\xa3\x91\x96', 'utf-8'), [graph.Node(str(-123456789), []), graph.Node(str('unicode remains unicode'), []), graph.Node(str(), []), graph.Node(str(1e+300 * 1e+300), []), graph.Node(str(1.5 + 3.5j), []), graph.Node(str(b'\xf0\xa3\x91\x96', 'utf-8'), [])]), graph.Node(str(-123456789), [graph.Node(str(-1234567890), []), graph.Node(str(), []), graph.Node(str(1e+300 * 1e+300), []), graph.Node(str(-123456789), []), graph.Node(str(-123456789), [])]), graph.Node(str(id), []), graph.Node(str(b'\x80'), [graph.Node(str(-123456789), []), graph.Node(str('unicode remains unicode'), []), graph.Node(str(), []), graph.Node(str(1e+300 * 1e+300), []), graph.Node(str(1.5 + 3.5j), []), graph.Node(str(b'\xf0\xa3\x91\x96', 'utf-8'), [])]), graph.Node(str('unicode remains unicode'), [graph.Node(str(b'\x80'), []), graph.Node(str(b'\x80'), []), graph.Node(str(), []), graph.Node(str('unicode remains unicode'), []), graph.Node(str(1.5 + 3.5j), [])]), graph.Node(str(b'\xf0\xa3\x91\x96', 'utf-8'), [graph.Node(str(-123456789), []), graph.Node(str('unicode remains unicode'), []), graph.Node(str(), []), graph.Node(str(1e+300 * 1e+300), []), graph.Node(str(1.5 + 3.5j), []), graph.Node(str(b'\xf0\xa3\x91\x96', 'utf-8'), [])]), graph.Node(str(id), [graph.Node(str(b'\xf0\xa3\x91\x96', 'utf-8'), []), graph.Node(str(-1234567890), []), graph.Node(str(id), []), graph.Node(str(), []), graph.Node(str(), []), graph.Node(str(-123456789), []), graph.Node(str(), [])]), graph.Node(str(b'\x80'), [graph.Node(str(-1234567890), []), graph.Node(str(), []), graph.Node(str(1e+300 * 1e+300), []), graph.Node(str(-123456789), []), graph.Node(str(-123456789), [])])])
        
        node = copyreg._reconstructor(graph.Node, builtins.object, None)
        node1 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node1.name = '-1234567890'
        node1.children = []
        node2 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node2.name = ''
        node2.children = []
        node3 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node3.name = 'inf'
        node3.children = []
        node4 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node4.name = '-123456789'
        node4.children = []
        node5 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node5.name = '-123456789'
        node5.children = []
        node.name = "b'\\x80'"
        node.children = [node1, node2, node3, node4, node5]
        node6 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node7 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node7.name = '𣑖'
        node7.children = []
        node8 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node8.name = '-1234567890'
        node8.children = []
        node9 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node9.name = '<built-in function id>'
        node9.children = []
        node10 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node10.name = ''
        node10.children = []
        node11 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node11.name = ''
        node11.children = []
        node12 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node12.name = '-123456789'
        node12.children = []
        node13 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node13.name = ''
        node13.children = []
        node6.name = '<built-in function id>'
        node6.children = [node7, node8, node9, node10, node11, node12, node13]
        node14 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node15 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node15.name = "b'\\x80'"
        node15.children = []
        node16 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node16.name = "b'\\x80'"
        node16.children = []
        node17 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node17.name = ''
        node17.children = []
        node18 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node18.name = 'unicode remains unicode'
        node18.children = []
        node19 = copyreg._reconstructor(graph.Node, builtins.object, None)
        node19.name = '(1.5+3.5j)'
        node19.children = []
        node14.name = 'unicode remains unicode'
        node14.children = [node15, node16, node17, node18, node19]
        self.assertEqual([node, node5, node3, node2, node1, node6, node7, node14, node19], actual)
    
    def test_bfs1(self):
        actual = graph.bfs([])
        
        self.assertEqual([], actual)
    
    # endregion
    
    # endregion
    


import sys
sys.path.append('samples')
import unittest
import builtins
import using_collections
import collections


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable using_collections.generate_collections
    # region
    def test_generate_collections(self):
        actual = using_collections.generate_collections({})
        
        counter = collections.Counter({0: 100, })
        self.assertEqual([{0: 100, }, counter, [(0, 100)]], actual)
    
    # endregion
    
    # endregion
    


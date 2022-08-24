import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import builtins
import samples.using_collections
import collections


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable samples.using_collections.generate_collections
    # region
    def test_generate_collections(self):
        actual = samples.using_collections.generate_collections({})
        
        counter = collections.Counter({0: 100, })
        self.assertEqual([{0: 100, }, counter, [(0, 100)]], actual)
    
    # endregion
    
    # endregion
    


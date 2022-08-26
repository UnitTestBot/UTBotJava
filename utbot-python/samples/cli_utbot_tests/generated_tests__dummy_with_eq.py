import sys
sys.path.append('/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import dummy_with_eq
import builtins
import copyreg
import types


class TestDummy(unittest.TestCase):
    # region Test suites for executable dummy_with_eq.propagate
    # region
    def test_propagate(self):
        dummy = dummy_with_eq.Dummy(1)
        
        actual = dummy.propagate()
        
        dummy1 = copyreg._reconstructor(dummy_with_eq.Dummy, builtins.object, None)
        dummy1.field = 1
        self.assertEqual([dummy1, dummy1], actual)
    
    # endregion
    
    # endregion
    


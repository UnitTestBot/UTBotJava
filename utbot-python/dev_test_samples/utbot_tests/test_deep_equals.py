import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import builtins
import deep_equals


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable deep_equals.primitive_set
    # region
    def test_primitive_set(self):
        actual = deep_equals.primitive_set(int('100000001', 16))
        
        self.assertEqual({4294967297, 4294967298, 4294967299, 4294967300, 4294967301}, actual)
    
    # endregion
    
    # endregion
    


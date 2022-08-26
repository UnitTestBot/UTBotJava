import sys
sys.path.append('/home/tochilinak/Documents/projects/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import dummy_without_eq
import builtins
import copyreg
import types


class TestDummy(unittest.TestCase):
    # region Test suites for executable dummy_without_eq.propagate
    # region
    def test_propagate(self):
        dummy = dummy_without_eq.Dummy()
        
        actual = dummy.propagate()
        
        dummy1 = copyreg._reconstructor(dummy_without_eq.Dummy, builtins.object, None)
        expected_list = [dummy1, dummy1]
        expected_length = len(expected_list)
        actual_length = len(actual)
        self.assertEqual(expected_length, actual_length)
        
        index = None
        for index in range(0, expected_length, 1):
            expected_element = expected_list[index]
            actual_element = actual[index]
            self.assertTrue(isinstance(actual_element, dummy_without_eq.Dummy))
    
    # endregion
    
    # endregion
    


import sys
sys.path.append('samples')
import dummy_with_eq
import builtins
import copyreg
import types
import unittest


class TestDummy(unittest.TestCase):
    # region Test suites for executable dummy_with_eq.propagate
    # region
    def test_propagate(self):
        dummy = dummy_with_eq.Dummy(1)
        
        actual = dummy.propagate()
        
        dummy1 = copyreg._reconstructor(dummy_with_eq.Dummy, builtins.object, None)
        dummy1.field = 1
        expected_list = [dummy1, dummy1]
        expected_length = len(expected_list)
        actual_length = len(actual)
        
        self.assertEqual(expected_length, actual_length)
        
        index = None
        for index in range(0, expected_length, 1):
            expected_element = expected_list[index]
            actual_element = actual[index]
            actual_field = actual_element.field
            expected_field = expected_element.field
            
            self.assertEqual(expected_field, actual_field)
    
    # endregion
    
    # endregion
    


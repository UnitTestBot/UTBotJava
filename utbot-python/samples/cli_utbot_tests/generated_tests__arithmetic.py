import sys
sys.path.append('samples')
import builtins
import arithmetic
import unittest


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable arithmetic.calculate_function_value
    # region
    def test_calculate_function_value(self):
        actual = arithmetic.calculate_function_value(1, 101)
        
        self.assertEqual(11886.327847992769, actual)
    
    def test_calculate_function_value1(self):
        actual = arithmetic.calculate_function_value(4294967296, 101)
        
        self.assertEqual(65535.99845886229, actual)
    
    def test_calculate_function_value2(self):
        actual = arithmetic.calculate_function_value(float('nan'), 4294967296)
        
        self.assertTrue(isinstance(actual, builtins.float))
    
    def test_calculate_function_value_throws_t(self):
        arithmetic.calculate_function_value(0, 101)
        
        # raises builtins.ZeroDivisionError
    
    def test_calculate_function_value_throws_t1(self):
        arithmetic.calculate_function_value(101, 101)
        
        # raises builtins.ValueError
    
    # endregion
    
    # endregion
    


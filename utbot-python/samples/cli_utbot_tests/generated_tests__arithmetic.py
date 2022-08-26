import sys
sys.path.append('samples')
import unittest
import builtins
import arithmetic


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable arithmetic.calculate_function_value
    # region
    def test_calculate_function_value(self):
        actual = arithmetic.calculate_function_value(1, 4294967297)
        
        self.assertEqual(2.1922020374737994e+19, actual)
    
    def test_calculate_function_value1(self):
        actual = arithmetic.calculate_function_value((1 << 100), 4294967297)
        
        self.assertEqual(1125899906842624.0, actual)
    
    def test_calculate_function_value2(self):
        actual = arithmetic.calculate_function_value(float('nan'), 101)
        
        self.assertTrue(isinstance(actual, builtins.float))
    
    def test_calculate_function_value_throws_t(self):
        arithmetic.calculate_function_value(101, 4294967297)
        
        # raises builtins.ValueError
    
    def test_calculate_function_value_throws_t1(self):
        arithmetic.calculate_function_value(0, 4294967297)
        
        # raises builtins.ZeroDivisionError
    
    def test_calculate_function_value_throws_t2(self):
        arithmetic.calculate_function_value(float('nan'), -3)
        
        # raises builtins.ValueError
    
    # endregion
    
    # endregion
    


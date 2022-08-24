import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import builtins
import arithmetic


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable arithmetic.calculate_function_value
    # region
    def test_calculate_function_value(self):
        actual = arithmetic.calculate_function_value(int('4000001', 32), int(' 0O123   ', 0))
        
        self.assertEqual(65535.99874114989, actual)
    
    def test_calculate_function_value1(self):
        actual = arithmetic.calculate_function_value(int(' 0O123   ', 0), int(' 0O123   ', 0))
        
        self.assertEqual(14228.114055679447, actual)
    
    def test_calculate_function_value2(self):
        actual = arithmetic.calculate_function_value(float('+NAn'), int('0123', 10))
        
        self.assertTrue(isinstance(actual, builtins.float))
    
    def test_calculate_function_value_throws_t(self):
        arithmetic.calculate_function_value(101, int(' 0O123   ', 0))
        
        # raises builtins.ValueError
    
    def test_calculate_function_value_throws_t1(self):
        arithmetic.calculate_function_value(int('000', 0), int(' 0O123   ', 0))
        
        # raises builtins.ZeroDivisionError
    
    # endregion
    
    # endregion
    


import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import builtins
import samples.arithmetic


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable samples.arithmetic.calculate_function_value
    # region
    def test_calculate_function_value(self):
        actual = samples.arithmetic.calculate_function_value(100, 101)
        
        self.assertEqual(-39499.12548601342, actual)
    
    def test_calculate_function_value_throws_t(self):
        samples.arithmetic.calculate_function_value(101, 101)
        
        # raises builtins.ValueError
    
    def test_calculate_function_value1(self):
        actual = samples.arithmetic.calculate_function_value(100, 3)
        
        self.assertEqual(-58078.585141651354, actual)
    
    # endregion
    
    # endregion
    


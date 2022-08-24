import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import builtins
import samples.test_coverage


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable samples.coverage.hard_function
    # region
    def test_hard_function(self):
        actual = samples.coverage.hard_function(int('000', 0))
        
        self.assertEqual(1, actual)
    
    def test_hard_function1(self):
        actual = samples.test_coverage.hard_function(3)
        
        self.assertEqual(2, actual)
    
    def test_hard_function2(self):
        actual = samples.coverage.hard_function(int('9ba461594', 12))
        
        self.assertEqual(3, actual)
    
    def test_hard_function3(self):
        actual = samples.coverage.hard_function(float('+NAn'))
        
        self.assertEqual(4, actual)
    
    # endregion
    
    # endregion
    


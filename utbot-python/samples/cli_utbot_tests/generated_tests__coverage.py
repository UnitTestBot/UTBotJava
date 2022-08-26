import sys
sys.path.append('samples')
import unittest
import builtins
import test_coverage


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable test_coverage.hard_function
    # region
    def test_hard_function(self):
        actual = test_coverage.hard_function(83)
        
        self.assertEqual(2, actual)
    
    def test_hard_function1(self):
        actual = test_coverage.hard_function(0)
        
        self.assertEqual(1, actual)
    
    def test_hard_function2(self):
        actual = test_coverage.hard_function(4294967296)
        
        self.assertEqual(3, actual)
    
    def test_hard_function3(self):
        actual = test_coverage.hard_function(float('nan'))
        
        self.assertEqual(4, actual)
    
    # endregion
    
    # endregion
    


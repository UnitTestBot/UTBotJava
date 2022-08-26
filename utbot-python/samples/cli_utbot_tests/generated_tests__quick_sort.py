import sys
sys.path.append('samples')
import unittest
import builtins
import quick_sort


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable quick_sort.quick_sort
    # region
    def test_quick_sort(self):
        actual = quick_sort.quick_sort([83, -3, -3, 1])
        
        self.assertEqual([-3, -3, 1, 83], actual)
    
    def test_quick_sort1(self):
        actual = quick_sort.quick_sort([1, 1])
        
        self.assertEqual([1, 1], actual)
    
    def test_quick_sort2(self):
        actual = quick_sort.quick_sort([])
        
        self.assertEqual([], actual)
    
    # endregion
    
    # endregion
    


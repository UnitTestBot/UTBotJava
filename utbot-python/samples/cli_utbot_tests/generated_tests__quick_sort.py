import sys
sys.path.append('samples')
import unittest
import builtins
import quick_sort


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable quick_sort.quick_sort
    # region
    def test_quick_sort(self):
        actual = quick_sort.quick_sort([4294967296, 0, 1, 123, 0, (1 << 100)])
        
        self.assertEqual([0, 0, 1, 123, 4294967296, 1267650600228229401496703205376], actual)
    
    def test_quick_sort1(self):
        actual = quick_sort.quick_sort([0, 0, 1])
        
        self.assertEqual([0, 0, 1], actual)
    
    def test_quick_sort2(self):
        actual = quick_sort.quick_sort([])
        
        self.assertEqual([], actual)
    
    # endregion
    
    # endregion
    


import sys
sys.path.append('samples')
import builtins
import quick_sort
import unittest


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable quick_sort.quick_sort
    # region
    def test_quick_sort(self):
        actual = quick_sort.quick_sort([4294967297, 83, (1 << 100), 4294967297, (1 << 100), 0, -3])
        
        self.assertEqual([-3, 0, 83, 4294967297, 4294967297, 1267650600228229401496703205376, 1267650600228229401496703205376], actual)
    
    def test_quick_sort1(self):
        actual = quick_sort.quick_sort([83, 123])
        
        self.assertEqual([83, 123], actual)
    
    def test_quick_sort2(self):
        actual = quick_sort.quick_sort([])
        
        self.assertEqual([], actual)
    
    # endregion
    
    # endregion
    


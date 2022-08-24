import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import builtins
import quick_sort


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable quick_sort.quick_sort
    # region
    def test_quick_sort(self):
        actual = quick_sort.quick_sort([-3, 0, -1, 83, 1, 0, 1])
        
        self.assertEqual([-3, -1, 0, 0, 1, 1, 83], actual)
    
    def test_quick_sort1(self):
        actual = quick_sort.quick_sort([0, 4294967297])
        
        self.assertEqual([0, 4294967297], actual)
    
    def test_quick_sort2(self):
        actual = quick_sort.quick_sort([])
        
        self.assertEqual([], actual)
    
    # endregion
    
    # endregion
    


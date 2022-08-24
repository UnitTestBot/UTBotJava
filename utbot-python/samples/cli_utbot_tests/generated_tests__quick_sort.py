import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import builtins
import quick_sort


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable quick_sort.quick_sort
    # region
    def test_quick_sort(self):
        actual = quick_sort.quick_sort([int('-1')])
        
        self.assertEqual([-1], actual)
    
    def test_quick_sort1(self):
        actual = quick_sort.quick_sort([int('3723ai4h', 20), int('100000001', 16), int('535a7988a', 13), int('000', 0), int('4000001', 32), int('000', 0), 0, 0])
        
        self.assertEqual([0, 0, 0, 0, 4294967297, 4294967297, 4294967297, 4294967297], actual)
    
    def test_quick_sort2(self):
        actual = quick_sort.quick_sort([])
        
        self.assertEqual([], actual)
    
    # endregion
    
    # endregion
    


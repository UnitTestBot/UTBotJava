import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import builtins
import samples.quick_sort


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable samples.quick_sort.quick_sort
    # region
    def test_quick_sort(self):
        actual = samples.quick_sort.quick_sort([int('000', 0), int('-1'), int('-1'), int('535a7988a', 13), int(' 0O123   ', 0), int('3723ai4h', 20), int('0123', 10), 1, 0, 0, int('0123', 10)])
        
        self.assertEqual([-1, -1, 0, 0, 0, 1, 83, 123, 123, 4294967297, 4294967297], actual)
    
    def test_quick_sort1(self):
        actual = samples.quick_sort.quick_sort([])
        
        self.assertEqual([], actual)
    
    # endregion
    
    # endregion
    


import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import builtins
import samples.longest_subsequence


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable samples.longest_subsequence.longest_subsequence
    # region
    def test_longest_subsequence(self):
        actual = samples.longest_subsequence.longest_subsequence([int('-1'), int('535a7988a', 13), int('100000001', 16), int('0123', 10), int('0123', 10), int('-1')])
        
        self.assertEqual([-1, 123, 123], actual)
    
    # endregion
    
    # endregion
    


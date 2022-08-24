import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import builtins
import longest_subsequence


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable longest_subsequence.longest_subsequence
    # region
    def test_longest_subsequence(self):
        actual = longest_subsequence.longest_subsequence([int('100000001', 16), int('000', 0), int('9ba461594', 12), int('100000001', 16), int('100000001', 16), 0, 2, int('0123', 10), int('-1'), int('4000001', 32)])
        
        self.assertEqual([0, 0, 2, 123, 4294967297], actual)
    
    def test_longest_subsequence1(self):
        actual = longest_subsequence.longest_subsequence([1, int('3723ai4h', 20)])
        
        self.assertEqual([1, 4294967297], actual)
    
    def test_longest_subsequence2(self):
        actual = longest_subsequence.longest_subsequence([2, int('000', 0)])
        
        self.assertEqual([0], actual)
    
    # endregion
    
    # endregion
    


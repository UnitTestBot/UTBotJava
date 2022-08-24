import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import builtins
import longest_subsequence


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable longest_subsequence.longest_subsequence
    # region
    def test_longest_subsequence(self):
        actual = longest_subsequence.longest_subsequence([0, 83, 1, 2, 2, 123, 0])
        
        self.assertEqual([0, 1, 2, 2, 123], actual)
    
    def test_longest_subsequence1(self):
        actual = longest_subsequence.longest_subsequence([1, 1, 1, 2])
        
        self.assertEqual([1, 1, 1, 2], actual)
    
    # endregion
    
    # endregion
    


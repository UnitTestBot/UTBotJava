import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import builtins
import longest_subsequence


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable longest_subsequence.longest_subsequence
    # region
    def test_longest_subsequence(self):
        actual = longest_subsequence.longest_subsequence([1, 2, 83, 83, 1, (1 << 100), (1 << 100), -1, 123, -1, 1, 4294967296, 4294967296])
        
        self.assertEqual([1, 2, 83, 83, 123, 4294967296, 4294967296], actual)
    
    # endregion
    
    # endregion
    


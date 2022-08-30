import sys
sys.path.append('samples')
import builtins
import longest_subsequence
import unittest


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable longest_subsequence.longest_subsequence
    # region
    def test_longest_subsequence(self):
        actual = longest_subsequence.longest_subsequence([1, 83])
        
        self.assertEqual([1, 83], actual)
    
    def test_longest_subsequence1(self):
        actual = longest_subsequence.longest_subsequence([2, -1, 4294967296])
        
        self.assertEqual([-1, 4294967296], actual)
    
    # endregion
    
    # endregion
    


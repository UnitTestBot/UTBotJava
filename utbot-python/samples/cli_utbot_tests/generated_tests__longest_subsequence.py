import sys
sys.path.append('samples')
import unittest
import builtins
import longest_subsequence


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable longest_subsequence.longest_subsequence
    # region
    def test_longest_subsequence(self):
        actual = longest_subsequence.longest_subsequence([1, -1])
        
        self.assertEqual([-1], actual)
    
    def test_longest_subsequence1(self):
        actual = longest_subsequence.longest_subsequence([1, 0, -3, 0, 1])
        
        self.assertEqual([0, 0, 1], actual)
    
    def test_longest_subsequence2(self):
        actual = longest_subsequence.longest_subsequence([0, 2, 123])
        
        self.assertEqual([0, 2, 123], actual)
    
    # endregion
    
    # endregion
    


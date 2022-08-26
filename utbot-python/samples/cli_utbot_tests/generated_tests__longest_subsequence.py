import sys
sys.path.append('samples')
import unittest
import builtins
import longest_subsequence


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable longest_subsequence.longest_subsequence
    # region
    def test_longest_subsequence(self):
        actual = longest_subsequence.longest_subsequence([1, 0, 83, 1, 1])
        
        self.assertEqual([0, 1, 1], actual)
    
    # endregion
    
    # endregion
    


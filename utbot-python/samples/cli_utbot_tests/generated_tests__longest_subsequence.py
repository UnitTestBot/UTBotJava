import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import builtins
import longest_subsequence


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable samples.longest_subsequence.longest_subsequence
    # region
    def test_longest_subsequence(self):
        actual = longest_subsequence.longest_subsequence([4294967296, -3, -3, 4294967297, 0, -3, 83, -1, 1, 1, 1, 1])

        self.assertEqual([-3, -3, 0, 1, 1, 1, 1], actual)

    def test_longest_subsequence1(self):
        actual = longest_subsequence.longest_subsequence([83, (1 << 100)])

        self.assertEqual([83, 1267650600228229401496703205376], actual)

    # endregion

    # endregion


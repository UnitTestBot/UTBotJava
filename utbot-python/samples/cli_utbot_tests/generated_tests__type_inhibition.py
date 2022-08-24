import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import builtins
import type_inhibition


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable type_inhibition.inhibition
    # region
    def test_inhibition_by_fuzzer(self):
        actual = type_inhibition.inhibition(int('000', 0), str(1e+300 * 1e+300), str(b'python.org', 'idna'), [], {})
        
        self.assertEqual([0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 'inf'], actual)
    
    # endregion
    
    # endregion
    


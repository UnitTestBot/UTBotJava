import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import builtins
import samples.type_inhibition


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable samples.type_inhibition.inhibition
    # region
    def test_inhibition_by_fuzzer(self):
        actual = samples.type_inhibition.inhibition(int('3723ai4h', 20), str(Exception('a')), str(1.5 + 3.5j), [], {})
        
        self.assertEqual([0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 'a'], actual)
    
    # endregion
    
    # endregion
    


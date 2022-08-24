import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import builtins
import samples.primitive_types
import deep_equals


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable samples.primitive_types.pretty_print
    # region
    def test_pretty_print(self):
        actual = samples.primitive_types.pretty_print('It is complex.\n')
        
        self.assertEqual('It is string.\nValue <<It is complex.\n>>', actual)
    
    def test_pretty_print1(self):
        x = deep_equals.Node(' + ')
        
        actual = samples.primitive_types.pretty_print(x)
        
        self.assertEqual('I do not have any variants', actual)
    
    def test_pretty_print2(self):
        actual = samples.primitive_types.pretty_print([])
        
        self.assertEqual('It is list.\nValue []', actual)
    
    # endregion
    
    # endregion
    


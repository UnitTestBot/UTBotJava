import sys
sys.path.append('samples')
import unittest
import builtins
import primitive_types


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable primitive_types.pretty_print
    # region
    def test_pretty_print(self):
        actual = primitive_types.pretty_print(object())
        
        self.assertEqual('I do not have any variants', actual)
    
    def test_pretty_print1(self):
        actual = primitive_types.pretty_print(str(b'\x80'))
        
        self.assertEqual("It is string.\nValue <<b'\\x80'>>", actual)
    
    def test_pretty_print2(self):
        actual = primitive_types.pretty_print((1 << 100))
        
        self.assertEqual('It is integer.\nValue 1267650600228229401496703205376', actual)
    
    def test_pretty_print3(self):
        actual = primitive_types.pretty_print(complex(float('inf'), float('inf')))
        
        self.assertEqual('It is complex.\nValue (inf + infi)', actual)
    
    def test_pretty_print4(self):
        actual = primitive_types.pretty_print([])
        
        self.assertEqual('It is list.\nValue []', actual)
    
    # endregion
    
    # endregion
    


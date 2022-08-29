import sys
sys.path.append('samples')
import unittest
import builtins
import dicts
import types


class TestDictionary(unittest.TestCase):
    # region Test suites for executable dicts.translate
    # region
    def test_translate(self):
        dictionary = dicts.Dictionary([str(-1234567890), str(-123456789), str(b'\x80'), str(id), str(id), str('unicode remains unicode'), str(-123456789)], [])
        
        actual = dictionary.translate(str(id), str(1.5 + 3.5j))
        
        self.assertEqual(None, actual)
    
    def test_translate_throws_t(self):
        dictionary = dicts.Dictionary([], [{str(): str(), str(-1234567890): str(), str(id): str(1e+300 * 1e+300), str(-123456789): str(), str(1e+300 * 1e+300): str(1e+300 * 1e+300)}, {str(1e+300 * 1e+300): str(1e+300 * 1e+300), str(id): str()}, {}, {str(): str(), str(-1234567890): str(), str(id): str(1e+300 * 1e+300), str(-123456789): str(), str(1e+300 * 1e+300): str(1e+300 * 1e+300)}, {str(1e+300 * 1e+300): str(), str(b'\x80'): str(), str(-123456789): str(1e+300 * 1e+300), str('unicode remains unicode'): str()}])
        
        dictionary.translate(str('unicode remains unicode'), str(1.5 + 3.5j))
        
        # raises builtins.KeyError
    
    # endregion
    
    # endregion
    


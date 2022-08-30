import sys
sys.path.append('samples')
import builtins
import dicts
import types
import unittest


class TestDictionary(unittest.TestCase):
    # region Test suites for executable dicts.translate
    # region
    def test_translate(self):
        dictionary = dicts.Dictionary([str(1.5 + 3.5j), str(1.5 + 3.5j), str('unicode remains unicode'), str(b'\x80'), str(-1234567890)], [{str(-123456789): str(1e+300 * 1e+300)}, {str(1.5 + 3.5j): str(), str(-123456789): str(), str(1e+300 * 1e+300): str(), str(): str(1e+300 * 1e+300)}])
        
        actual = dictionary.translate(str(-1234567890), str(-123456789))
        
        self.assertEqual(None, actual)
    
    def test_translate_throws_t(self):
        dictionary = dicts.Dictionary([str(1e+300 * 1e+300), str(1.5 + 3.5j), str(1e+300 * 1e+300), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(1.5 + 3.5j), str(-1234567890)], [{str(b'\x80'): str(1e+300 * 1e+300), str(-123456789): str(1e+300 * 1e+300)}, {str(b'\x80'): str(1e+300 * 1e+300), str(b'\xf0\xa3\x91\x96', 'utf-8'): str(), str(id): str(1e+300 * 1e+300), str(): str()}, {str(b'\x80'): str(1e+300 * 1e+300), str(-123456789): str(), str(): str()}, {str(1e+300 * 1e+300): str(), str(-123456789): str(1e+300 * 1e+300)}, {str(1.5 + 3.5j): str(), str(1e+300 * 1e+300): str()}])
        
        dictionary.translate(str(), str('unicode remains unicode'))
        
        # raises builtins.KeyError
    
    # endregion
    
    # endregion
    


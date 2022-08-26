import sys
sys.path.append('samples')
import unittest
import builtins
import dicts
import copyreg
import types


class TestDictionary(unittest.TestCase):
    # region Test suites for executable dicts.translate
    # region
    def test_translate(self):
        dictionary = dicts.Dictionary([str(), str(-123456789), str(b'\x80'), str(1.5 + 3.5j), str(b'\x80'), str(1.5 + 3.5j), str(1e+300 * 1e+300)], [{str(b'\x80'): str(1e+300 * 1e+300), str('unicode remains unicode'): str(), str(1.5 + 3.5j): str(), str(): str()}, {str(): str()}, {}, {str('unicode remains unicode'): str(), str(b'\x80'): str(1e+300 * 1e+300), str(-123456789): str(1e+300 * 1e+300), str(1.5 + 3.5j): str()}, {str('unicode remains unicode'): str()}, {str(): str()}, {str(1.5 + 3.5j): str(), str(-123456789): str(1e+300 * 1e+300), str(1e+300 * 1e+300): str(), str(id): str(), str(b'\x80'): str(1e+300 * 1e+300), str(): str()}])
        
        actual = dictionary.translate(str(), str(1.5 + 3.5j))
        
        word = copyreg._reconstructor(dicts.Word, builtins.object, None)
        word.translations = {"b'\\x80'": 'inf', 'unicode remains unicode': '', '(1.5+3.5j)': '', '': '', }
        actual_translations = actual.translations
        expected_translations = word.translations
        self.assertEqual(expected_translations, actual_translations)
    
    def test_translate_throws_t(self):
        dictionary = dicts.Dictionary([str(-1234567890), str(1e+300 * 1e+300), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(1.5 + 3.5j), str(-123456789), str(), str()], [{str(-1234567890): str(), str('unicode remains unicode'): str(), str(-123456789): str(), str(id): str(), str(): str(1e+300 * 1e+300)}, {str(id): str(), str(1e+300 * 1e+300): str(1e+300 * 1e+300), str(): str(), str('unicode remains unicode'): str()}, {str(id): str(), str(1e+300 * 1e+300): str(1e+300 * 1e+300), str(): str(), str('unicode remains unicode'): str()}, {str(): str(), str(-1234567890): str(), str(b'\x80'): str(), str('unicode remains unicode'): str()}, {str(): str()}, {}])
        
        dictionary.translate(str(-1234567890), str(1.5 + 3.5j))
        
        # raises builtins.KeyError
    
    # endregion
    
    # endregion
    


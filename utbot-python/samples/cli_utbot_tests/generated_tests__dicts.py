import sys
sys.path.append('samples')
import builtins
import types
import dicts
import unittest


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable dicts.keys
    
    # region
    
    def test_keys(self):
        word = dicts.Word({str(-123456789): str(), str(1.5 + 3.5j): str(), str(b'\x80'): str(), str(): str(1e+300 * 1e+300), str('unicode remains unicode'): str(), })
        
        actual = word.keys()
        
        self.assertEqual(['-123456789', '(1.5+3.5j)', "b'\\x80'", '', 'unicode remains unicode'], actual)
    # endregion
    
    # endregion
    
    # region Test suites for executable dicts.translate
    
    # region
    
    def test_translate(self):
        dictionary = dicts.Dictionary([str(b'\xf0\xa3\x91\x96', 'utf-8'), str(id), str(1e+300 * 1e+300)], [])
        
        actual = dictionary.translate(str(id), str(1.5 + 3.5j))
        
        self.assertEqual(None, actual)
    
    def test_translate_throws_t(self):
        dictionary = dicts.Dictionary([], [{str(): str(), str(1e+300 * 1e+300): str(1e+300 * 1e+300), str(b'\x80'): str(), str(1.5 + 3.5j): str(), }, {str(-123456789): str(), str(id): str(), str(): str(), str(-1234567890): str(), }, {str(1.5 + 3.5j): str(), str(1e+300 * 1e+300): str(), str(-1234567890): str(), str(): str(1e+300 * 1e+300), }])
        
        dictionary.translate(str('unicode remains unicode'), str(1.5 + 3.5j))
        
        # raises builtins.KeyError
    # endregion
    
    # endregion


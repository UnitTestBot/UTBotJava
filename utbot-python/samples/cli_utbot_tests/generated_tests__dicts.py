import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import builtins
import dicts
import types


class TestDictionary(unittest.TestCase):
    # region Test suites for executable dicts.translate
    # region
    def test_translate(self):
        dictionary = dicts.Dictionary([str(-123456789), str(id), str(1e+300 * 1e+300), str('unicode remains unicode'), str(1e+300 * 1e+300), str(id)], [])
        
        actual = dictionary.translate(str(id), str(1.5 + 3.5j))
        
        self.assertEqual(None, actual)
    
    def test_translate_throws_t(self):
        dictionary = dicts.Dictionary([], [{}, {str(id): str(b'\x80'), str(-123456789): str(b'\x80'), str(1e+300 * 1e+300): str(id), str(b'\xf0\xa3\x91\x96', 'utf-8'): str(), str(): str(), str('unicode remains unicode'): str(b'\x80')}, {str(1e+300 * 1e+300): str(id), str(id): str(b'\x80'), str(-123456789): str(), str(1.5 + 3.5j): str(b'\x80'), str(-1234567890): str(b'\x80'), str(b'\x80'): str(id), str('unicode remains unicode'): str(1e+300 * 1e+300)}, {str(1e+300 * 1e+300): str(id), str(id): str(b'\x80'), str(-123456789): str(), str(1.5 + 3.5j): str(b'\x80'), str(-1234567890): str(b'\x80'), str(b'\x80'): str(id), str('unicode remains unicode'): str(1e+300 * 1e+300)}, {str(b'\x80'): str(b'\x80'), str('unicode remains unicode'): str(b'\x80'), str(id): str(1e+300 * 1e+300)}, {}, {str(1.5 + 3.5j): str(), str(b'\x80'): str(id), str(id): str(1e+300 * 1e+300), str(b'\xf0\xa3\x91\x96', 'utf-8'): str(1e+300 * 1e+300), str(-123456789): str()}, {str(id): str(b'\x80'), str(-123456789): str(b'\x80'), str(1e+300 * 1e+300): str(id), str(b'\xf0\xa3\x91\x96', 'utf-8'): str(), str(): str(), str('unicode remains unicode'): str(b'\x80')}])
        
        dictionary.translate(str('unicode remains unicode'), str(1.5 + 3.5j))
        
        # raises builtins.KeyError
    
    # endregion
    
    # endregion
    


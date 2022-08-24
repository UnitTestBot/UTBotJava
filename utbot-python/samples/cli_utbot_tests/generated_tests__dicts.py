import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import builtins
import dicts
import copyreg
import types


class TestDictionary(unittest.TestCase):
    # region Test suites for executable dicts.translate
    # region
    def test_translate(self):
        dictionary = dicts.Dictionary([str(1e+300 * 1e+300), str(-1234567890), str(Exception), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(-1234567890), str(b'\xf0\xa3\x91\x96', 'utf-8')], [{str(Exception): str(b'python.org', 'idna'), str(1e+300 * 1e+300): str(b'\xf0\xa3\x91\x96', 'utf-8'), str(b'python.org.', 'idna'): str(b'xn--pythn-mua.org', 'idna'), str(b'python.org', 'idna'): str(b'xn--pythn-mua.org', 'idna'), str(b'\xf0\xa3\x91\x96', 'utf-8'): str(b'xn--pythn-mua.org', 'idna'), str(b''): str(b'xn--pythn-mua.org', 'idna')}, {str(b'python.org.', 'idna'): str(b'\xf0\xa3\x91\x96', 'utf-8'), str(-1234567890): str(b'xn--pythn-mua.org', 'idna')}, {}, {str(1.5 + 3.5j): str(b'xn--pythn-mua.org', 'idna'), str(b'xn--pythn-mua.org', 'idna'): str(b'python.org', 'idna'), str(Exception): str(b'\xf0\xa3\x91\x96', 'utf-8'), str(b'python.org', 'idna'): str(b'python.org', 'idna'), str(b'\xf0\xa3\x91\x96', 'utf-8'): str(b'python.org', 'idna'), str(-1234567890): str(b'python.org', 'idna'), str(b'python.org.', 'idna'): str(b'python.org', 'idna'), str(1e+300 * 1e+300): str(b'\xf0\xa3\x91\x96', 'utf-8')}])
        
        actual = dictionary.translate(str(b'\xf0\xa3\x91\x96', 'utf-8'), str(b'python.org.', 'idna'))
        
        word = copyreg._reconstructor(dicts.Word, builtins.object, None)
        word.translations = {'python.org.': '𣑖', '-1234567890': 'pythön.org', }
        actual_translations = actual.translations
        expected_translations = word.translations
        self.assertEqual(expected_translations, actual_translations)
    
    def test_translate_throws_t(self):
        dictionary = dicts.Dictionary([str(b'xn--pythn-mua.org', 'idna'), str(b'xn--pythn-mua.org', 'idna'), str(1e+300 * 1e+300), str(b'python.org', 'idna'), str(b'python.org', 'idna'), str(-1234567890)], [{str(b'\xf0\xa3\x91\x96', 'utf-8'): str(b'python.org', 'idna'), str(b'python.org', 'idna'): str(1e+300 * 1e+300), str(-1234567890): str(b'python.org', 'idna'), str(1e+300 * 1e+300): str(b'xn--pythn-mua.org', 'idna'), str(b'python.org.', 'idna'): str(b'xn--pythn-mua.org', 'idna')}, {str(1e+300 * 1e+300): str(b'\xf0\xa3\x91\x96', 'utf-8'), str(-1234567890): str(b'xn--pythn-mua.org', 'idna'), str(b'\xf0\xa3\x91\x96', 'utf-8'): str(b'xn--pythn-mua.org', 'idna'), str(Exception): str(b'python.org', 'idna'), str(1.5 + 3.5j): str(b'python.org', 'idna'), str(b'python.org.', 'idna'): str(b'\xf0\xa3\x91\x96', 'utf-8')}, {str(1.5 + 3.5j): str(b'xn--pythn-mua.org', 'idna'), str(1e+300 * 1e+300): str(b'xn--pythn-mua.org', 'idna'), str(b'xn--pythn-mua.org', 'idna'): str(b'xn--pythn-mua.org', 'idna'), str(b''): str(b'\xf0\xa3\x91\x96', 'utf-8'), str(b'python.org.', 'idna'): str(b'python.org', 'idna'), str(Exception): str(b'python.org', 'idna')}, {str(b'python.org.', 'idna'): str(b'\xf0\xa3\x91\x96', 'utf-8'), str(-1234567890): str(b'xn--pythn-mua.org', 'idna')}, {str(b'\xf0\xa3\x91\x96', 'utf-8'): str(b'python.org', 'idna'), str(b'python.org', 'idna'): str(1e+300 * 1e+300), str(-1234567890): str(b'python.org', 'idna'), str(1e+300 * 1e+300): str(b'xn--pythn-mua.org', 'idna'), str(b'python.org.', 'idna'): str(b'xn--pythn-mua.org', 'idna')}, {}, {str(b'\xf0\xa3\x91\x96', 'utf-8'): str(b'python.org', 'idna'), str(b'python.org', 'idna'): str(b'xn--pythn-mua.org', 'idna'), str(b''): str(b'\xf0\xa3\x91\x96', 'utf-8'), str(Exception): str(b'xn--pythn-mua.org', 'idna')}])
        
        dictionary.translate(str(b''), str(b'python.org.', 'idna'))
        
        # raises builtins.KeyError
    
    # endregion
    
    # endregion
    


import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import builtins
import samples.dicts


class TestDictionary(unittest.TestCase):
    # region Test suites for executable samples.dicts.translate
    # region
    def test_translate_throws_t(self):
        dictionary = samples.dicts.Dictionary([str(b'xn--pythn-mua.org', 'idna'), str(1.5 + 3.5j), str(b'\xf0\xa3\x91\x96', 'utf-8')], [{str(b'xn--pythn-mua.org', 'idna'): str(1.5 + 3.5j), str(b'python.org.', 'idna'): str(Exception('a'))}])
        
        dictionary.translate(str(b''), str(float('-infinity')))
        
        # raises builtins.KeyError
    
    def test_translate_throws_t1(self):
        dictionary = samples.dicts.Dictionary([str(b'python.org.', 'idna'), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(b''), str(1.5 + 3.5j), str(float('-nan'))], [{str(b'xn--pythn-mua.org', 'idna'): str(Exception('a')), str(b'\xf0\xa3\x91\x96', 'utf-8'): str(b'xn--pythn-mua.org', 'idna'), str(1.5 + 3.5j): str(b'xn--pythn-mua.org', 'idna'), str(b'python.org', 'idna'): str(b'xn--pythn-mua.org', 'idna')}, {}, {str(Exception('a')): str(Exception('a')), str(float('-nan')): str(1.5 + 3.5j), str(b'python.org', 'idna'): str(Exception('a'))}])
        
        dictionary.translate(str(float('-nan')), str(float('-infinity')))
        
        # raises builtins.KeyError
    
    def test_translate_throws_t2(self):
        dictionary = samples.dicts.Dictionary([], [{str(b'xn--pythn-mua.org', 'idna'): str(Exception('a')), str(b'\xf0\xa3\x91\x96', 'utf-8'): str(b'xn--pythn-mua.org', 'idna'), str(1.5 + 3.5j): str(b'xn--pythn-mua.org', 'idna'), str(b'python.org', 'idna'): str(b'xn--pythn-mua.org', 'idna')}, {}, {str(Exception('a')): str(Exception('a')), str(float('-nan')): str(1.5 + 3.5j), str(b'python.org', 'idna'): str(Exception('a'))}])
        
        dictionary.translate(str(b'python.org', 'idna'), str(float('-infinity')))
        
        # raises builtins.KeyError
    
    def test_translate_throws_t3(self):
        dictionary = samples.dicts.Dictionary([str(float('-nan')), str(b'xn--pythn-mua.org', 'idna')], [{str(b'xn--pythn-mua.org', 'idna'): str(Exception('a')), str(b'\xf0\xa3\x91\x96', 'utf-8'): str(b'xn--pythn-mua.org', 'idna'), str(1.5 + 3.5j): str(b'xn--pythn-mua.org', 'idna'), str(b'python.org', 'idna'): str(b'xn--pythn-mua.org', 'idna')}, {}, {str(Exception('a')): str(Exception('a')), str(float('-nan')): str(1.5 + 3.5j), str(b'python.org', 'idna'): str(Exception('a'))}])
        
        dictionary.translate(str(float('-infinity')), str(float('-infinity')))
        
        # raises builtins.KeyError
    
    # endregion
    
    # endregion
    


import sys
sys.path.append('samples')
import unittest
import builtins
import lists
import datetime


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable lists.find_articles_with_author
    # region
    def test_find_articles_with_author(self):
        actual = lists.find_articles_with_author([lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(b'\x80'), datetime.datetime(2014, 11, 2, 1, 30)), lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(1.5 + 3.5j), datetime.datetime(2014, 11, 2, 1, 30)), lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str('unicode remains unicode'), datetime.datetime(2011, 1, 1)), lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str('unicode remains unicode'), datetime.datetime(2011, 1, 1))], str('unicode remains unicode'))
        
        self.assertEqual([], actual)
    
    # endregion
    
    # endregion
    


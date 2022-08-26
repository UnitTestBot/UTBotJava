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
        actual = lists.find_articles_with_author([lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(b'\x80'), datetime.datetime(2014, 11, 2, 1, 30)), lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(id), datetime.datetime(1, 1, 1)), lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(b'\x80'), datetime.datetime(2014, 11, 2, 1, 30)), lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(b'\x80'), datetime.datetime(2014, 11, 2, 1, 30)), lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(-1234567890), datetime.datetime(1, 2, 3, 4, 5, 6, 7)), lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(-123456789), datetime.datetime(2011, 1, 1)), lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(), datetime.datetime(2014, 11, 2, 1, 30))], str('unicode remains unicode'))
        
        self.assertEqual([], actual)
    
    # endregion
    
    # endregion
    


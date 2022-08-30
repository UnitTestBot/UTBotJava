import sys
sys.path.append('samples')
import builtins
import lists
import datetime
import unittest


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable lists.find_articles_with_author
    # region
    def test_find_articles_with_author(self):
        actual = lists.find_articles_with_author([lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str('unicode remains unicode'), datetime.datetime(2015, 4, 5, 1, 45)), lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str('unicode remains unicode'), datetime.datetime(2011, 1, 1)), lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(), datetime.datetime(1, 2, 3, 4, 5, 6, 7)), lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(id), datetime.datetime(1, 2, 3, 4, 5, 6, 7)), lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(id), datetime.datetime(2014, 11, 2, 1, 30)), lists.Article(str(-123456789), str(b'\xf0\xa3\x91\x96', 'utf-8'), str(id), datetime.datetime(1, 2, 3, 4, 5, 6, 7))], str('unicode remains unicode'))
        
        self.assertEqual([], actual)
    
    # endregion
    
    # endregion
    


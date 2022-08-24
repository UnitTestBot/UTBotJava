import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import builtins
import samples.lists


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable samples.lists.find_articles_with_author
    # region
    def test_find_articles_with_author(self):
        actual = samples.lists.find_articles_with_author([], str(1.5 + 3.5j))
        
        self.assertEqual([], actual)
    
    # endregion
    
    # endregion
    


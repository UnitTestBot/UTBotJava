import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import builtins
import general
import collections
import copyreg
import abc
import types


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable general.repr_test
    # region
    def test_repr_test(self):
        actual = general.repr_test(int('4000001', 32))
        
        user_list = copyreg._reconstructor(collections.UserList, builtins.object, None)
        user_list.data = [1, 2, 3]
        counter = collections.Counter({'f': 3, 'l': 1, 'k': 2, 'a': 1, 's': 1, 'd': 1, })
        ordered_dict = collections.OrderedDict()
        ordered_dict[1] = 2
        ordered_dict[4] = 'jflas'
        self.assertEqual([1, 429496729701, user_list, counter, ordered_dict], actual)
    
    # endregion
    
    # endregion
    


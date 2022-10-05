import sys
sys.path.append('samples')
import builtins
import deque
import collections
import unittest


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable deque.generate_people_deque
    # region
    def test_generate_people_deque(self):
        actual = deque.generate_people_deque(4294967297)
        
        deque1 = collections.deque()
        deque1.append('Alex')
        deque1.append('Bob')
        deque1.append('Cate')
        deque1.append('Daisy')
        deque1.append('Ed')
        
        self.assertEqual(deque1, actual)
    
    def test_generate_people_deque1(self):
        actual = deque.generate_people_deque(0)
        
        deque1 = collections.deque()
        
        self.assertEqual(deque1, actual)
    
    # endregion
    
    # endregion
    


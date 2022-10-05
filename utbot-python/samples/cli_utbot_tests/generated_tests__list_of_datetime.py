import sys
sys.path.append('samples')
import builtins
import list_of_datetime
import types
import datetime
import unittest


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable list_of_datetime.get_data_labels
    # region
    def test_get_data_labels(self):
        actual = list_of_datetime.get_data_labels({})
        
        self.assertEqual(None, actual)
    
    def test_get_data_labels1(self):
        actual = list_of_datetime.get_data_labels([datetime.time(0), datetime.time(microsecond=40), datetime.time(18, 45, 3, 1234), datetime.time(12, 0)])
        
        self.assertEqual(['00:00', '00:00', '18:45', '12:00'], actual)
    
    def test_get_data_labels2(self):
        actual = list_of_datetime.get_data_labels([datetime.time(microsecond=40), datetime.time()])
        
        self.assertEqual(['1900-01-01', '1900-01-01'], actual)
    
    # endregion
    
    # endregion
    


import sys
sys.path.append('samples')
import unittest
import builtins
import list_of_datetime
import types
import datetime


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable list_of_datetime.get_data_labels
    # region
    def test_get_data_labels(self):
        actual = list_of_datetime.get_data_labels({})
        
        self.assertEqual(None, actual)
    
    def test_get_data_labels1(self):
        actual = list_of_datetime.get_data_labels([datetime.time(fold=1), datetime.time(22, 12, 55, 99999), datetime.time(fold=1), datetime.time(12, 30)])
        
        self.assertEqual(['00:00', '22:12', '00:00', '12:30'], actual)
    
    def test_get_data_labels2(self):
        actual = list_of_datetime.get_data_labels([datetime.time(0), datetime.time(microsecond=40)])
        
        self.assertEqual(['1900-01-01', '1900-01-01'], actual)
    
    # endregion
    
    # endregion
    


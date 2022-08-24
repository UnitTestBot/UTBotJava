import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import builtins
import deep_equals
import copyreg
import types


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable deep_equals.comparable_list
    # region
    def test_comparable_list(self):
        actual = deep_equals.comparable_list(4294967296)
        
        comparable_class = copyreg._reconstructor(deep_equals.ComparableClass, builtins.object, None)
        comparable_class.x = 0
        comparable_class1 = copyreg._reconstructor(deep_equals.ComparableClass, builtins.object, None)
        comparable_class1.x = 1
        comparable_class2 = copyreg._reconstructor(deep_equals.ComparableClass, builtins.object, None)
        comparable_class2.x = 2
        comparable_class3 = copyreg._reconstructor(deep_equals.ComparableClass, builtins.object, None)
        comparable_class3.x = 3
        comparable_class4 = copyreg._reconstructor(deep_equals.ComparableClass, builtins.object, None)
        comparable_class4.x = 4
        comparable_class5 = copyreg._reconstructor(deep_equals.ComparableClass, builtins.object, None)
        comparable_class5.x = 5
        comparable_class6 = copyreg._reconstructor(deep_equals.ComparableClass, builtins.object, None)
        comparable_class6.x = 6
        comparable_class7 = copyreg._reconstructor(deep_equals.ComparableClass, builtins.object, None)
        comparable_class7.x = 7
        comparable_class8 = copyreg._reconstructor(deep_equals.ComparableClass, builtins.object, None)
        comparable_class8.x = 8
        comparable_class9 = copyreg._reconstructor(deep_equals.ComparableClass, builtins.object, None)
        comparable_class9.x = 9
        self.assertEqual([comparable_class, comparable_class1, comparable_class2, comparable_class3, comparable_class4, comparable_class5, comparable_class6, comparable_class7, comparable_class8, comparable_class9], actual)
    
    # endregion
    
    # endregion
    
    # region Test suites for executable deep_equals.incomparable_list
    # region
    def test_incomparable_list(self):
        actual = deep_equals.incomparable_list(4294967296)
        
        incomparable_class = copyreg._reconstructor(deep_equals.IncomparableClass, builtins.object, None)
        incomparable_class.x = 0
        incomparable_class1 = copyreg._reconstructor(deep_equals.IncomparableClass, builtins.object, None)
        incomparable_class1.x = 1
        incomparable_class2 = copyreg._reconstructor(deep_equals.IncomparableClass, builtins.object, None)
        incomparable_class2.x = 2
        incomparable_class3 = copyreg._reconstructor(deep_equals.IncomparableClass, builtins.object, None)
        incomparable_class3.x = 3
        incomparable_class4 = copyreg._reconstructor(deep_equals.IncomparableClass, builtins.object, None)
        incomparable_class4.x = 4
        incomparable_class5 = copyreg._reconstructor(deep_equals.IncomparableClass, builtins.object, None)
        incomparable_class5.x = 5
        incomparable_class6 = copyreg._reconstructor(deep_equals.IncomparableClass, builtins.object, None)
        incomparable_class6.x = 6
        incomparable_class7 = copyreg._reconstructor(deep_equals.IncomparableClass, builtins.object, None)
        incomparable_class7.x = 7
        incomparable_class8 = copyreg._reconstructor(deep_equals.IncomparableClass, builtins.object, None)
        incomparable_class8.x = 8
        incomparable_class9 = copyreg._reconstructor(deep_equals.IncomparableClass, builtins.object, None)
        incomparable_class9.x = 9
        expected_list = [incomparable_class, incomparable_class1, incomparable_class2, incomparable_class3, incomparable_class4, incomparable_class5, incomparable_class6, incomparable_class7, incomparable_class8, incomparable_class9]
        expected_length = len(expected_list)
        actual_length = len(actual)
        self.assertEqual(expected_length, actual_length)
        
        index = None
        for index in range(0, expected_length, 1):
            expected_element = expected_list[index]
            actual_element = actual[index]
            actual_x = actual_element.x
            expected_x = expected_element.x
            self.assertEqual(expected_x, actual_x)
    
    # endregion
    
    # endregion
    


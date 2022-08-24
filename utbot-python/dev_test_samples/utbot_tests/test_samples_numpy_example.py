import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import builtins
import samples.numpy_example


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable samples.numpy_example.numpy_operations
    # region
    def test_numpy_operations_throws_t(self):
        samples.numpy_example.numpy_operations(1)
        
        # raises numpy.AxisError
    
    def test_numpy_operations_throws_t1(self):
        samples.numpy_example.numpy_operations(0)
        
        # raises numpy.AxisError
    
    def test_numpy_operations_throws_t2(self):
        samples.numpy_example.numpy_operations({})
        
        # raises numpy.AxisError
    
    def test_numpy_operations_throws_t3(self):
        samples.numpy_example.numpy_operations({1: 0, 0: 0})
        
        # raises numpy.AxisError
    
    def test_numpy_operations_throws_t4(self):
        samples.numpy_example.numpy_operations({1: 1})
        
        # raises numpy.AxisError
    
    def test_numpy_operations_throws_t5(self):
        samples.numpy_example.numpy_operations({0: 1})
        
        # raises numpy.AxisError
    
    def test_numpy_operations_throws_t6(self):
        samples.numpy_example.numpy_operations({1: 1, 0: 0})
        
        # raises numpy.AxisError
    
    def test_numpy_operations_throws_t7(self):
        samples.numpy_example.numpy_operations({0: 1, 1: 1})
        
        # raises numpy.AxisError
    
    def test_numpy_operations_throws_t8(self):
        samples.numpy_example.numpy_operations({1: 1, 0: 1})
        
        # raises numpy.AxisError
    
    def test_numpy_operations_throws_t9(self):
        samples.numpy_example.numpy_operations({0: 1, 1: 0})
        
        # raises numpy.AxisError
    
    # endregion
    
    # endregion
    


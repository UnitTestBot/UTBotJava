import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import builtins
import samples.matrix
import copyreg
import types


class TestMatrix(unittest.TestCase):
    # region Test suites for executable samples.matrix.__mul__
    # region
    def test__mul__(self):
        matrix = samples.matrix.Matrix([])
        
        actual = matrix.__mul__(int('-1'))
        
        matrix1 = copyreg._reconstructor(samples.matrix.Matrix, builtins.object, None)
        matrix1.elements = []
        matrix1.dim = (0, 0)
        actual_elements = actual.elements
        expected_elements = matrix1.elements
        self.assertEqual(expected_elements, actual_elements)
        actual_dim = actual.dim
        expected_dim = matrix1.dim
        self.assertEqual(expected_dim, actual_dim)
    
    # endregion
    
    # endregion
    
    # region Test suites for executable samples.matrix.__matmul__
    # region
    def test__matmul__(self):
        matrix = samples.matrix.Matrix([])
        
        actual = matrix.__matmul__(0)
        
        self.assertEqual(None, actual)
    
    # endregion
    
    # endregion
    


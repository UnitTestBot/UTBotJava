import sys
sys.path.append('samples')
import matrix
import builtins
import copyreg
import types
import unittest


class TestMatrix(unittest.TestCase):
    # region Test suites for executable matrix.__add__
    # region
    def test__add__(self):
        matrix1 = matrix.Matrix([[float(-1), float('1.4'), float('nan'), float(-1)], [float(-1), float('1.4'), float('nan'), float(-1)], [7.3, float(1970), float(314), 7.3, float(-1)], [float(1970)], [float('nan'), 7.3, float(10 ** 23), float(314), float(10 ** 23), float(314)]])
        self1 = matrix.Matrix([[float(-1), float('1.4'), float('nan'), float(-1)], [float(-1), float('1.4'), float('nan'), float(-1)], [7.3, float(1970), float(314), 7.3, float(-1)], [float(1970)], [float('nan'), 7.3, float(10 ** 23), float(314), float(10 ** 23), float(314)]])
        
        actual = matrix1.__add__(self1)
        
        matrix2 = copyreg._reconstructor(matrix.Matrix, builtins.object, None)
        matrix2.dim = (5, 6)
        matrix2.elements = [[-2.0, 2.8, float('nan'), -2.0, 0, 0], [-2.0, 2.8, float('nan'), -2.0, 0, 0], [14.6, 3940.0, 628.0, 14.6, -2.0, 0], [3940.0, 0, 0, 0, 0, 0], [float('nan'), 14.6, 2e+23, 628.0, 2e+23, 628.0]]
        actual_dim = actual.dim
        expected_dim = matrix2.dim
        
        self.assertEqual(expected_dim, actual_dim)
        actual_elements = actual.elements
        expected_elements = matrix2.elements
        expected_list = expected_elements
        expected_length = len(expected_list)
        actual_length = len(actual_elements)
        
        self.assertEqual(expected_length, actual_length)
        
        self.assertTrue(isinstance(actual_elements, builtins.list))
    
    # endregion
    
    # endregion
    
    # region Test suites for executable matrix.__mul__
    # region
    def test__mul__(self):
        matrix1 = matrix.Matrix([])
        
        actual = matrix1.__mul__(123)
        
        matrix2 = copyreg._reconstructor(matrix.Matrix, builtins.object, None)
        matrix2.dim = (0, 0)
        matrix2.elements = []
        
        self.assertEqual(matrix2, actual)
    
    def test__mul__throws_t(self):
        matrix1 = matrix.Matrix([])
        self1 = matrix.Matrix([[0.0]])
        
        matrix1.__mul__(self1)
        
        # raises matrix.MatrixException
    
    # endregion
    
    # endregion
    
    # region Test suites for executable matrix.__matmul__
    # region
    def test__matmul__throws_t(self):
        matrix1 = matrix.Matrix([])
        
        matrix1.__matmul__(0)
        
        # raises matrix.MatrixException
    
    # endregion
    
    # endregion
    


import sys
sys.path.append('samples')
import unittest
import matrix
import builtins
import copyreg
import types


class TestMatrix(unittest.TestCase):
    # region Test suites for executable matrix.__add__
    # region
    def test__add__(self):
        matrix1 = matrix.Matrix([[float(314), 7.3], [float(314), float(314), float(10 ** 23), float(10 ** 23), float('nan'), float('+infinity'), float(10 ** 23)], [float('+infinity'), float(10 ** 23), 7.3], [float(-1), 0.0]])
        self1 = matrix.Matrix([[float(314), 7.3], [float(314), float(314), float(10 ** 23), float(10 ** 23), float('nan'), float('+infinity'), float(10 ** 23)], [float('+infinity'), float(10 ** 23), 7.3], [float(-1), 0.0]])
        
        actual = matrix1.__add__(self1)
        
        matrix2 = copyreg._reconstructor(matrix.Matrix, builtins.object, None)
        matrix2.elements = [[628.0, 14.6], [628.0, 628.0, 2e+23, 2e+23, float('nan'), float('inf'), 2e+23], [float('inf'), 2e+23, 14.6], [-2.0, 0.0]]
        matrix2.dim = (4, 2)
        self.assertEqual(matrix2, actual)
    
    # endregion
    
    # endregion
    
    # region Test suites for executable matrix.__mul__
    # region
    def test__mul__(self):
        matrix1 = matrix.Matrix([])
        
        actual = matrix1.__mul__(123)
        
        matrix2 = copyreg._reconstructor(matrix.Matrix, builtins.object, None)
        matrix2.elements = []
        matrix2.dim = (0, 0)
        self.assertEqual(matrix2, actual)
    
    def test__mul__throws_t(self):
        matrix1 = matrix.Matrix([])
        self1 = matrix.Matrix([[float('nan'), float(10 ** 23), float('1.4'), float('nan'), float(314)], [float('+infinity'), float(314), float(10 ** 23)], [float(1970)], [0.0, float('nan'), float(314), float(1970), float('1.4'), float(-1)], [float(1970)]])
        
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
    


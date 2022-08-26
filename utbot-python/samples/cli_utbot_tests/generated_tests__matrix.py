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
        matrix1 = matrix.Matrix([[7.3, float(314), 7.3, 7.3, 0.0, 7.3], [float(10 ** 23)], [float('1.4')], [float(-1), float(10 ** 23), float(-1), float('nan'), 7.3], [float(-1), float('+infinity'), float(10 ** 23), float(-1), 0.0], [float(314), float('+infinity'), float(-1), float('nan'), float(10 ** 23)], [float(10 ** 23)]])
        self1 = matrix.Matrix([[7.3, float(314), 7.3, 7.3, 0.0, 7.3], [float(10 ** 23)], [float('1.4')], [float(-1), float(10 ** 23), float(-1), float('nan'), 7.3], [float(-1), float('+infinity'), float(10 ** 23), float(-1), 0.0], [float(314), float('+infinity'), float(-1), float('nan'), float(10 ** 23)], [float(10 ** 23)]])
        
        actual = matrix1.__add__(self1)
        
        matrix2 = copyreg._reconstructor(matrix.Matrix, builtins.object, None)
        matrix2.elements = [[14.6, 628.0, 14.6, 14.6, 0.0, 14.6], [2e+23], [2.8], [-2.0, 2e+23, -2.0, float('nan'), 14.6], [-2.0, float('inf'), 2e+23, -2.0, 0.0], [628.0, float('inf'), -2.0, float('nan'), 2e+23], [2e+23]]
        matrix2.dim = (7, 6)
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
        self1 = matrix.Matrix([[0.0, float('nan'), float('+infinity'), float(-1), float('1.4'), float('+infinity')]])
        
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
    


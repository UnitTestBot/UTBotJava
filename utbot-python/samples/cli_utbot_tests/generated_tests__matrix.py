import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import matrix
import builtins
import copyreg
import types


class TestMatrix(unittest.TestCase):
    # region Test suites for executable matrix.__add__
    # region
    def test__add__(self):
        matrix1 = matrix.Matrix([[float(10 ** 23), float(314), 0.0, float(234)], [7.3, 0.0, float(-1), 7.3, 0.0, float(10 ** 23), 7.3, 7.3, float('1.4')], [float(10 ** 23), float(314), 0.0, float(234)], [float(1970), float('1.4'), float('1.4'), float(-1), float(314), 0.0, 0.0, float(10 ** 23), float(314)], [float('+infinity'), float(10 ** 23), float('1.4'), float('+infinity')], []])
        self1 = matrix.Matrix([[float(10 ** 23), float(314), 0.0, float(234)], [7.3, 0.0, float(-1), 7.3, 0.0, float(10 ** 23), 7.3, 7.3, float('1.4')], [float(10 ** 23), float(314), 0.0, float(234)], [float(1970), float('1.4'), float('1.4'), float(-1), float(314), 0.0, 0.0, float(10 ** 23), float(314)], [float('+infinity'), float(10 ** 23), float('1.4'), float('+infinity')], []])
        
        actual = matrix1.__add__(self1)
        
        matrix2 = copyreg._reconstructor(matrix.Matrix, builtins.object, None)
        matrix2.elements = [[2e+23, 628.0, 0.0, float(468)], [14.6, 0.0, -2.0, 14.6, 0.0, 2e+23, 14.6, 14.6, 2.8], [2e+23, 628.0, 0.0, float(468)], [3940.0, 2.8, 2.8, -2.0, 628.0, 0.0, 0.0, 2e+23, 628.0], [float('inf'), 2e+23, 2.8, float('inf')], []]
        matrix2.dim = (6, 4)
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
        self1 = matrix.Matrix([[float('1.4'), float('nan'), 7.3, float(1970), float(-1), float(-1), float(1970)], [float('nan'), float(1970), float(-1), float(-1), float('1.4'), float(10 ** 23)], [float('1.4'), float('nan'), 7.3, float(1970), float(-1), float(-1), float(1970)], [float('nan'), 7.3, float(10 ** 23), float(10 ** 23), 0.0, float(-1), float(10 ** 23), float(10 ** 23)], [float(-1), float(-1), float(1970), 7.3, 7.3, float('1.4'), float(-1), float(10 ** 23)], [float('nan'), float(10 ** 23), float(-1), float('nan'), float('1.4')], [float(314), 0.0, float(-1), float(-1), float(-1), 7.3, float(1970), 7.3, float(10 ** 23)], [float('nan'), float(1970), float(-1), float(-1), float('1.4'), float(10 ** 23)], [float('1.4'), float('nan'), 7.3, float(1970), float(-1), float(-1), float(1970)], [float('nan'), float('1.4'), float('nan'), 7.3, float(10 ** 23), float('1.4'), 0.0, 0.0]])
        
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
    


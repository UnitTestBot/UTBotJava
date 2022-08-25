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
        matrix1 = matrix.Matrix([[7.3, 7.3, float(1970), float(1970), 0.0, float(1970), float(10 ** 23)], [float(10 ** 23), float(1970), float(10 ** 23), float(-1), float(1970)], [7.3, 7.3, float(1970), float(1970), 0.0, float(1970), float(10 ** 23)], [7.3, 7.3, float(1970), float(1970), 0.0, float(1970), float(10 ** 23)], [float('1.4')], []])
        self1 = matrix.Matrix([[7.3, 7.3, float(1970), float(1970), 0.0, float(1970), float(10 ** 23)], [float(10 ** 23), float(1970), float(10 ** 23), float(-1), float(1970)], [7.3, 7.3, float(1970), float(1970), 0.0, float(1970), float(10 ** 23)], [7.3, 7.3, float(1970), float(1970), 0.0, float(1970), float(10 ** 23)], [float('1.4')], []])
        
        actual = matrix1.__add__(self1)
        
        matrix2 = copyreg._reconstructor(matrix.Matrix, builtins.object, None)
        matrix2.elements = [[14.6, 14.6, 3940.0, 3940.0, 0.0, 3940.0, 2e+23], [2e+23, 3940.0, 2e+23, -2.0, 3940.0], [14.6, 14.6, 3940.0, 3940.0, 0.0, 3940.0, 2e+23], [14.6, 14.6, 3940.0, 3940.0, 0.0, 3940.0, 2e+23], [2.8], []]
        matrix2.dim = (6, 7)
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
        self1 = matrix.Matrix([[float('nan')], [float(1970), float(10 ** 23), float('1.4'), 0.0, 7.3, float(1970)], [0.0, float('nan'), float(1970), float(-1), float('+infinity'), float(-1)], [float('+infinity'), float(1970), float('1.4'), float(1970)], [float(1970), float(1970), 7.3, 0.0, float(10 ** 23), float('1.4')]])
        
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
    


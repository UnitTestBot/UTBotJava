import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples/samples')
import unittest
import builtins
import matrix
import copyreg
import types


class TestMatrix(unittest.TestCase):
    # region Test suites for executable matrix.__mul__
    # region
    def test__mul__(self):
        matrix = matrix.Matrix([])
        
        actual = matrix.__mul__(int('-1'))
        
        matrix1 = copyreg._reconstructor(matrix.Matrix, builtins.object, None)
        matrix1.elements = []
        matrix1.dim = (0, 0)
        actual_elements = actual.elements
        expected_elements = matrix1.elements
        self.assertEqual(expected_elements, actual_elements)
        actual_dim = actual.dim
        expected_dim = matrix1.dim
        self.assertEqual(expected_dim, actual_dim)
    
    def test__mul__throws_t(self):
        matrix = matrix.Matrix([])
        self1 = matrix.Matrix([[float(0.0), float(7.3)], [float(10 ** 23), float('+NAn'), float('+infinity'), float('+NAn'), float('+infinity'), float(0.0), float(0.0), float('+NAn'), float('+NAn')], [float(-1), float(314), float(314), float(1970), float(314)], [float(10 ** 23), float('+NAn'), float('+infinity'), float('+NAn'), float('+infinity'), float(0.0), float(0.0), float('+NAn'), float('+NAn')]])
        
        matrix.__mul__(self1)
        
        # raises matrix.MatrixException
    
    # endregion
    
    # endregion
    
    # region Test suites for executable matrix.__matmul__
    # region
    def test__matmul__throws_t(self):
        matrix = matrix.Matrix([])
        
        matrix.__matmul__(0)
        
        # raises matrix.MatrixException
    
    # endregion
    
    # endregion
    


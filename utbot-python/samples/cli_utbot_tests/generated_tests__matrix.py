import sys
sys.path.append('samples')
import matrix
import builtins
import types
import copyreg
import unittest


class TestMatrix(unittest.TestCase):
    # region Test suites for executable matrix.__add__
    
    # region
    
    def test__add__(self):
        matrix1 = matrix.Matrix([[float('nan'), 0.0, float(1970), 7.3, float('nan')], [float(10 ** 23), float('1.4'), float(-1), float(-1), float('nan'), float('nan'), float(1970)], [float('nan'), 0.0, float(1970), 7.3, float('nan')], [float(314), float(-1), float('nan'), float(1970), 7.3, float(-1), float(-1)], [float('nan'), 0.0, float(1970), 7.3, float('nan')], [float('nan')]])
        self1 = matrix.Matrix([[float('nan'), 0.0, float(1970), 7.3, float('nan')], [float(10 ** 23), float('1.4'), float(-1), float(-1), float('nan'), float('nan'), float(1970)], [float('nan'), 0.0, float(1970), 7.3, float('nan')], [float(314), float(-1), float('nan'), float(1970), 7.3, float(-1), float(-1)], [float('nan'), 0.0, float(1970), 7.3, float('nan')], [float('nan')]])
        
        actual = matrix1.__add__(self1)
        
        matrix2 = copyreg._reconstructor(matrix.Matrix, builtins.object, None)
        matrix2.dim = (6, 7)
        matrix2.elements = [[float('nan'), 0.0, 3940.0, 14.6, float('nan'), 0, 0], [2e+23, 2.8, -2.0, -2.0, float('nan'), float('nan'), 3940.0], [float('nan'), 0.0, 3940.0, 14.6, float('nan'), 0, 0], [628.0, -2.0, float('nan'), 3940.0, 14.6, -2.0, -2.0], [float('nan'), 0.0, 3940.0, 14.6, float('nan'), 0, 0], [float('nan'), 0, 0, 0, 0, 0, 0]]
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
    


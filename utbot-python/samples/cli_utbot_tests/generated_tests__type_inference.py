import sys
sys.path.append('samples')
import builtins
import type_inference
import unittest


class TestTopLevelFunctions(unittest.TestCase):
    # region Test suites for executable type_inference.type_inference
    # region
    def test_type_inference_by_fuzzer(self):
        actual = type_inference.type_inference(0, str(), str(b'\x80'), [], {})
        
        self.assertEqual([0, 0, 0, 0, 0, 0, 0, 0, 0, ''], actual)
    
    # endregion
    
    # endregion
    


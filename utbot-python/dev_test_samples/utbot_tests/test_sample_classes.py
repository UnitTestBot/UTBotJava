import sys
sys.path.append('/home/vyacheslav/Desktop/utbot/UTBotJava/utbot-python/samples')
import unittest
import sample_classes
import builtins


class TestB(unittest.TestCase):
    # region Test suites for executable sample_classes.sqrt
    # region
    def test_sqrt(self):
        b = sample_classes.B(complex('1j'))
        
        actual = b.sqrt()
        
        self.assertEqual(complex('(0.7071067811865476+0.7071067811865475j)'), actual)
    
    def test_sqrt_throws_t(self):
        b = sample_classes.B(complex(1.0, float('inf')))
        
        b.sqrt()
        
        # raises builtins.OverflowError
    
    def test_sqrt1(self):
        b = sample_classes.B(complex(1.0, 10.0))
        
        actual = b.sqrt()
        
        self.assertEqual(complex('(2.350518625869713+2.1271901209248893j)'), actual)
    
    def test_sqrt_throws_t1(self):
        b = sample_classes.B(complex(float('inf'), -1))
        
        b.sqrt()
        
        # raises builtins.OverflowError
    
    def test_sqrt_throws_t2(self):
        b = sample_classes.B(complex('1' * 500))
        
        b.sqrt()
        
        # raises builtins.OverflowError
    
    def test_sqrt2(self):
        b = sample_classes.B(complex(0.0, float('nan')))
        
        actual = b.sqrt()
        
        self.assertTrue(isinstance(actual, builtins.complex))
    
    def test_sqrt3(self):
        b = sample_classes.B(complex('(1+2j)'))
        
        actual = b.sqrt()
        
        self.assertEqual(complex('(1.272019649514069+0.7861513777574233j)'), actual)
    
    def test_sqrt4(self):
        b = sample_classes.B(complex(0.0j, 3.14))
        
        actual = b.sqrt()
        
        self.assertEqual(complex('(1.2529964086141667+1.2529964086141667j)'), actual)
    
    def test_sqrt_throws_t3(self):
        b = sample_classes.B(complex(float('inf'), float('inf')))
        
        b.sqrt()
        
        # raises builtins.OverflowError
    
    # endregion
    
    # endregion
    


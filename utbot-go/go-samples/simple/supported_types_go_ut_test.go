package simple

import (
	"github.com/pmezard/go-difflib/difflib"
	"github.com/stretchr/testify/assert"
	"go-samples/simple/nested"
	"math"
	"testing"
)

func TestWithoutParametersAndReturnValuesByUtGoFuzzer(t *testing.T) {
	assert.NotPanics(t, func() { WithoutParametersAndReturnValues() })
}

func TestIntByUtGoFuzzer1(t *testing.T) {
	actualVal := Int(0)

	assert.Equal(t, 0, actualVal)
}

func TestIntByUtGoFuzzer2(t *testing.T) {
	actualVal := Int(-9223372036854775808)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestInt8ByUtGoFuzzer1(t *testing.T) {
	actualVal := Int8(0)

	assert.Equal(t, int8(0), actualVal)
}

func TestInt8ByUtGoFuzzer2(t *testing.T) {
	actualVal := Int8(-128)

	assert.Equal(t, int8(-128), actualVal)
}

func TestInt16ByUtGoFuzzer1(t *testing.T) {
	actualVal := Int16(0)

	assert.Equal(t, int16(0), actualVal)
}

func TestInt16ByUtGoFuzzer2(t *testing.T) {
	actualVal := Int16(-32768)

	assert.Equal(t, int16(-32768), actualVal)
}

func TestInt32ByUtGoFuzzer1(t *testing.T) {
	actualVal := Int32(0)

	assert.Equal(t, int32(0), actualVal)
}

func TestInt32ByUtGoFuzzer2(t *testing.T) {
	actualVal := Int32(-2147483648)

	assert.Equal(t, int32(-2147483648), actualVal)
}

func TestInt64ByUtGoFuzzer1(t *testing.T) {
	actualVal := Int64(0)

	assert.Equal(t, int64(0), actualVal)
}

func TestInt64ByUtGoFuzzer2(t *testing.T) {
	actualVal := Int64(-9223372036854775808)

	assert.Equal(t, int64(-9223372036854775808), actualVal)
}

func TestUintByUtGoFuzzer(t *testing.T) {
	actualVal := Uint(0)

	assert.Equal(t, uint(0), actualVal)
}

func TestUint8ByUtGoFuzzer(t *testing.T) {
	actualVal := Uint8(0)

	assert.Equal(t, uint8(0), actualVal)
}

func TestUint16ByUtGoFuzzer(t *testing.T) {
	actualVal := Uint16(0)

	assert.Equal(t, uint16(0), actualVal)
}

func TestUint32ByUtGoFuzzer(t *testing.T) {
	actualVal := Uint32(0)

	assert.Equal(t, uint32(0), actualVal)
}

func TestUint64ByUtGoFuzzer(t *testing.T) {
	actualVal := Uint64(0)

	assert.Equal(t, uint64(0), actualVal)
}

func TestUintPtrByUtGoFuzzer(t *testing.T) {
	actualVal := UintPtr(0)

	assert.Equal(t, uintptr(0), actualVal)
}

func TestFloat32ByUtGoFuzzer(t *testing.T) {
	actualVal := Float32(0.73096776)

	assert.Equal(t, float32(0.73096776), actualVal)
}

func TestFloat64ByUtGoFuzzer(t *testing.T) {
	actualVal := Float64(0.8314409887870612)

	assert.Equal(t, 0.8314409887870612, actualVal)
}

func TestComplex64ByUtGoFuzzer(t *testing.T) {
	actualVal := Complex64(complex(0.6063452, 0.6063452))

	assert.Equal(t, complex(float32(0.6063452), float32(0.6063452)), actualVal)
}

func TestComplex128ByUtGoFuzzer(t *testing.T) {
	actualVal := Complex128(complex(0.6374174253501083, 0.6374174253501083))

	assert.Equal(t, complex(0.6374174253501083, 0.6374174253501083), actualVal)
}

func TestByteByUtGoFuzzer(t *testing.T) {
	actualVal := Byte(0)

	assert.Equal(t, uint8(0), actualVal)
}

func TestRuneByUtGoFuzzer(t *testing.T) {
	actualVal := Rune(2147483647)

	assert.Equal(t, int32(2147483647), actualVal)
}

func TestStringByUtGoFuzzer(t *testing.T) {
	actualVal := String("hello")

	assert.Equal(t, "hello", actualVal)
}

func TestBoolByUtGoFuzzer(t *testing.T) {
	actualVal := Bool(true)

	assert.Equal(t, true, actualVal)
}

func TestStructByUtGoFuzzer(t *testing.T) {
	actualVal := Struct(Structure{int: -1, int8: 1, int16: 32767, int32: -1, int64: -1, uint: 18446744073709551615, uint8: 0, uint16: 1, uint32: 0, uint64: 18446744073709551615, uintptr: 18446744073709551615, float32: 0.550437, float64: 0.11700660880722513, complex64: complex(0.550437, 0.550437), complex128: complex(0.11700660880722513, 0.11700660880722513), byte: 0, rune: -1, string: "", bool: false})

	assert.Equal(t, Structure{int: -1, int8: 1, int16: 32767, int32: -1, int64: -1, uint: 18446744073709551615, uint8: 0, uint16: 1, uint32: 0, uint64: 18446744073709551615, uintptr: 18446744073709551615, float32: 0.550437, float64: 0.11700660880722513, complex64: complex(float32(0.550437), float32(0.550437)), complex128: complex(0.11700660880722513, 0.11700660880722513), byte: 0, rune: -1, string: "", bool: false}, actualVal)
}

func TestStructWithNanByUtGoFuzzer(t *testing.T) {
	actualVal := StructWithNan(Structure{int: -1, int8: 1, int16: 32767, int32: -1, int64: -1, uint: 18446744073709551615, uint8: 0, uint16: 1, uint32: 0, uint64: 18446744073709551615, uintptr: 18446744073709551615, float32: 0.7815346, float64: 0.3332183994766498, complex64: complex(0.7815346, 0.7815346), complex128: complex(0.3332183994766498, 0.3332183994766498), byte: 0, rune: -1, string: "", bool: false})

	assert.NotEqual(t, Structure{int: -1, int8: 1, int16: 32767, int32: -1, int64: -1, uint: 18446744073709551615, uint8: 0, uint16: 1, uint32: 0, uint64: 18446744073709551615, uintptr: 18446744073709551615, float32: 0.7815346, float64: math.NaN(), complex64: complex(float32(0.7815346), float32(0.7815346)), complex128: complex(0.3332183994766498, 0.3332183994766498), byte: 0, rune: -1, string: "", bool: false}, actualVal)
}

func TestArrayOfIntByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfInt([10]int{-1, 0, 0, 0, 0, 0, 0, 0, 0, 0})

	assert.Equal(t, [10]int{-1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, actualVal)
}

func TestArrayOfUintPtrByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfUintPtr([10]uintptr{0, 0, 0, 0, 0, 0, 0, 0, 0, 0})

	assert.Equal(t, [10]uintptr{0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, actualVal)
}

func TestArrayOfStringByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfString([10]string{"hello", "", "", "", "", "", "", "", "", ""})

	assert.Equal(t, [10]string{"hello", "", "", "", "", "", "", "", "", ""}, actualVal)
}

func TestArrayOfStructsByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfStructs([10]Structure{{int: -1, int8: 0, int16: -1, int32: -1, int64: -9223372036854775808, uint: 1, uint8: 0, uint16: 65535, uint32: 1, uint64: 18446744073709551615, uintptr: 1, float32: 0.38518918, float64: 0.6130357680446138, complex64: complex(0.38518918, 0.38518918), complex128: complex(0.6130357680446138, 0.6130357680446138), byte: 255, rune: 2147483647, string: "", bool: false}, {}, {}, {}, {}, {}, {}, {}, {}, {}})

	assert.Equal(t, [10]Structure{{int: -1, int8: 0, int16: -1, int32: -1, int64: -9223372036854775808, uint: 1, uint8: 0, uint16: 65535, uint32: 1, uint64: 18446744073709551615, uintptr: 1, float32: 0.38518918, float64: 0.6130357680446138, complex64: complex(float32(0.38518918), float32(0.38518918)), complex128: complex(0.6130357680446138, 0.6130357680446138), byte: 255, rune: 2147483647, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}}, actualVal)
}

func TestArrayOfStructsWithNanByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfStructsWithNan([10]Structure{{int: 1, int8: 0, int16: -1, int32: -1, int64: -9223372036854775808, uint: 1, uint8: 0, uint16: 65535, uint32: 1, uint64: 18446744073709551615, uintptr: 1, float32: 0.9828195, float64: 0.8791825178724801, complex64: complex(0.9828195, 0.9828195), complex128: complex(0.8791825178724801, 0.8791825178724801), byte: 255, rune: 2147483647, string: "", bool: false}, {}, {}, {}, {}, {}, {}, {}, {}, {}})

	assert.NotEqual(t, [10]Structure{{int: 1, int8: 0, int16: -1, int32: -1, int64: -9223372036854775808, uint: 1, uint8: 0, uint16: 65535, uint32: 1, uint64: 18446744073709551615, uintptr: 1, float32: 0.9828195, float64: math.NaN(), complex64: complex(float32(0.9828195), float32(0.9828195)), complex128: complex(0.8791825178724801, 0.8791825178724801), byte: 255, rune: 2147483647, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}}, actualVal)
}

func TestArrayOfArrayOfUintByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfArrayOfUint([5][5]uint{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}})

	assert.Equal(t, [5][5]uint{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}, actualVal)
}

func TestArrayOfArrayOfStructsByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfArrayOfStructs([5][5]Structure{{{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}})

	assert.Equal(t, [5][5]Structure{{{int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}}, {{int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}}, {{int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}}, {{int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}}, {{int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}}}, actualVal)
}

func TestArrayOfSliceOfUintByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfSliceOfUint([5][]uint{{}, {}, {}, {}, {}})

	assert.Equal(t, [5][]uint{{}, {}, {}, {}, {}}, actualVal)
}

func TestReturnErrorOrNilByUtGoFuzzer1(t *testing.T) {
	actualErr := returnErrorOrNil(0)

	assert.Nil(t, actualErr)
}

func TestReturnErrorOrNilWithNonNilErrorByUtGoFuzzer2(t *testing.T) {
	actualErr := returnErrorOrNil(8)

	assert.ErrorContains(t, actualErr, "error")
}

func TestExternalStructByUtGoFuzzer(t *testing.T) {
	actualVal := ExternalStruct(difflib.Match{A: 9223372036854775807, B: -1, Size: -9223372036854775808}, Structure{int: -1, int8: 1, int16: -32768, int32: 2147483647, int64: 1, uint: 1, uint8: 1, uint16: 1, uint32: 1, uint64: 18446744073709551615, uintptr: 18446744073709551615, float32: 0.94124913, float64: 0.17597680203548016, complex64: complex(0.94124913, 0.94124913), complex128: complex(0.17597680203548016, 0.17597680203548016), byte: 1, rune: 0, string: "", bool: false})

	assert.Equal(t, Structure{int: -1, int8: 1, int16: -32768, int32: 2147483647, int64: 1, uint: 1, uint8: 1, uint16: 1, uint32: 1, uint64: 18446744073709551615, uintptr: 18446744073709551615, float32: 0.94124913, float64: 0.17597680203548016, complex64: complex(float32(0.94124913), float32(0.94124913)), complex128: complex(0.17597680203548016, 0.17597680203548016), byte: 1, rune: 0, string: "", bool: false}, actualVal)
}

func TestExternalStructWithAliasByUtGoFuzzer(t *testing.T) {
	actualVal := ExternalStructWithAlias(difflib.Match{A: 9223372036854775807, B: -1, Size: -9223372036854775808})

	assert.Equal(t, difflib.Match{A: 9223372036854775807, B: -1, Size: -9223372036854775808}, actualVal)
}

func TestSliceOfIntByUtGoFuzzer(t *testing.T) {
	actualVal := SliceOfInt([]int{-1})

	assert.Equal(t, []int{-1}, actualVal)
}

func TestSliceOfUintPtrByUtGoFuzzer(t *testing.T) {
	actualVal := SliceOfUintPtr([]uintptr{0})

	assert.Equal(t, []uintptr{0}, actualVal)
}

func TestSliceOfStringByUtGoFuzzer(t *testing.T) {
	actualVal := SliceOfString([]string{"hello"})

	assert.Equal(t, []string{"hello"}, actualVal)
}

func TestSliceOfStructsByUtGoFuzzer(t *testing.T) {
	actualVal := SliceOfStructs([]Structure{{int: -1, int8: 0, int16: -1, int32: -1, int64: -9223372036854775808, uint: 1, uint8: 0, uint16: 65535, uint32: 1, uint64: 18446744073709551615, uintptr: 1, float32: 0.31293595, float64: 0.12889715087377673, complex64: complex(0.31293595, 0.31293595), complex128: complex(0.12889715087377673, 0.12889715087377673), byte: 255, rune: 2147483647, string: "", bool: false}})

	assert.Equal(t, []Structure{{int: -1, int8: 0, int16: -1, int32: -1, int64: -9223372036854775808, uint: 1, uint8: 0, uint16: 65535, uint32: 1, uint64: 18446744073709551615, uintptr: 1, float32: 0.31293595, float64: 0.12889715087377673, complex64: complex(float32(0.31293595), float32(0.31293595)), complex128: complex(0.12889715087377673, 0.12889715087377673), byte: 255, rune: 2147483647, string: "", bool: false}}, actualVal)
}

func TestSliceOfStructsWithNanByUtGoFuzzer(t *testing.T) {
	actualVal := SliceOfStructsWithNan([]Structure{{int: 1, int8: 0, int16: -1, int32: -1, int64: -9223372036854775808, uint: 1, uint8: 0, uint16: 65535, uint32: 1, uint64: 18446744073709551615, uintptr: 1, float32: 0.14660162, float64: 0.7051747444754559, complex64: complex(0.14660162, 0.14660162), complex128: complex(0.7051747444754559, 0.7051747444754559), byte: 255, rune: 2147483647, string: "", bool: false}})

	assert.NotEqual(t, []Structure{{int: 1, int8: 0, int16: -1, int32: -1, int64: -9223372036854775808, uint: 1, uint8: 0, uint16: 65535, uint32: 1, uint64: 18446744073709551615, uintptr: 1, float32: 0.14660162, float64: math.NaN(), complex64: complex(float32(0.14660162), float32(0.14660162)), complex128: complex(0.7051747444754559, 0.7051747444754559), byte: 255, rune: 2147483647, string: "", bool: false}}, actualVal)
}

func TestSliceOfSliceOfByteByUtGoFuzzer(t *testing.T) {
	actualVal := SliceOfSliceOfByte([][]byte{{}})

	assert.Equal(t, [][]byte{{}}, actualVal)
}

func TestSliceOfSliceOfStructsByUtGoFuzzer(t *testing.T) {
	actualVal := SliceOfSliceOfStructs([][]Structure{{}})

	assert.Equal(t, [][]Structure{{}}, actualVal)
}

func TestSliceOfArrayOfIntByUtGoFuzzer(t *testing.T) {
	actualVal := SliceOfArrayOfInt([][5]int{{0, 0, 0, 0, 0}})

	assert.Equal(t, [][5]int{{0, 0, 0, 0, 0}}, actualVal)
}

func TestExportedStructWithEmbeddedUnexportedStructByUtGoFuzzer(t *testing.T) {
	actualVal := ExportedStructWithEmbeddedUnexportedStruct(nested.ExportedStruct{})

	assert.Equal(t, nested.ExportedStruct{}, actualVal)
}

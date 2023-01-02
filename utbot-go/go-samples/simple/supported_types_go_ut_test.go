package simple

import (
	"github.com/stretchr/testify/assert"
	"math"
	"testing"
)

func TestIntByUtGoFuzzer1(t *testing.T) {
	actualVal := Int(2147483647)

	assert.Equal(t, 2147483647, actualVal)
}

func TestIntByUtGoFuzzer2(t *testing.T) {
	actualVal := Int(-2147483648)

	assert.Equal(t, 2147483648, actualVal)
}

func TestInt8ByUtGoFuzzer1(t *testing.T) {
	actualVal := Int8(127)

	assert.Equal(t, int8(127), actualVal)
}

func TestInt8ByUtGoFuzzer2(t *testing.T) {
	actualVal := Int8(-128)

	assert.Equal(t, int8(-128), actualVal)
}

func TestInt16ByUtGoFuzzer1(t *testing.T) {
	actualVal := Int16(32767)

	assert.Equal(t, int16(32767), actualVal)
}

func TestInt16ByUtGoFuzzer2(t *testing.T) {
	actualVal := Int16(-32768)

	assert.Equal(t, int16(-32768), actualVal)
}

func TestInt32ByUtGoFuzzer1(t *testing.T) {
	actualVal := Int32(2147483647)

	assert.Equal(t, int32(2147483647), actualVal)
}

func TestInt32ByUtGoFuzzer2(t *testing.T) {
	actualVal := Int32(-2147483648)

	assert.Equal(t, int32(-2147483648), actualVal)
}

func TestInt64ByUtGoFuzzer(t *testing.T) {
	actualVal := Int64(2147483647)

	assert.Equal(t, int64(2147483647), actualVal)
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
	actualVal := UintPtr(1)

	assert.Equal(t, uintptr(1), actualVal)
}

func TestFloat32ByUtGoFuzzer(t *testing.T) {
	actualVal := Float32(float32(math.MaxFloat32))

	assert.Equal(t, float32(3.4028235e38), actualVal)
}

func TestFloat64ByUtGoFuzzer(t *testing.T) {
	actualVal := Float64(math.MaxFloat64)

	assert.Equal(t, 1.7976931348623157e308, actualVal)
}

func TestComplex64ByUtGoFuzzer(t *testing.T) {
	actualVal := Complex64(complex(float32(math.MaxFloat32), float32(math.SmallestNonzeroFloat32)))

	assert.Equal(t, complex(float32(3.4028235e38), float32(1.4e-45)), actualVal)
}

func TestComplex128ByUtGoFuzzer(t *testing.T) {
	actualVal := Complex128(complex(math.MaxFloat64, math.SmallestNonzeroFloat64))

	assert.Equal(t, complex(1.7976931348623157e308, 4.9e-324), actualVal)
}

func TestByteByUtGoFuzzer(t *testing.T) {
	actualVal := Byte(0)

	assert.Equal(t, byte(0), actualVal)
}

func TestRuneByUtGoFuzzer(t *testing.T) {
	actualVal := Rune(2147483647)

	assert.Equal(t, rune(2147483647), actualVal)
}

func TestStringByUtGoFuzzer(t *testing.T) {
	actualVal := String("")

	assert.Equal(t, "", actualVal)
}

func TestBoolByUtGoFuzzer(t *testing.T) {
	actualVal := Bool(true)

	assert.Equal(t, true, actualVal)
}

func TestStructByUtGoFuzzer(t *testing.T) {
	actualVal := Struct(Structure{int: 2147483647, int8: 127, int16: -1, int32: -2147483648, int64: 4294967295, uint: 4294967295, uint8: 0, uint16: 1, uint32: 4294967295, uint64: 18446744073709551615, uintptr: 0, float32: 0.0, float64: 1.1, complex64: complex(float32(1.1), float32(math.Inf(1))), complex128: complex(math.Inf(-1), math.MaxFloat64), byte: 1, rune: 2147483647, string: "hello", bool: false})

	assert.Equal(t, Structure{int: 2147483647, int8: 127, int16: -1, int32: -2147483648, int64: 4294967295, uint: 4294967295, uint8: 0, uint16: 1, uint32: 4294967295, uint64: 18446744073709551615, uintptr: 0, float32: 0.0, float64: 1.1, complex64: complex(float32(1.1), float32(math.Inf(1))), complex128: complex(math.Inf(-1), 1.7976931348623157e308), byte: 1, rune: 2147483647, string: "hello", bool: false}, actualVal)
}

func TestStructWithNanByUtGoFuzzer(t *testing.T) {
	actualVal := StructWithNan(Structure{int: 2147483647, int8: 127, int16: -1, int32: -2147483648, int64: 4294967295, uint: 4294967295, uint8: 0, uint16: 1, uint32: 4294967295, uint64: 18446744073709551615, uintptr: 0, float32: 0.0, float64: 1.1, complex64: complex(float32(1.1), float32(math.Inf(1))), complex128: complex(math.Inf(-1), math.MaxFloat64), byte: 1, rune: 2147483647, string: "hello", bool: false})

	assert.NotEqual(t, Structure{int: 2147483647, int8: 127, int16: -1, int32: -2147483648, int64: 4294967295, uint: 4294967295, uint8: 0, uint16: 1, uint32: 4294967295, uint64: 18446744073709551615, uintptr: 0, float32: 0.0, float64: math.NaN(), complex64: complex(float32(1.1), float32(math.Inf(1))), complex128: complex(math.Inf(-1), 1.7976931348623157e308), byte: 1, rune: 2147483647, string: "hello", bool: false}, actualVal)
}

func TestArrayOfIntByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfInt([10]int{-1, 0, 0, 0, 0, 0, 0, 0, 0, 0})

	assert.Equal(t, [10]int{-1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, actualVal)
}

func TestArrayOfStructsByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfStructs([10]Structure{{int: -1, int8: -1, int16: 1, int32: -1, int64: 2147483648, uint: 1, uint8: 0, uint16: 65535, uint32: 4294967295, uint64: 18446744073709551615, uintptr: 1, float32: 1.1, float64: math.Inf(-1), complex64: complex(float32(-1.1), float32(math.NaN())), complex128: complex(math.NaN(), math.MaxFloat64), byte: 255, rune: 0, string: "   ", bool: true}, {}, {}, {}, {}, {}, {}, {}, {}, {}})

	assert.NotEqual(t, [10]Structure{{int: -1, int8: -1, int16: 1, int32: -1, int64: 2147483648, uint: 1, uint8: 0, uint16: 65535, uint32: 4294967295, uint64: 18446744073709551615, uintptr: 1, float32: 1.1, float64: math.Inf(-1), complex64: complex(float32(-1.1), float32(math.NaN())), complex128: complex(math.NaN(), 1.7976931348623157e308), byte: 255, rune: 0, string: "   ", bool: true}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}}, actualVal)
}

func TestArrayOfStructsWithNanByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfStructsWithNan([10]Structure{{int: -1, int8: -1, int16: 1, int32: -1, int64: 2147483648, uint: 1, uint8: 0, uint16: 65535, uint32: 4294967295, uint64: 18446744073709551615, uintptr: 1, float32: 1.1, float64: math.Inf(-1), complex64: complex(float32(-1.1), float32(math.NaN())), complex128: complex(math.NaN(), math.MaxFloat64), byte: 255, rune: 0, string: "   ", bool: true}, {}, {}, {}, {}, {}, {}, {}, {}, {}})

	assert.NotEqual(t, [10]Structure{{int: -1, int8: -1, int16: 1, int32: -1, int64: 2147483648, uint: 1, uint8: 0, uint16: 65535, uint32: 4294967295, uint64: 18446744073709551615, uintptr: 1, float32: 1.1, float64: math.NaN(), complex64: complex(float32(-1.1), float32(math.NaN())), complex128: complex(math.NaN(), 1.7976931348623157e308), byte: 255, rune: 0, string: "   ", bool: true}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}, {int: 0, int8: 0, int16: 0, int32: 0, int64: 0, uint: 0, uint8: 0, uint16: 0, uint32: 0, uint64: 0, uintptr: 0, float32: 0.0, float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: 0, rune: 0, string: "", bool: false}}, actualVal)
}

func TestReturnErrorOrNilWithNonNilErrorByUtGoFuzzer1(t *testing.T) {
	actualErr := returnErrorOrNil(2147483647)

	assert.ErrorContains(t, actualErr, "error")
}

func TestReturnErrorOrNilByUtGoFuzzer2(t *testing.T) {
	actualErr := returnErrorOrNil(0)

	assert.Nil(t, actualErr)
}

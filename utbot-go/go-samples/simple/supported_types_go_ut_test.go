package simple

import (
	"github.com/pmezard/go-difflib/difflib"
	"github.com/stretchr/testify/assert"
	"go-samples/simple/nested"
	"math"
	"testing"
)

func TestWithoutParametersAndReturnValuesByUtGoFuzzer(t *testing.T) {
	assert.NotPanics(t, func() {
		WithoutParametersAndReturnValues()
	})
}

func TestIntByUtGoFuzzer1(t *testing.T) {
	n := 1024

	actualVal := Int(n)

	expectedVal := 1024

	assert.Equal(t, expectedVal, actualVal)
}

func TestIntByUtGoFuzzer2(t *testing.T) {
	n := -9223372036854774784

	actualVal := Int(n)

	expectedVal := 9223372036854774784

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt8ByUtGoFuzzer1(t *testing.T) {
	n := int8(2)

	actualVal := Int8(n)

	expectedVal := int8(2)

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt8ByUtGoFuzzer2(t *testing.T) {
	n := int8(-126)

	actualVal := Int8(n)

	expectedVal := int8(126)

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt16ByUtGoFuzzer1(t *testing.T) {
	n := int16(4)

	actualVal := Int16(n)

	expectedVal := int16(4)

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt16ByUtGoFuzzer2(t *testing.T) {
	n := int16(-32764)

	actualVal := Int16(n)

	expectedVal := int16(32764)

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt32ByUtGoFuzzer1(t *testing.T) {
	n := int32(32)

	actualVal := Int32(n)

	expectedVal := int32(32)

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt32ByUtGoFuzzer2(t *testing.T) {
	n := int32(-2147483616)

	actualVal := Int32(n)

	expectedVal := int32(2147483616)

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt64ByUtGoFuzzer1(t *testing.T) {
	n := int64(1024)

	actualVal := Int64(n)

	expectedVal := int64(1024)

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt64ByUtGoFuzzer2(t *testing.T) {
	n := int64(-9223372036854774784)

	actualVal := Int64(n)

	expectedVal := int64(9223372036854774784)

	assert.Equal(t, expectedVal, actualVal)
}

func TestUintByUtGoFuzzer(t *testing.T) {
	n := uint(1024)

	actualVal := Uint(n)

	expectedVal := uint(1024)

	assert.Equal(t, expectedVal, actualVal)
}

func TestUint8ByUtGoFuzzer(t *testing.T) {
	n := uint8(2)

	actualVal := Uint8(n)

	expectedVal := uint8(2)

	assert.Equal(t, expectedVal, actualVal)
}

func TestUint16ByUtGoFuzzer(t *testing.T) {
	n := uint16(4)

	actualVal := Uint16(n)

	expectedVal := uint16(4)

	assert.Equal(t, expectedVal, actualVal)
}

func TestUint32ByUtGoFuzzer(t *testing.T) {
	n := uint32(32)

	actualVal := Uint32(n)

	expectedVal := uint32(32)

	assert.Equal(t, expectedVal, actualVal)
}

func TestUint64ByUtGoFuzzer(t *testing.T) {
	n := uint64(1024)

	actualVal := Uint64(n)

	expectedVal := uint64(1024)

	assert.Equal(t, expectedVal, actualVal)
}

func TestUintPtrByUtGoFuzzer(t *testing.T) {
	n := uintptr(1024)

	actualVal := UintPtr(n)

	expectedVal := uintptr(1024)

	assert.Equal(t, expectedVal, actualVal)
}

func TestFloat32ByUtGoFuzzer(t *testing.T) {
	n := float32(-0.59754527)

	actualVal := Float32(n)

	expectedVal := float32(-0.59754527)

	assert.Equal(t, expectedVal, actualVal)
}

func TestFloat64ByUtGoFuzzer(t *testing.T) {
	n := -0.7815346320453048

	actualVal := Float64(n)

	expectedVal := -0.7815346320453048

	assert.Equal(t, expectedVal, actualVal)
}

func TestComplex64ByUtGoFuzzer(t *testing.T) {
	n := complex(float32(0.25277615), float32(9.874068e-4))

	actualVal := Complex64(n)

	expectedVal := complex(float32(0.25277615), float32(9.874068e-4))

	assert.Equal(t, expectedVal, actualVal)
}

func TestComplex128ByUtGoFuzzer(t *testing.T) {
	n := complex(0.3851891847407185, 0.024074324046294907)

	actualVal := Complex128(n)

	expectedVal := complex(0.3851891847407185, 0.024074324046294907)

	assert.Equal(t, expectedVal, actualVal)
}

func TestByteByUtGoFuzzer(t *testing.T) {
	n := byte(2)

	actualVal := Byte(n)

	expectedVal := byte(2)

	assert.Equal(t, expectedVal, actualVal)
}

func TestRuneByUtGoFuzzer(t *testing.T) {
	n := rune(2147483615)

	actualVal := Rune(n)

	expectedVal := rune(2147483615)

	assert.Equal(t, expectedVal, actualVal)
}

func TestStringByUtGoFuzzer(t *testing.T) {
	n := "3hllo"

	actualVal := String(n)

	expectedVal := "3hllo"

	assert.Equal(t, expectedVal, actualVal)
}

func TestBoolByUtGoFuzzer(t *testing.T) {
	n := false

	actualVal := Bool(n)

	assert.False(t, actualVal)
}

func TestStructByUtGoFuzzer(t *testing.T) {
	s := Structure{int: -9223372036854775808, int8: int8(127), int16: int16(32767), int32: int32(0), int64: int64(9223372036854775807), uint: uint(18446744073709551615), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.9848415), float64: 0.9828195255872982, complex64: complex(float32(0.9848415), float32(0.9848415)), complex128: complex(0.9828195255872982, 0.9828195255872982), byte: byte(0), rune: rune(0), string: "hello", bool: false}

	actualVal := Struct(s)

	expectedVal := Structure{int: -9223372036854775808, int8: int8(127), int16: int16(32767), int32: int32(0), int64: int64(9223372036854775807), uint: uint(18446744073709551615), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.9848415), float64: 0.9828195255872982, complex64: complex(float32(0.9848415), float32(0.9848415)), complex128: complex(0.9828195255872982, 0.9828195255872982), byte: byte(0), rune: rune(0), string: "hello", bool: false}

	assert.Equal(t, expectedVal, actualVal)
}

func TestStructWithNanByUtGoFuzzer(t *testing.T) {
	s := Structure{int: -9223372036854775808, int8: int8(127), int16: int16(32767), int32: int32(0), int64: int64(9223372036854775807), uint: uint(18446744073709551615), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.02308184), float64: 0.9412491794821144, complex64: complex(float32(0.02308184), float32(0.02308184)), complex128: complex(0.9412491794821144, 0.9412491794821144), byte: byte(0), rune: rune(0), string: "hello", bool: false}

	actualVal := StructWithNan(s)

	expectedVal := Structure{int: -9223372036854775808, int8: int8(127), int16: int16(32767), int32: int32(0), int64: int64(9223372036854775807), uint: uint(18446744073709551615), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.02308184), float64: math.NaN(), complex64: complex(float32(0.02308184), float32(0.02308184)), complex128: complex(0.9412491794821144, 0.9412491794821144), byte: byte(0), rune: rune(0), string: "hello", bool: false}

	assert.NotEqual(t, expectedVal, actualVal)
}

func TestArrayOfIntByUtGoFuzzer(t *testing.T) {
	array := [10]int{-1, 0, 0, 0, 0, 0, 0, 0, 0, 0}

	actualVal := ArrayOfInt(array)

	expectedVal := [10]int{-1, 0, 0, 0, 0, 0, 0, 0, 0, 0}

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfUintPtrByUtGoFuzzer(t *testing.T) {
	array := [10]uintptr{0, 0, 0, 0, 0, 0, 0, 0, 0, 0}

	actualVal := ArrayOfUintPtr(array)

	expectedVal := [10]uintptr{0, 0, 0, 0, 0, 0, 0, 0, 0, 0}

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfStringByUtGoFuzzer(t *testing.T) {
	array := [10]string{"hello", "", "", "", "", "", "", "", "", ""}

	actualVal := ArrayOfString(array)

	expectedVal := [10]string{"hello", "", "", "", "", "", "", "", "", ""}

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfStructsByUtGoFuzzer(t *testing.T) {
	array := [10]Structure{{int: -9223372036854775808, int8: int8(-1), int16: int16(-1), int32: int32(1), int64: int64(-9223372036854775808), uint: uint(18446744073709551615), uint8: uint8(1), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(1), uintptr: uintptr(18446744073709551615), float32: float32(0.27495396), float64: 0.31293596519376554, complex64: complex(float32(0.27495396), float32(0.27495396)), complex128: complex(0.31293596519376554, 0.31293596519376554), byte: byte(0), rune: rune(-2147483648), string: "", bool: true}, {}, {}, {}, {}, {}, {}, {}, {}, {}}

	actualVal := ArrayOfStructs(array)

	expectedVal := [10]Structure{{int: -9223372036854775808, int8: int8(-1), int16: int16(-1), int32: int32(1), int64: int64(-9223372036854775808), uint: uint(18446744073709551615), uint8: uint8(1), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(1), uintptr: uintptr(18446744073709551615), float32: float32(0.27495396), float64: 0.31293596519376554, complex64: complex(float32(0.27495396), float32(0.27495396)), complex128: complex(0.31293596519376554, 0.31293596519376554), byte: byte(0), rune: rune(-2147483648), string: "", bool: true}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}}

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfStructsWithNanByUtGoFuzzer(t *testing.T) {
	array := [10]Structure{{int: -9223372036854775808, int8: int8(-1), int16: int16(-1), int32: int32(1), int64: int64(-9223372036854775808), uint: uint(18446744073709551615), uint8: uint8(1), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(1), uintptr: uintptr(18446744073709551615), float32: float32(0.3679757), float64: 0.14660165764651822, complex64: complex(float32(0.3679757), float32(0.3679757)), complex128: complex(0.14660165764651822, 0.14660165764651822), byte: byte(0), rune: rune(-2147483648), string: "", bool: true}, {}, {}, {}, {}, {}, {}, {}, {}, {}}

	actualVal := ArrayOfStructsWithNan(array)

	expectedVal := [10]Structure{{int: -9223372036854775808, int8: int8(-1), int16: int16(-1), int32: int32(1), int64: int64(-9223372036854775808), uint: uint(18446744073709551615), uint8: uint8(1), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(1), uintptr: uintptr(18446744073709551615), float32: float32(0.3679757), float64: math.NaN(), complex64: complex(float32(0.3679757), float32(0.3679757)), complex128: complex(0.14660165764651822, 0.14660165764651822), byte: byte(0), rune: rune(-2147483648), string: "", bool: true}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}}

	assert.NotEqual(t, expectedVal, actualVal)
}

func TestArrayOfArrayOfUintByUtGoFuzzer(t *testing.T) {
	array := [5][5]uint{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}

	actualVal := ArrayOfArrayOfUint(array)

	expectedVal := [5][5]uint{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfArrayOfStructsByUtGoFuzzer(t *testing.T) {
	array := [5][5]Structure{{{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}}

	actualVal := ArrayOfArrayOfStructs(array)

	expectedVal := [5][5]Structure{{{int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}}, {{int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}}, {{int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}}, {{int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}}, {{int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}}}

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfSliceOfUintByUtGoFuzzer(t *testing.T) {
	array := [5][]uint{nil, nil, nil, nil, nil}

	actualVal := ArrayOfSliceOfUint(array)

	expectedVal := [5][]uint{nil, nil, nil, nil, nil}

	assert.Equal(t, expectedVal, actualVal)
}

func TestReturnErrorOrNilWithNonNilErrorByUtGoFuzzer1(t *testing.T) {
	n := 1024

	actualErr := returnErrorOrNil(n)

	expectedErrorMessage := "error"

	assert.ErrorContains(t, actualErr, expectedErrorMessage)
}

func TestReturnErrorOrNilWithNonNilErrorByUtGoFuzzer2(t *testing.T) {
	n := -9223372036854774784

	actualErr := returnErrorOrNil(n)

	assert.Nil(t, actualErr)
}

func TestExternalStructByUtGoFuzzer(t *testing.T) {
	match := difflib.Match{A: 1, B: 1, Size: 1}
	structure := Structure{int: -1, int8: int8(1), int16: int16(32767), int32: int32(0), int64: int64(9223372036854775807), uint: uint(18446744073709551615), uint8: uint8(1), uint16: uint16(65535), uint32: uint32(0), uint64: uint64(1), uintptr: uintptr(0), float32: float32(0.023238122), float64: 0.0013866804054343262, complex64: complex(float32(0.023238122), float32(0.023238122)), complex128: complex(0.0013866804054343262, 0.0013866804054343262), byte: byte(1), rune: rune(-2147483648), string: "hello", bool: false}

	actualVal := ExternalStruct(match, structure)

	expectedVal := Structure{int: -1, int8: int8(1), int16: int16(32767), int32: int32(0), int64: int64(9223372036854775807), uint: uint(18446744073709551615), uint8: uint8(1), uint16: uint16(65535), uint32: uint32(0), uint64: uint64(1), uintptr: uintptr(0), float32: float32(0.023238122), float64: 0.0013866804054343262, complex64: complex(float32(0.023238122), float32(0.023238122)), complex128: complex(0.0013866804054343262, 0.0013866804054343262), byte: byte(1), rune: rune(-2147483648), string: "hello", bool: false}

	assert.Equal(t, expectedVal, actualVal)
}

func TestExternalStructWithAliasByUtGoFuzzer(t *testing.T) {
	match := difflib.Match{A: 1, B: 1, Size: 1}

	actualVal := ExternalStructWithAlias(match)

	expectedVal := difflib.Match{A: 1, B: 1, Size: 1}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfIntByUtGoFuzzer1(t *testing.T) {
	slice := ([]int)(nil)

	actualVal := SliceOfInt(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfIntByUtGoFuzzer2(t *testing.T) {
	slice := []int{9223372036854775807, -1, 0}

	actualVal := SliceOfInt(slice)

	expectedVal := []int{9223372036854775807, -1, 0}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfUintPtrByUtGoFuzzer1(t *testing.T) {
	slice := ([]uintptr)(nil)

	actualVal := SliceOfUintPtr(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfUintPtrByUtGoFuzzer2(t *testing.T) {
	slice := []uintptr{18446744073709551615, 18446744073709551615, 1}

	actualVal := SliceOfUintPtr(slice)

	expectedVal := []uintptr{18446744073709551615, 18446744073709551615, 1}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfStringByUtGoFuzzer1(t *testing.T) {
	slice := ([]string)(nil)

	actualVal := SliceOfString(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfStringByUtGoFuzzer2(t *testing.T) {
	slice := []string{"hello", "", "hello"}

	actualVal := SliceOfString(slice)

	expectedVal := []string{"hello", "", "hello"}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfStructsByUtGoFuzzer1(t *testing.T) {
	slice := ([]Structure)(nil)

	actualVal := SliceOfStructs(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfStructsByUtGoFuzzer2(t *testing.T) {
	slice := []Structure{{int: -9223372036854775808, int8: int8(1), int16: int16(-32768), int32: int32(0), int64: int64(1), uint: uint(18446744073709551615), uint8: uint8(0), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.009224832), float64: 0.9644868606768501, complex64: complex(float32(0.009224832), float32(0.009224832)), complex128: complex(0.9644868606768501, 0.9644868606768501), byte: byte(255), rune: rune(1), string: "", bool: true}, {int: -9223372036854775808, int8: int8(1), int16: int16(-32768), int32: int32(0), int64: int64(1), uint: uint(18446744073709551615), uint8: uint8(0), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.009224832), float64: 0.9644868606768501, complex64: complex(float32(0.009224832), float32(0.009224832)), complex128: complex(0.9644868606768501, 0.9644868606768501), byte: byte(255), rune: rune(1), string: "", bool: true}}

	actualVal := SliceOfStructs(slice)

	expectedVal := []Structure{{int: -9223372036854775808, int8: int8(1), int16: int16(-32768), int32: int32(0), int64: int64(1), uint: uint(18446744073709551615), uint8: uint8(0), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.009224832), float64: 0.9644868606768501, complex64: complex(float32(0.009224832), float32(0.009224832)), complex128: complex(0.9644868606768501, 0.9644868606768501), byte: byte(255), rune: rune(1), string: "", bool: true}, {int: -9223372036854775808, int8: int8(1), int16: int16(-32768), int32: int32(0), int64: int64(1), uint: uint(18446744073709551615), uint8: uint8(0), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.009224832), float64: 0.9644868606768501, complex64: complex(float32(0.009224832), float32(0.009224832)), complex128: complex(0.9644868606768501, 0.9644868606768501), byte: byte(255), rune: rune(1), string: "", bool: true}}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfStructsWithNanByUtGoFuzzer1(t *testing.T) {
	slice := ([]Structure)(nil)

	actualVal := SliceOfStructsWithNan(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfStructsWithNanByUtGoFuzzer2(t *testing.T) {
	slice := []Structure{{int: -1, int8: int8(1), int16: int16(-32768), int32: int32(0), int64: int64(1), uint: uint(18446744073709551615), uint8: uint8(0), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.10449064), float64: 0.252883130686676, complex64: complex(float32(0.10449064), float32(0.10449064)), complex128: complex(0.252883130686676, 0.252883130686676), byte: byte(255), rune: rune(1), string: "", bool: true}, {int: -1, int8: int8(1), int16: int16(-32768), int32: int32(0), int64: int64(1), uint: uint(18446744073709551615), uint8: uint8(0), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.10449064), float64: 0.252883130686676, complex64: complex(float32(0.10449064), float32(0.10449064)), complex128: complex(0.252883130686676, 0.252883130686676), byte: byte(255), rune: rune(1), string: "", bool: true}}

	actualVal := SliceOfStructsWithNan(slice)

	expectedVal := []Structure{{int: -1, int8: int8(1), int16: int16(-32768), int32: int32(0), int64: int64(1), uint: uint(18446744073709551615), uint8: uint8(0), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.10449064), float64: math.NaN(), complex64: complex(float32(0.10449064), float32(0.10449064)), complex128: complex(0.252883130686676, 0.252883130686676), byte: byte(255), rune: rune(1), string: "", bool: true}, {int: -1, int8: int8(1), int16: int16(-32768), int32: int32(0), int64: int64(1), uint: uint(18446744073709551615), uint8: uint8(0), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.10449064), float64: 0.252883130686676, complex64: complex(float32(0.10449064), float32(0.10449064)), complex128: complex(0.252883130686676, 0.252883130686676), byte: byte(255), rune: rune(1), string: "", bool: true}}

	assert.NotEqual(t, expectedVal, actualVal)
}

func TestSliceOfSliceOfByteByUtGoFuzzer1(t *testing.T) {
	slice := [][]byte{nil}

	actualVal := SliceOfSliceOfByte(slice)

	expectedVal := [][]byte{nil}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfSliceOfByteByUtGoFuzzer2(t *testing.T) {
	slice := ([][]byte)(nil)

	actualVal := SliceOfSliceOfByte(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfSliceOfStructsByUtGoFuzzer1(t *testing.T) {
	slice := [][]Structure{nil}

	actualVal := SliceOfSliceOfStructs(slice)

	expectedVal := [][]Structure{nil}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfSliceOfStructsByUtGoFuzzer2(t *testing.T) {
	slice := ([][]Structure)(nil)

	actualVal := SliceOfSliceOfStructs(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfArrayOfIntByUtGoFuzzer1(t *testing.T) {
	slice := [][5]int{{0, 0, 0, 0, 0}}

	actualVal := SliceOfArrayOfInt(slice)

	expectedVal := [][5]int{{0, 0, 0, 0, 0}}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfArrayOfIntByUtGoFuzzer2(t *testing.T) {
	slice := ([][5]int)(nil)

	actualVal := SliceOfArrayOfInt(slice)

	assert.Nil(t, actualVal)
}

func TestExportedStructWithEmbeddedUnexportedStructByUtGoFuzzer(t *testing.T) {
	exportedStruct := nested.ExportedStruct{}

	actualVal := ExportedStructWithEmbeddedUnexportedStruct(exportedStruct)

	expectedVal := nested.ExportedStruct{}

	assert.Equal(t, expectedVal, actualVal)
}

func TestNamedTypeByUtGoFuzzer(t *testing.T) {
	n := Type(4)

	actualVal := NamedType(n)

	expectedVal := Type(4)

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfNamedTypeByUtGoFuzzer(t *testing.T) {
	array := [5]Type{0, 0, 0, 0, 0}

	actualVal := ArrayOfNamedType(array)

	expectedVal := [5]Type{0, 0, 0, 0, 0}

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfArrayOfNamedTypeByUtGoFuzzer(t *testing.T) {
	array := [5][5]Type{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}

	actualVal := ArrayOfArrayOfNamedType(array)

	expectedVal := T{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfNamedTypeByUtGoFuzzer1(t *testing.T) {
	slice := []Type{0}

	actualVal := SliceOfNamedType(slice)

	expectedVal := []Type{0}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfNamedTypeByUtGoFuzzer2(t *testing.T) {
	slice := ([]Type)(nil)

	actualVal := SliceOfNamedType(slice)

	assert.Nil(t, actualVal)
}

func TestNamedArrayByUtGoFuzzer(t *testing.T) {
	array := NA{1, 0, 18446744073709551615, 18446744073709551615, 18446744073709551615}

	actualVal := NamedArray(array)

	expectedVal := NA{1, 0, 18446744073709551615, 18446744073709551615, 18446744073709551615}

	assert.Equal(t, expectedVal, actualVal)
}

func TestNamedSliceByUtGoFuzzer1(t *testing.T) {
	slice := NS(nil)

	actualVal := NamedSlice(slice)

	assert.Nil(t, actualVal)
}

func TestNamedSliceByUtGoFuzzer2(t *testing.T) {
	slice := NS{1073741824, -9223372036854775808, 0, -9223372036854775808, -9223372036854775808}

	actualVal := NamedSlice(slice)

	expectedVal := NS{1073741824, -9223372036854775808, 0, -9223372036854775808, -9223372036854775808}

	assert.Equal(t, expectedVal, actualVal)
}

func TestStructWithFieldsOfNamedTypesByUtGoFuzzer(t *testing.T) {
	s := S{t: Type(0), T: T{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}, n: NA{1, 1, 1, 18446744073709551615, 0}, NS: NS(nil)}

	actualVal := StructWithFieldsOfNamedTypes(s)

	expectedVal := S{t: Type(0), T: T{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}, n: NA{1, 1, 1, 18446744073709551615, 0}, NS: NS(nil)}

	assert.Equal(t, expectedVal, actualVal)
}

func TestMapByUtGoFuzzer(t *testing.T) {
	table := (map[string]int)(nil)

	actualVal := Map(table)

	assert.Nil(t, actualVal)
}

func TestMapOfStructuresByUtGoFuzzer(t *testing.T) {
	table := map[Structure]Structure{Structure{int: -9223372036854775808, int8: int8(-1), int16: int16(-1), int32: int32(1), int64: int64(-9223372036854775808), uint: uint(18446744073709551615), uint8: uint8(1), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(1), uintptr: uintptr(18446744073709551615), float32: float32(0.77631223), float64: 0.9766978471088823, complex64: complex(float32(0.77631223), float32(0.77631223)), complex128: complex(0.9766978471088823, 0.9766978471088823), byte: byte(0), rune: rune(-2147483648), string: "", bool: true}: {int: 9223372036854775807, int8: int8(-1), int16: int16(-1), int32: int32(-1), int64: int64(-9223372036854775808), uint: uint(1), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(1), uintptr: uintptr(1), float32: float32(0.77631223), float64: 0.9766978471088823, complex64: complex(float32(0.77631223), float32(0.77631223)), complex128: complex(0.9766978471088823, 0.9766978471088823), byte: byte(0), rune: rune(1), string: "hello", bool: false}}

	actualVal := MapOfStructures(table)

	expectedVal := map[Structure]Structure{Structure{int: -9223372036854775808, int8: int8(-1), int16: int16(-1), int32: int32(1), int64: int64(-9223372036854775808), uint: uint(18446744073709551615), uint8: uint8(1), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(1), uintptr: uintptr(18446744073709551615), float32: float32(0.77631223), float64: 0.9766978471088823, complex64: complex(float32(0.77631223), float32(0.77631223)), complex128: complex(0.9766978471088823, 0.9766978471088823), byte: byte(0), rune: rune(-2147483648), string: "", bool: true}: {int: 9223372036854775807, int8: int8(-1), int16: int16(-1), int32: int32(-1), int64: int64(-9223372036854775808), uint: uint(1), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(1), uintptr: uintptr(1), float32: float32(0.77631223), float64: 0.9766978471088823, complex64: complex(float32(0.77631223), float32(0.77631223)), complex128: complex(0.9766978471088823, 0.9766978471088823), byte: byte(0), rune: rune(1), string: "hello", bool: false}}

	assert.Equal(t, expectedVal, actualVal)
}

func TestMapOfSliceOfIntByUtGoFuzzer(t *testing.T) {
	table := map[string][]int{"hello": {}}

	actualVal := MapOfSliceOfInt(table)

	expectedVal := map[string][]int{"hello": {}}

	assert.Equal(t, expectedVal, actualVal)
}

func TestMapOfNamedTypeByUtGoFuzzer(t *testing.T) {
	table := map[int]Type{-1: 255}

	actualVal := MapOfNamedType(table)

	expectedVal := map[int]Type{-1: 255}

	assert.Equal(t, expectedVal, actualVal)
}

func TestMapOfNamedSliceByUtGoFuzzer(t *testing.T) {
	table := (map[uint]NS)(nil)

	actualVal := MapOfNamedSlice(table)

	assert.Nil(t, actualVal)
}

func TestNamedMapByUtGoFuzzer(t *testing.T) {
	n := NM(nil)

	actualVal := NamedMap(n)

	assert.Nil(t, actualVal)
}

func TestChannelByUtGoFuzzer1(t *testing.T) {
	c := (chan Structure)(nil)

	assert.NotPanics(t, func() {
		Channel(c)
	})
}

func TestChannelByUtGoFuzzer2(t *testing.T) {
	c := make(chan Structure, 2)
	c <- Structure{int: -9223372036854775808, int8: int8(1), int16: int16(-32768), int32: int32(0), int64: int64(1), uint: uint(18446744073709551615), uint8: uint8(0), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.6882345), float64: 0.4872328470301428, complex64: complex(float32(0.6882345), float32(0.6882345)), complex128: complex(0.4872328470301428, 0.4872328470301428), byte: byte(255), rune: rune(1), string: "", bool: true}
	c <- Structure{int: -9223372036854775808, int8: int8(1), int16: int16(-32768), int32: int32(0), int64: int64(1), uint: uint(18446744073709551615), uint8: uint8(0), uint16: uint16(65535), uint32: uint32(4294967295), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.6882345), float64: 0.4872328470301428, complex64: complex(float32(0.6882345), float32(0.6882345)), complex128: complex(0.4872328470301428, 0.4872328470301428), byte: byte(255), rune: rune(1), string: "", bool: true}
	close(c)

	assert.NotPanics(t, func() {
		Channel(c)
	})
}

func TestSendOnlyChannelByUtGoFuzzer1(t *testing.T) {
	c := (chan<- int)(nil)

	assert.NotPanics(t, func() {
		SendOnlyChannel(c)
	})
}

func TestSendOnlyChannelByUtGoFuzzer2(t *testing.T) {
	c := make(chan int, 3)
	c <- 9223372036854775807
	c <- -1
	c <- 0
	close(c)

	assert.NotPanics(t, func() {
		SendOnlyChannel(c)
	})
}

func TestRecvOnlyChannelByUtGoFuzzer1(t *testing.T) {
	c := (<-chan NM)(nil)

	assert.NotPanics(t, func() {
		RecvOnlyChannel(c)
	})
}

func TestRecvOnlyChannelByUtGoFuzzer2(t *testing.T) {
	c := make(chan NM, 3)
	c <- NM(nil)
	c <- NM(nil)
	c <- NM(nil)
	close(c)

	assert.NotPanics(t, func() {
		RecvOnlyChannel(c)
	})
}

func TestPointerToIntByUtGoFuzzer1(t *testing.T) {
	n := new(int)
	*n = 9223372036850581503

	actualVal := PointerToInt(n)

	expectedVal := new(int)
	*expectedVal = 9223372036850581503

	assert.Equal(t, expectedVal, actualVal)
}

func TestPointerToIntByUtGoFuzzer2(t *testing.T) {
	n := (*int)(nil)

	actualVal := PointerToInt(n)

	assert.Nil(t, actualVal)
}

func TestPointerToSliceByUtGoFuzzer1(t *testing.T) {
	n := (*[]int)(nil)

	actualVal := PointerToSlice(n)

	assert.Nil(t, actualVal)
}

func TestPointerToSliceByUtGoFuzzer2(t *testing.T) {
	n := new([]int)

	actualVal := PointerToSlice(n)

	expectedVal := new([]int)

	assert.Equal(t, expectedVal, actualVal)
}

func TestPointerToArrayByUtGoFuzzer1(t *testing.T) {
	n := (*[3]int)(nil)

	actualVal := PointerToArray(n)

	assert.Nil(t, actualVal)
}

func TestPointerToArrayByUtGoFuzzer2(t *testing.T) {
	n := &[3]int{-9223372036854775800, 0, 0}

	actualVal := PointerToArray(n)

	expectedVal := &[3]int{-9223372036854775800, 0, 0}

	assert.Equal(t, expectedVal, actualVal)
}

func TestPointerToMapByUtGoFuzzer1(t *testing.T) {
	n := (*map[string]int)(nil)

	actualVal := PointerToMap(n)

	assert.Nil(t, actualVal)
}

func TestPointerToMapByUtGoFuzzer2(t *testing.T) {
	n := &map[string]int{"hello": -9223372036854775552, "heulo": -9223372036854775808}

	actualVal := PointerToMap(n)

	expectedVal := &map[string]int{"hello": -9223372036854775552, "heulo": -9223372036854775808}

	assert.Equal(t, expectedVal, actualVal)
}

func TestPointerToStructureByUtGoFuzzer1(t *testing.T) {
	n := (*Structure)(nil)

	actualVal := PointerToStructure(n)

	assert.Nil(t, actualVal)
}

func TestPointerToStructureByUtGoFuzzer2(t *testing.T) {
	n := &Structure{int: 0, int8: int8(127), int16: int16(0), int32: int32(1), int64: int64(0), uint: uint(1), uint8: uint8(0), uint16: uint16(65535), uint32: uint32(1), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.7462414), float64: 0.9809451659493456, complex64: complex(float32(0.7462414), float32(0.7462414)), complex128: complex(0.9809451659493456, 0.9809451659493456), byte: byte(1), rune: rune(0), string: "", bool: true}

	actualVal := PointerToStructure(n)

	expectedVal := &Structure{int: 0, int8: int8(127), int16: int16(0), int32: int32(1), int64: int64(0), uint: uint(1), uint8: uint8(0), uint16: uint16(65535), uint32: uint32(1), uint64: uint64(18446744073709551615), uintptr: uintptr(1), float32: float32(0.7462414), float64: 0.9809451659493456, complex64: complex(float32(0.7462414), float32(0.7462414)), complex128: complex(0.9809451659493456, 0.9809451659493456), byte: byte(1), rune: rune(0), string: "", bool: true}

	assert.Equal(t, expectedVal, actualVal)
}

func TestPointerToNamedTypeByUtGoFuzzer1(t *testing.T) {
	n := (*Type)(nil)

	actualVal := PointerToNamedType(n)

	assert.Nil(t, actualVal)
}

func TestPointerToNamedTypeByUtGoFuzzer2(t *testing.T) {
	n := new(Type)
	*n = 64

	actualVal := PointerToNamedType(n)

	expectedVal := new(Type)
	*expectedVal = 64

	assert.Equal(t, expectedVal, actualVal)
}

func TestPointerToRecursiveStructByUtGoFuzzer1(t *testing.T) {
	n := &Node{prev: (*Node)(nil), next: (*Node)(nil), val: 1}

	actualVal := PointerToRecursiveStruct(n)

	expectedVal := &Node{prev: (*Node)(nil), next: (*Node)(nil), val: 1}

	assert.Equal(t, expectedVal, actualVal)
}

func TestPointerToRecursiveStructByUtGoFuzzer2(t *testing.T) {
	n := (*Node)(nil)

	actualVal := PointerToRecursiveStruct(n)

	assert.Nil(t, actualVal)
}

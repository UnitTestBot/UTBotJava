package simple

import (
	"fmt"
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
	n := 4

	actualVal := Int(n)

	expectedVal := 4

	assert.Equal(t, expectedVal, actualVal)
}

func TestIntByUtGoFuzzer2(t *testing.T) {
	n := -5

	actualVal := Int(n)

	expectedVal := 5

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt8ByUtGoFuzzer1(t *testing.T) {
	n := int8(3)

	actualVal := Int8(n)

	expectedVal := int8(3)

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt8ByUtGoFuzzer2(t *testing.T) {
	n := int8(-2)

	actualVal := Int8(n)

	expectedVal := int8(2)

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt16ByUtGoFuzzer1(t *testing.T) {
	n := int16(-3)

	actualVal := Int16(n)

	expectedVal := int16(3)

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt16ByUtGoFuzzer2(t *testing.T) {
	n := int16(1)

	actualVal := Int16(n)

	expectedVal := int16(1)

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt32ByUtGoFuzzer1(t *testing.T) {
	n := int32(0)

	actualVal := Int32(n)

	expectedVal := int32(0)

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt32ByUtGoFuzzer2(t *testing.T) {
	n := int32(-3)

	actualVal := Int32(n)

	expectedVal := int32(3)

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt64ByUtGoFuzzer1(t *testing.T) {
	n := int64(0)

	actualVal := Int64(n)

	expectedVal := int64(0)

	assert.Equal(t, expectedVal, actualVal)
}

func TestInt64ByUtGoFuzzer2(t *testing.T) {
	n := int64(-5)

	actualVal := Int64(n)

	expectedVal := int64(5)

	assert.Equal(t, expectedVal, actualVal)
}

func TestUintByUtGoFuzzer(t *testing.T) {
	n := uint(1)

	actualVal := Uint(n)

	expectedVal := uint(1)

	assert.Equal(t, expectedVal, actualVal)
}

func TestUint8ByUtGoFuzzer(t *testing.T) {
	n := uint8(3)

	actualVal := Uint8(n)

	expectedVal := uint8(3)

	assert.Equal(t, expectedVal, actualVal)
}

func TestUint16ByUtGoFuzzer(t *testing.T) {
	n := uint16(4)

	actualVal := Uint16(n)

	expectedVal := uint16(4)

	assert.Equal(t, expectedVal, actualVal)
}

func TestUint32ByUtGoFuzzer(t *testing.T) {
	n := uint32(0)

	actualVal := Uint32(n)

	expectedVal := uint32(0)

	assert.Equal(t, expectedVal, actualVal)
}

func TestUint64ByUtGoFuzzer(t *testing.T) {
	n := uint64(0)

	actualVal := Uint64(n)

	expectedVal := uint64(0)

	assert.Equal(t, expectedVal, actualVal)
}

func TestUintPtrByUtGoFuzzer(t *testing.T) {
	n := uintptr(4)

	actualVal := UintPtr(n)

	expectedVal := uintptr(4)

	assert.Equal(t, expectedVal, actualVal)
}

func TestFloat32ByUtGoFuzzer1(t *testing.T) {
	n := float32(-1.464)

	actualVal := Float32(n)

	expectedVal := float32(-1.464)

	assert.Equal(t, expectedVal, actualVal)
}

func TestFloat32ByUtGoFuzzer2(t *testing.T) {
	n := float32(math.Inf(-1))

	actualVal := Float32(n)

	assert.True(t, math.IsInf(float64(actualVal), -1))
}

func TestFloat32ByUtGoFuzzer3(t *testing.T) {
	n := float32(math.Inf(1))

	actualVal := Float32(n)

	assert.True(t, math.IsInf(float64(actualVal), 1))
}

func TestFloat64ByUtGoFuzzer1(t *testing.T) {
	n := 1.86473618990061

	actualVal := Float64(n)

	expectedVal := 1.86473618990061

	assert.Equal(t, expectedVal, actualVal)
}

func TestFloat64ByUtGoFuzzer2(t *testing.T) {
	n := math.Inf(1)

	actualVal := Float64(n)

	assert.True(t, math.IsInf(actualVal, 1))
}

func TestFloat64ByUtGoFuzzer3(t *testing.T) {
	n := math.Inf(-1)

	actualVal := Float64(n)

	assert.True(t, math.IsInf(actualVal, -1))
}

func TestComplex64ByUtGoFuzzer(t *testing.T) {
	n := complex(float32(0.692293), float32(math.Inf(1)))

	actualVal := Complex64(n)

	expectedVal := complex(float32(0.692293), float32(math.Inf(1)))

	assertMultiple := assert.New(t)
	assertMultiple.Equal(real(expectedVal), real(actualVal))
	assertMultiple.Equal(imag(expectedVal), imag(actualVal))
}

func TestComplex128ByUtGoFuzzer(t *testing.T) {
	n := complex(math.Inf(1), 0.945333238959629)

	actualVal := Complex128(n)

	expectedVal := complex(math.Inf(1), 0.945333238959629)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(real(expectedVal), real(actualVal))
	assertMultiple.Equal(imag(expectedVal), imag(actualVal))
}

func TestByteByUtGoFuzzer(t *testing.T) {
	n := byte(1)

	actualVal := Byte(n)

	expectedVal := byte(1)

	assert.Equal(t, expectedVal, actualVal)
}

func TestRuneByUtGoFuzzer(t *testing.T) {
	n := rune(0)

	actualVal := Rune(n)

	expectedVal := rune(0)

	assert.Equal(t, expectedVal, actualVal)
}

func TestStringByUtGoFuzzer(t *testing.T) {
	n := ""

	actualVal := String(n)

	expectedVal := ""

	assert.Equal(t, expectedVal, actualVal)
}

func TestBoolByUtGoFuzzer(t *testing.T) {
	n := true

	actualVal := Bool(n)

	assert.True(t, actualVal)
}

func TestStructByUtGoFuzzer(t *testing.T) {
	s := Structure{int: 0, int8: int8(0), int16: int16(0), int32: int32(1), int64: int64(1), uint: uint(0), uint8: uint8(1), uint16: uint16(1), uint32: uint32(0), uint64: uint64(1), uintptr: uintptr(1), float32: float32(0.724295), float64: 0.2871518999993117, complex64: complex(float32(0.724295), float32(0.724295)), complex128: complex(0.2871518999993117, 0.2871518999993117), byte: byte(1), rune: rune(0), string: "", bool: false}

	actualVal := Struct(s)

	expectedVal := Structure{int: 0, int8: int8(0), int16: int16(0), int32: int32(1), int64: int64(1), uint: uint(0), uint8: uint8(1), uint16: uint16(1), uint32: uint32(0), uint64: uint64(1), uintptr: uintptr(1), float32: float32(0.724295), float64: 0.2871518999993117, complex64: complex(float32(0.724295), float32(0.724295)), complex128: complex(0.2871518999993117, 0.2871518999993117), byte: byte(1), rune: rune(0), string: "", bool: false}

	assert.Equal(t, expectedVal, actualVal)
}

func TestStructWithNanByUtGoFuzzer(t *testing.T) {
	s := Structure{int: 1, int8: int8(-1), int16: int16(32767), int32: int32(1), int64: int64(1), uint: uint(0), uint8: uint8(1), uint16: uint16(0), uint32: uint32(1), uint64: uint64(1), uintptr: uintptr(0), float32: float32(0.63904), float64: 0.2555253108964435, complex64: complex(float32(0.63904), float32(0.63904)), complex128: complex(0.2555253108964435, 0.2555253108964435), byte: byte(0), rune: rune(-1), string: "", bool: false}

	actualVal := StructWithNan(s)

	expectedVal := Structure{int: 1, int8: int8(-1), int16: int16(32767), int32: int32(1), int64: int64(1), uint: uint(0), uint8: uint8(1), uint16: uint16(0), uint32: uint32(1), uint64: uint64(1), uintptr: uintptr(0), float32: float32(0.63904), float64: math.NaN(), complex64: complex(float32(0.63904), float32(0.63904)), complex128: complex(0.2555253108964435, 0.2555253108964435), byte: byte(0), rune: rune(-1), string: "", bool: false}

	assert.NotEqual(t, expectedVal, actualVal)
}

func TestArrayOfIntByUtGoFuzzer(t *testing.T) {
	array := [10]int{0, 0, 0, 0, 0, 0, 0, 0, 0, 0}

	actualVal := ArrayOfInt(array)

	expectedVal := [10]int{0, 0, 0, 0, 0, 0, 0, 0, 0, 0}

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfUintPtrByUtGoFuzzer(t *testing.T) {
	array := [10]uintptr{0, 0, 0, 0, 0, 0, 0, 0, 0, 0}

	actualVal := ArrayOfUintPtr(array)

	expectedVal := [10]uintptr{0, 0, 0, 0, 0, 0, 0, 0, 0, 0}

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfStringByUtGoFuzzer(t *testing.T) {
	array := [10]string{"", "", "", "", "", "", "", "", "", ""}

	actualVal := ArrayOfString(array)

	expectedVal := [10]string{"", "", "", "", "", "", "", "", "", ""}

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfStructsByUtGoFuzzer(t *testing.T) {
	array := [10]Structure{{}, {}, {}, {}, {}, {}, {}, {}, {}, {}}

	actualVal := ArrayOfStructs(array)

	expectedVal := [10]Structure{{int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}}

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfStructsWithNanByUtGoFuzzer(t *testing.T) {
	array := [10]Structure{{}, {}, {}, {}, {}, {}, {}, {}, {}, {}}

	actualVal := ArrayOfStructsWithNan(array)

	expectedVal := [10]Structure{{int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: math.NaN(), complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}, {int: 0, int8: int8(0), int16: int16(0), int32: int32(0), int64: int64(0), uint: uint(0), uint8: uint8(0), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.0), float64: 0.0, complex64: complex(float32(0.0), float32(0.0)), complex128: complex(0.0, 0.0), byte: byte(0), rune: rune(0), string: "", bool: false}}

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
	n := 1

	actualErr := returnErrorOrNil(n)

	expectedErrorMessage := "error"

	assert.ErrorContains(t, actualErr, expectedErrorMessage)
}

func TestReturnErrorOrNilByUtGoFuzzer2(t *testing.T) {
	n := 0

	actualErr := returnErrorOrNil(n)

	assert.Nil(t, actualErr)
}

func TestExternalStructByUtGoFuzzer(t *testing.T) {
	match := difflib.Match{A: -1, B: -1, Size: 0}
	structure := Structure{int: 1, int8: int8(-1), int16: int16(-1), int32: int32(0), int64: int64(-1), uint: uint(0), uint8: uint8(1), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.42890447), float64: 0.6875516381838638, complex64: complex(float32(0.42890447), float32(0.42890447)), complex128: complex(0.6875516381838638, 0.6875516381838638), byte: byte(0), rune: rune(0), string: "", bool: true}

	actualVal := ExternalStruct(match, structure)

	expectedVal := Structure{int: 1, int8: int8(-1), int16: int16(-1), int32: int32(0), int64: int64(-1), uint: uint(0), uint8: uint8(1), uint16: uint16(0), uint32: uint32(0), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.42890447), float64: 0.6875516381838638, complex64: complex(float32(0.42890447), float32(0.42890447)), complex128: complex(0.6875516381838638, 0.6875516381838638), byte: byte(0), rune: rune(0), string: "", bool: true}

	assert.Equal(t, expectedVal, actualVal)
}

func TestExternalStructWithAliasByUtGoFuzzer(t *testing.T) {
	match := difflib.Match{A: 1, B: 0, Size: 1}

	actualVal := ExternalStructWithAlias(match)

	expectedVal := difflib.Match{A: 1, B: 0, Size: 1}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfIntByUtGoFuzzer1(t *testing.T) {
	slice := ([]int)(nil)

	actualVal := SliceOfInt(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfIntByUtGoFuzzer2(t *testing.T) {
	slice := []int{}

	actualVal := SliceOfInt(slice)

	expectedVal := []int{}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfUintPtrByUtGoFuzzer1(t *testing.T) {
	slice := ([]uintptr)(nil)

	actualVal := SliceOfUintPtr(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfUintPtrByUtGoFuzzer2(t *testing.T) {
	slice := []uintptr{}

	actualVal := SliceOfUintPtr(slice)

	expectedVal := []uintptr{}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfStringByUtGoFuzzer1(t *testing.T) {
	slice := []string{}

	actualVal := SliceOfString(slice)

	expectedVal := []string{}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfStringByUtGoFuzzer2(t *testing.T) {
	slice := ([]string)(nil)

	actualVal := SliceOfString(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfStructsByUtGoFuzzer1(t *testing.T) {
	slice := []Structure{}

	actualVal := SliceOfStructs(slice)

	expectedVal := []Structure{}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfStructsByUtGoFuzzer2(t *testing.T) {
	slice := ([]Structure)(nil)

	actualVal := SliceOfStructs(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfStructsWithNanByUtGoFuzzer1(t *testing.T) {
	slice := ([]Structure)(nil)

	actualVal := SliceOfStructsWithNan(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfStructsWithNanByUtGoFuzzer2(t *testing.T) {
	slice := []Structure{{int: 0, int8: int8(-1), int16: int16(0), int32: int32(1), int64: int64(0), uint: uint(0), uint8: uint8(1), uint16: uint16(1), uint32: uint32(1), uint64: uint64(1), uintptr: uintptr(0), float32: float32(0.1789779), float64: 0.043686735841552804, complex64: complex(float32(0.1789779), float32(0.1789779)), complex128: complex(0.043686735841552804, 0.043686735841552804), byte: byte(0), rune: rune(-1), string: "", bool: false}}

	actualVal := SliceOfStructsWithNan(slice)

	expectedVal := []Structure{{int: 0, int8: int8(-1), int16: int16(0), int32: int32(1), int64: int64(0), uint: uint(0), uint8: uint8(1), uint16: uint16(1), uint32: uint32(1), uint64: uint64(1), uintptr: uintptr(0), float32: float32(0.1789779), float64: math.NaN(), complex64: complex(float32(0.1789779), float32(0.1789779)), complex128: complex(0.043686735841552804, 0.043686735841552804), byte: byte(0), rune: rune(-1), string: "", bool: false}}

	assert.NotEqual(t, expectedVal, actualVal)
}

func TestSliceOfStructsWithNanPanicsByUtGoFuzzer(t *testing.T) {
	slice := []Structure{}

	expectedErrorMessage := "runtime error: index out of range [0] with length 0"

	assert.PanicsWithError(t, expectedErrorMessage, func() {
		_ = SliceOfStructsWithNan(slice)
	})
}

func TestSliceOfSliceOfByteByUtGoFuzzer1(t *testing.T) {
	slice := [][]byte{}

	actualVal := SliceOfSliceOfByte(slice)

	expectedVal := [][]byte{}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfSliceOfByteByUtGoFuzzer2(t *testing.T) {
	slice := ([][]byte)(nil)

	actualVal := SliceOfSliceOfByte(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfSliceOfStructsByUtGoFuzzer1(t *testing.T) {
	slice := [][]Structure{}

	actualVal := SliceOfSliceOfStructs(slice)

	expectedVal := [][]Structure{}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfSliceOfStructsByUtGoFuzzer2(t *testing.T) {
	slice := ([][]Structure)(nil)

	actualVal := SliceOfSliceOfStructs(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfArrayOfIntByUtGoFuzzer1(t *testing.T) {
	slice := [][5]int{}

	actualVal := SliceOfArrayOfInt(slice)

	expectedVal := [][5]int{}

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
	n := Type(1)

	actualVal := NamedType(n)

	expectedVal := Type(1)

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfNamedTypeByUtGoFuzzer(t *testing.T) {
	array := [5]Type{1, 1, 8, 0, 0}

	actualVal := ArrayOfNamedType(array)

	expectedVal := [5]Type{1, 1, 8, 0, 0}

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfArrayOfNamedTypeByUtGoFuzzer(t *testing.T) {
	array := [5][5]Type{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}

	actualVal := ArrayOfArrayOfNamedType(array)

	expectedVal := T{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfNamedTypeByUtGoFuzzer1(t *testing.T) {
	slice := ([]Type)(nil)

	actualVal := SliceOfNamedType(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfNamedTypeByUtGoFuzzer2(t *testing.T) {
	slice := []Type{}

	actualVal := SliceOfNamedType(slice)

	expectedVal := []Type{}

	assert.Equal(t, expectedVal, actualVal)
}

func TestNamedArrayByUtGoFuzzer(t *testing.T) {
	array := NA{1, 0, 0, 0, 0}

	actualVal := NamedArray(array)

	expectedVal := NA{1, 0, 0, 0, 0}

	assert.Equal(t, expectedVal, actualVal)
}

func TestNamedSliceByUtGoFuzzer1(t *testing.T) {
	slice := NS{}

	actualVal := NamedSlice(slice)

	expectedVal := NS{}

	assert.Equal(t, expectedVal, actualVal)
}

func TestNamedSliceByUtGoFuzzer2(t *testing.T) {
	slice := NS(nil)

	actualVal := NamedSlice(slice)

	assert.Nil(t, actualVal)
}

func TestStructWithFieldsOfNamedTypesByUtGoFuzzer(t *testing.T) {
	s := S{t: Type(1), T: T{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}, n: NA{1, 5, 0, 0, 0}, NS: NS(nil)}

	actualVal := StructWithFieldsOfNamedTypes(s)

	expectedVal := S{t: Type(1), T: T{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}, n: NA{1, 5, 0, 0, 0}, NS: NS(nil)}

	assert.Equal(t, expectedVal, actualVal)
}

func TestMapByUtGoFuzzer(t *testing.T) {
	table := (map[string]int)(nil)

	actualVal := Map(table)

	assert.Nil(t, actualVal)
}

func TestMapOfStructuresByUtGoFuzzer(t *testing.T) {
	table := (map[Structure]Structure)(nil)

	actualVal := MapOfStructures(table)

	assert.Nil(t, actualVal)
}

func TestMapOfSliceOfIntByUtGoFuzzer(t *testing.T) {
	table := (map[string][]int)(nil)

	actualVal := MapOfSliceOfInt(table)

	assert.Nil(t, actualVal)
}

func TestMapOfNamedTypeByUtGoFuzzer(t *testing.T) {
	table := (map[int]Type)(nil)

	actualVal := MapOfNamedType(table)

	assert.Nil(t, actualVal)
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
	c := make(chan Structure, 0)

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
	c := make(chan int, 0)

	close(c)

	assert.NotPanics(t, func() {
		SendOnlyChannel(c)
	})
}

func TestRecvOnlyChannelByUtGoFuzzer1(t *testing.T) {
	c := make(chan NM, 0)

	close(c)

	assert.NotPanics(t, func() {
		RecvOnlyChannel(c)
	})
}

func TestRecvOnlyChannelByUtGoFuzzer2(t *testing.T) {
	c := (<-chan NM)(nil)

	assert.NotPanics(t, func() {
		RecvOnlyChannel(c)
	})
}

func TestPointerToIntByUtGoFuzzer1(t *testing.T) {
	n := (*int)(nil)

	actualVal := PointerToInt(n)

	assert.Nil(t, actualVal)
}

func TestPointerToIntByUtGoFuzzer2(t *testing.T) {
	n := new(int)
	*n = 3

	actualVal := PointerToInt(n)

	expectedVal := new(int)
	*expectedVal = 3

	assert.Equal(t, expectedVal, actualVal)
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
	n := &[3]int{0, 0, 0}

	actualVal := PointerToArray(n)

	expectedVal := &[3]int{0, 0, 0}

	assert.Equal(t, expectedVal, actualVal)
}

func TestPointerToMapByUtGoFuzzer1(t *testing.T) {
	n := (*map[string]int)(nil)

	actualVal := PointerToMap(n)

	assert.Nil(t, actualVal)
}

func TestPointerToMapByUtGoFuzzer2(t *testing.T) {
	n := new(map[string]int)

	actualVal := PointerToMap(n)

	expectedVal := new(map[string]int)

	assert.Equal(t, expectedVal, actualVal)
}

func TestPointerToStructureByUtGoFuzzer1(t *testing.T) {
	n := &Structure{int: 0, int8: int8(0), int16: int16(-1), int32: int32(1), int64: int64(-1), uint: uint(1), uint8: uint8(255), uint16: uint16(0), uint32: uint32(1), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.5587146), float64: 0.745147167878201, complex64: complex(float32(0.5587146), float32(0.5587146)), complex128: complex(0.745147167878201, 0.745147167878201), byte: byte(0), rune: rune(0), string: "", bool: true}

	actualVal := PointerToStructure(n)

	expectedVal := &Structure{int: 0, int8: int8(0), int16: int16(-1), int32: int32(1), int64: int64(-1), uint: uint(1), uint8: uint8(255), uint16: uint16(0), uint32: uint32(1), uint64: uint64(0), uintptr: uintptr(0), float32: float32(0.5587146), float64: 0.745147167878201, complex64: complex(float32(0.5587146), float32(0.5587146)), complex128: complex(0.745147167878201, 0.745147167878201), byte: byte(0), rune: rune(0), string: "", bool: true}

	assert.Equal(t, expectedVal, actualVal)
}

func TestPointerToStructureByUtGoFuzzer2(t *testing.T) {
	n := (*Structure)(nil)

	actualVal := PointerToStructure(n)

	assert.Nil(t, actualVal)
}

func TestPointerToNamedTypeByUtGoFuzzer1(t *testing.T) {
	n := (*Type)(nil)

	actualVal := PointerToNamedType(n)

	assert.Nil(t, actualVal)
}

func TestPointerToNamedTypeByUtGoFuzzer2(t *testing.T) {
	n := new(Type)
	*n = 1

	actualVal := PointerToNamedType(n)

	expectedVal := new(Type)
	*expectedVal = 1

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

func TestInterfaceByUtGoFuzzer1(t *testing.T) {
	i := I(nil)

	assert.NotPanics(t, func() {
		Interface(i)
	})
}

func TestInterfaceByUtGoFuzzer2(t *testing.T) {
	i := I(nil)

	assert.NotPanics(t, func() {
		Interface(i)
	})
}

func TestExternalInterfaceByUtGoFuzzer(t *testing.T) {
	stringer := fmt.Stringer(nil)

	actualVal := ExternalInterface(stringer)

	expectedVal := ""

	assert.Equal(t, expectedVal, actualVal)
}

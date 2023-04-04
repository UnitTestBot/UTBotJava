package simple

import (
	"github.com/pmezard/go-difflib/difflib"
	"github.com/stretchr/testify/assert"
	"go-samples/simple/nested"
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
	actualVal := Float32(0.24053639)

	assert.Equal(t, float32(0.24053639), actualVal)
}

func TestFloat64ByUtGoFuzzer(t *testing.T) {
	actualVal := Float64(0.6063452159973596)

	assert.Equal(t, 0.6063452159973596, actualVal)
}

func TestComplex64ByUtGoFuzzer(t *testing.T) {
	actualVal := Complex64(complex(0.30905056, 0.30905056))

	assert.Equal(t, complex(float32(0.30905056), float32(0.30905056)), actualVal)
}

func TestComplex128ByUtGoFuzzer(t *testing.T) {
	actualVal := Complex128(complex(0.5504370051176339, 0.5504370051176339))

	assert.Equal(t, complex(0.5504370051176339, 0.5504370051176339), actualVal)
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
	actualVal := Struct(Structure{})

	assert.Equal(t, Structure{}, actualVal)
}

func TestStructWithNanByUtGoFuzzer(t *testing.T) {
	actualVal := StructWithNan(Structure{})

	assert.NotEqual(t, Structure{}, actualVal)
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
	actualVal := ArrayOfStructs([10]Structure{{}, {}, {}, {}, {}, {}, {}, {}, {}, {}})

	assert.Equal(t, [10]Structure{{}, {}, {}, {}, {}, {}, {}, {}, {}, {}}, actualVal)
}

func TestArrayOfStructsWithNanByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfStructsWithNan([10]Structure{{}, {}, {}, {}, {}, {}, {}, {}, {}, {}})

	assert.NotEqual(t, [10]Structure{{}, {}, {}, {}, {}, {}, {}, {}, {}, {}}, actualVal)
}

func TestArrayOfArrayOfUintByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfArrayOfUint([5][5]uint{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}})

	assert.Equal(t, [5][5]uint{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}, actualVal)
}

func TestArrayOfArrayOfStructsByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfArrayOfStructs([5][5]Structure{{{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}})

	assert.Equal(t, [5][5]Structure{{{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}}, actualVal)
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
	actualVal := ExternalStruct(difflib.Match{A: 1, B: 1, Size: 1}, Structure{})

	assert.Equal(t, Structure{}, actualVal)
}

func TestExternalStructWithAliasByUtGoFuzzer(t *testing.T) {
	actualVal := ExternalStructWithAlias(difflib.Match{A: 1, B: 1, Size: 1})

	assert.Equal(t, difflib.Match{A: 1, B: 1, Size: 1}, actualVal)
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
	actualVal := SliceOfStructs([]Structure{{}})

	assert.Equal(t, []Structure{{}}, actualVal)
}

func TestSliceOfStructsWithNanByUtGoFuzzer(t *testing.T) {
	actualVal := SliceOfStructsWithNan([]Structure{{}})

	assert.NotEqual(t, []Structure{{}}, actualVal)
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

func TestNamedTypeByUtGoFuzzer(t *testing.T) {
	actualVal := NamedType(Type(0))

	assert.Equal(t, Type(0), actualVal)
}

func TestArrayOfNamedTypeByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfNamedType([5]Type{0, 0, 0, 0, 0})

	assert.Equal(t, [5]Type{0, 0, 0, 0, 0}, actualVal)
}

func TestArrayOfArrayOfNamedTypeByUtGoFuzzer(t *testing.T) {
	actualVal := ArrayOfArrayOfNamedType([5][5]Type{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}})

	assert.Equal(t, T{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}, actualVal)
}

func TestSliceOfNamedTypeByUtGoFuzzer(t *testing.T) {
	actualVal := SliceOfNamedType([]Type{0})

	assert.Equal(t, []Type{0}, actualVal)
}

func TestNamedArrayByUtGoFuzzer(t *testing.T) {
	actualVal := NamedArray(NA{1, 0, 18446744073709551615, 18446744073709551615, 18446744073709551615})

	assert.Equal(t, NA{1, 0, 18446744073709551615, 18446744073709551615, 18446744073709551615}, actualVal)
}

func TestNamedSliceByUtGoFuzzer(t *testing.T) {
	actualVal := NamedSlice(NS{-1, 9223372036854775807, 9223372036854775807, 1, 9223372036854775807})

	assert.Equal(t, NS{-1, 9223372036854775807, 9223372036854775807, 1, 9223372036854775807}, actualVal)
}

func TestStructWithFieldsOfNamedTypesByUtGoFuzzer(t *testing.T) {
	actualVal := StructWithFieldsOfNamedTypes(S{T: T{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}, NS: NS{9223372036854775807, 1, 0, -9223372036854775808, 9223372036854775807}})

	assert.Equal(t, S{T: T{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}, NS: NS{9223372036854775807, 1, 0, -9223372036854775808, 9223372036854775807}}, actualVal)
}

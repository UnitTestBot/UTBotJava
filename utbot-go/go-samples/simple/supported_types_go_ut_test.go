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
	actualVal := Int(1024)

	assert.Equal(t, 1024, actualVal)
}

func TestIntByUtGoFuzzer2(t *testing.T) {
	actualVal := Int(-9223372036854774784)

	assert.Equal(t, 9223372036854774784, actualVal)
}

func TestInt8ByUtGoFuzzer1(t *testing.T) {
	actualVal := Int8(2)

	assert.Equal(t, int8(2), actualVal)
}

func TestInt8ByUtGoFuzzer2(t *testing.T) {
	actualVal := Int8(-126)

	assert.Equal(t, int8(126), actualVal)
}

func TestInt16ByUtGoFuzzer1(t *testing.T) {
	actualVal := Int16(4)

	assert.Equal(t, int16(4), actualVal)
}

func TestInt16ByUtGoFuzzer2(t *testing.T) {
	actualVal := Int16(-32764)

	assert.Equal(t, int16(32764), actualVal)
}

func TestInt32ByUtGoFuzzer1(t *testing.T) {
	actualVal := Int32(32)

	assert.Equal(t, int32(32), actualVal)
}

func TestInt32ByUtGoFuzzer2(t *testing.T) {
	actualVal := Int32(-2147483616)

	assert.Equal(t, int32(2147483616), actualVal)
}

func TestInt64ByUtGoFuzzer1(t *testing.T) {
	actualVal := Int64(1024)

	assert.Equal(t, int64(1024), actualVal)
}

func TestInt64ByUtGoFuzzer2(t *testing.T) {
	actualVal := Int64(-9223372036854774784)

	assert.Equal(t, int64(9223372036854774784), actualVal)
}

func TestUintByUtGoFuzzer(t *testing.T) {
	actualVal := Uint(1024)

	assert.Equal(t, uint(1024), actualVal)
}

func TestUint8ByUtGoFuzzer(t *testing.T) {
	actualVal := Uint8(2)

	assert.Equal(t, uint8(2), actualVal)
}

func TestUint16ByUtGoFuzzer(t *testing.T) {
	actualVal := Uint16(4)

	assert.Equal(t, uint16(4), actualVal)
}

func TestUint32ByUtGoFuzzer(t *testing.T) {
	actualVal := Uint32(32)

	assert.Equal(t, uint32(32), actualVal)
}

func TestUint64ByUtGoFuzzer(t *testing.T) {
	actualVal := Uint64(1024)

	assert.Equal(t, uint64(1024), actualVal)
}

func TestUintPtrByUtGoFuzzer(t *testing.T) {
	actualVal := UintPtr(1024)

	assert.Equal(t, uintptr(1024), actualVal)
}

func TestFloat32ByUtGoFuzzer(t *testing.T) {
	actualVal := Float32(-0.24053639)

	assert.Equal(t, float32(-0.24053639), actualVal)
}

func TestFloat64ByUtGoFuzzer(t *testing.T) {
	actualVal := Float64(-0.6063452159973596)

	assert.Equal(t, -0.6063452159973596, actualVal)
}

func TestComplex64ByUtGoFuzzer(t *testing.T) {
	actualVal := Complex64(complex(0.30905056, 0.0012072287))

	assert.Equal(t, complex(float32(0.30905056), float32(0.0012072287)), actualVal)
}

func TestComplex128ByUtGoFuzzer(t *testing.T) {
	actualVal := Complex128(complex(0.5504370051176339, 0.03440231281985212))

	assert.Equal(t, complex(0.5504370051176339, 0.03440231281985212), actualVal)
}

func TestByteByUtGoFuzzer(t *testing.T) {
	actualVal := Byte(2)

	assert.Equal(t, uint8(2), actualVal)
}

func TestRuneByUtGoFuzzer(t *testing.T) {
	actualVal := Rune(2147483615)

	assert.Equal(t, int32(2147483615), actualVal)
}

func TestStringByUtGoFuzzer(t *testing.T) {
	actualVal := String("3hllo")

	assert.Equal(t, "3hllo", actualVal)
}

func TestBoolByUtGoFuzzer(t *testing.T) {
	actualVal := Bool(false)

	assert.Equal(t, false, actualVal)
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
	actualVal := ArrayOfSliceOfUint([5][]uint{nil, nil, nil, nil, nil})

	assert.Equal(t, [5][]uint{nil, nil, nil, nil, nil}, actualVal)
}

func TestReturnErrorOrNilWithNonNilErrorByUtGoFuzzer1(t *testing.T) {
	actualErr := returnErrorOrNil(1024)

	assert.ErrorContains(t, actualErr, "error")
}

func TestReturnErrorOrNilWithNonNilErrorByUtGoFuzzer2(t *testing.T) {
	actualErr := returnErrorOrNil(-9223372036854774784)

	assert.Nil(t, actualErr)
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
	actualVal := SliceOfInt(nil)

	assert.Nil(t, actualVal)
}

func TestSliceOfUintPtrByUtGoFuzzer(t *testing.T) {
	actualVal := SliceOfUintPtr(nil)

	assert.Nil(t, actualVal)
}

func TestSliceOfStringByUtGoFuzzer(t *testing.T) {
	actualVal := SliceOfString(nil)

	assert.Nil(t, actualVal)
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
	actualVal := SliceOfSliceOfByte([][]byte{nil})

	assert.Equal(t, [][]byte{nil}, actualVal)
}

func TestSliceOfSliceOfStructsByUtGoFuzzer(t *testing.T) {
	actualVal := SliceOfSliceOfStructs([][]Structure{nil})

	assert.Equal(t, [][]Structure{nil}, actualVal)
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
	actualVal := NamedType(4)

	assert.Equal(t, Type(4), actualVal)
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
	actualVal := NamedSlice(NS(nil))

	assert.Nil(t, actualVal)
}

func TestStructWithFieldsOfNamedTypesByUtGoFuzzer(t *testing.T) {
	actualVal := StructWithFieldsOfNamedTypes(S{T: T{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}, NS: NS(nil)})

	assert.Equal(t, S{T: T{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}, NS: NS(nil)}, actualVal)
}

func TestMapByUtGoFuzzer(t *testing.T) {
	actualVal := Map(nil)

	assert.Nil(t, actualVal)
}

func TestMapOfStructuresByUtGoFuzzer(t *testing.T) {
	actualVal := MapOfStructures(map[Structure]Structure{Structure{}: {}})

	assert.Equal(t, map[Structure]Structure{Structure{}: {}}, actualVal)
}

func TestMapOfSliceOfIntByUtGoFuzzer(t *testing.T) {
	actualVal := MapOfSliceOfInt(map[string][]int{"hello": {}})

	assert.Equal(t, map[string][]int{"hello": {}}, actualVal)
}

func TestMapOfNamedTypeByUtGoFuzzer(t *testing.T) {
	actualVal := MapOfNamedType(map[int]Type{-1: 255})

	assert.Equal(t, map[int]Type{-1: 255}, actualVal)
}

func TestMapOfNamedSliceByUtGoFuzzer(t *testing.T) {
	actualVal := MapOfNamedSlice(nil)

	assert.Nil(t, actualVal)
}

func TestNamedMapByUtGoFuzzer(t *testing.T) {
	actualVal := NamedMap(NM(nil))

	assert.Nil(t, actualVal)
}

func TestChannelByUtGoFuzzer1(t *testing.T) {
	assert.NotPanics(t, func() { Channel(make(chan Structure, 1)) })
}

func TestChannelByUtGoFuzzer2(t *testing.T) {
	assert.NotPanics(t, func() { Channel(nil) })
}

func TestSendOnlyChannelByUtGoFuzzer1(t *testing.T) {
	assert.NotPanics(t, func() { SendOnlyChannel(nil) })
}

func TestSendOnlyChannelByUtGoFuzzer2(t *testing.T) {
	assert.NotPanics(t, func() { SendOnlyChannel(make(chan<- int, 3)) })
}

func TestRecvOnlyChannelByUtGoFuzzer1(t *testing.T) {
	assert.NotPanics(t, func() { RecvOnlyChannel(nil) })
}

func TestRecvOnlyChannelByUtGoFuzzer2(t *testing.T) {
	assert.NotPanics(t, func() { RecvOnlyChannel(make(<-chan NM, 3)) })
}

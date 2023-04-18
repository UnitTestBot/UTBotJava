package simple

import (
	"github.com/pmezard/go-difflib/difflib"
	"github.com/stretchr/testify/assert"
	"go-samples/simple/nested"
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
	n := float32(-0.24053639)

	actualVal := Float32(n)

	expectedVal := float32(-0.24053639)

	assert.Equal(t, expectedVal, actualVal)
}

func TestFloat64ByUtGoFuzzer(t *testing.T) {
	n := -0.6063452159973596

	actualVal := Float64(n)

	expectedVal := -0.6063452159973596

	assert.Equal(t, expectedVal, actualVal)
}

func TestComplex64ByUtGoFuzzer(t *testing.T) {
	n := complex(float32(0.30905056), float32(0.0012072287))

	actualVal := Complex64(n)

	expectedVal := complex(float32(0.30905056), float32(0.0012072287))

	assert.Equal(t, expectedVal, actualVal)
}

func TestComplex128ByUtGoFuzzer(t *testing.T) {
	n := complex(0.5504370051176339, 0.03440231281985212)

	actualVal := Complex128(n)

	expectedVal := complex(0.5504370051176339, 0.03440231281985212)

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
	s := Structure{}

	actualVal := Struct(s)

	expectedVal := Structure{}

	assert.Equal(t, expectedVal, actualVal)
}

func TestStructWithNanByUtGoFuzzer(t *testing.T) {
	s := Structure{}

	actualVal := StructWithNan(s)

	expectedVal := Structure{}

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
	array := [10]Structure{{}, {}, {}, {}, {}, {}, {}, {}, {}, {}}

	actualVal := ArrayOfStructs(array)

	expectedVal := [10]Structure{{}, {}, {}, {}, {}, {}, {}, {}, {}, {}}

	assert.Equal(t, expectedVal, actualVal)
}

func TestArrayOfStructsWithNanByUtGoFuzzer(t *testing.T) {
	array := [10]Structure{{}, {}, {}, {}, {}, {}, {}, {}, {}, {}}

	actualVal := ArrayOfStructsWithNan(array)

	expectedVal := [10]Structure{{}, {}, {}, {}, {}, {}, {}, {}, {}, {}}

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

	expectedVal := [5][5]Structure{{{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}, {{}, {}, {}, {}, {}}}

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
	structure := Structure{}

	actualVal := ExternalStruct(match, structure)

	expectedVal := Structure{}

	assert.Equal(t, expectedVal, actualVal)
}

func TestExternalStructWithAliasByUtGoFuzzer(t *testing.T) {
	match := difflib.Match{A: 1, B: 1, Size: 1}

	actualVal := ExternalStructWithAlias(match)

	expectedVal := difflib.Match{A: 1, B: 1, Size: 1}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfIntByUtGoFuzzer(t *testing.T) {
	slice := ([]int)(nil)

	actualVal := SliceOfInt(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfUintPtrByUtGoFuzzer(t *testing.T) {
	slice := ([]uintptr)(nil)

	actualVal := SliceOfUintPtr(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfStringByUtGoFuzzer(t *testing.T) {
	slice := ([]string)(nil)

	actualVal := SliceOfString(slice)

	assert.Nil(t, actualVal)
}

func TestSliceOfStructsByUtGoFuzzer(t *testing.T) {
	slice := []Structure{{}}

	actualVal := SliceOfStructs(slice)

	expectedVal := []Structure{{}}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfStructsWithNanByUtGoFuzzer(t *testing.T) {
	slice := []Structure{{}}

	actualVal := SliceOfStructsWithNan(slice)

	expectedVal := []Structure{{}}

	assert.NotEqual(t, expectedVal, actualVal)
}

func TestSliceOfSliceOfByteByUtGoFuzzer(t *testing.T) {
	slice := [][]byte{nil}

	actualVal := SliceOfSliceOfByte(slice)

	expectedVal := [][]byte{nil}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfSliceOfStructsByUtGoFuzzer(t *testing.T) {
	slice := [][]Structure{nil}

	actualVal := SliceOfSliceOfStructs(slice)

	expectedVal := [][]Structure{nil}

	assert.Equal(t, expectedVal, actualVal)
}

func TestSliceOfArrayOfIntByUtGoFuzzer(t *testing.T) {
	slice := [][5]int{{0, 0, 0, 0, 0}}

	actualVal := SliceOfArrayOfInt(slice)

	expectedVal := [][5]int{{0, 0, 0, 0, 0}}

	assert.Equal(t, expectedVal, actualVal)
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

func TestSliceOfNamedTypeByUtGoFuzzer(t *testing.T) {
	slice := []Type{0}

	actualVal := SliceOfNamedType(slice)

	expectedVal := []Type{0}

	assert.Equal(t, expectedVal, actualVal)
}

func TestNamedArrayByUtGoFuzzer(t *testing.T) {
	array := NA{1, 0, 18446744073709551615, 18446744073709551615, 18446744073709551615}

	actualVal := NamedArray(array)

	expectedVal := NA{1, 0, 18446744073709551615, 18446744073709551615, 18446744073709551615}

	assert.Equal(t, expectedVal, actualVal)
}

func TestNamedSliceByUtGoFuzzer(t *testing.T) {
	slice := NS(nil)

	actualVal := NamedSlice(slice)

	assert.Nil(t, actualVal)
}

func TestStructWithFieldsOfNamedTypesByUtGoFuzzer(t *testing.T) {
	s := S{T: T{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}, NS: NS(nil)}

	actualVal := StructWithFieldsOfNamedTypes(s)

	expectedVal := S{T: T{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}}, NS: NS(nil)}

	assert.Equal(t, expectedVal, actualVal)
}

func TestMapByUtGoFuzzer(t *testing.T) {
	table := (map[string]int)(nil)

	actualVal := Map(table)

	assert.Nil(t, actualVal)
}

func TestMapOfStructuresByUtGoFuzzer(t *testing.T) {
	table := map[Structure]Structure{Structure{}: {}}

	actualVal := MapOfStructures(table)

	expectedVal := map[Structure]Structure{Structure{}: {}}

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
	c := make(chan Structure, 1)
	c <- Structure{}
	close(c)

	assert.NotPanics(t, func() {
		Channel(c)
	})
}

func TestChannelByUtGoFuzzer2(t *testing.T) {
	c := (chan Structure)(nil)

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

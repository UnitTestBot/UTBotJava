package simple

import (
	"errors"
	"github.com/pmezard/go-difflib/difflib"
	dif "github.com/pmezard/go-difflib/difflib"
	"math"
)

func WithoutParametersAndReturnValues() {
	print("Hello World")
}

func Int(n int) int {
	if n < 0 {
		return -n
	}
	return n
}

func Int8(n int8) int8 {
	if n < 0 {
		return -n
	}
	return n
}

func Int16(n int16) int16 {
	if n < 0 {
		return -n
	}
	return n
}

func Int32(n int32) int32 {
	if n < 0 {
		return -n
	}
	return n
}

func Int64(n int64) int64 {
	if n < 0 {
		return -n
	}
	return n
}

func Uint(n uint) uint {
	return n
}

func Uint8(n uint8) uint8 {
	return n
}

func Uint16(n uint16) uint16 {
	return n
}

func Uint32(n uint32) uint32 {
	return n
}

func Uint64(n uint64) uint64 {
	return n
}

func UintPtr(n uintptr) uintptr {
	return n
}

func Float32(n float32) float32 {
	return n
}

func Float64(n float64) float64 {
	return n
}

func Complex64(n complex64) complex64 {
	return n
}

func Complex128(n complex128) complex128 {
	return n
}

func Byte(n byte) byte {
	return n
}

func Rune(n rune) rune {
	return n
}

func String(n string) string {
	return n
}

func Bool(n bool) bool {
	return n
}

type Structure struct {
	int
	int8
	int16
	int32
	int64
	uint
	uint8
	uint16
	uint32
	uint64
	uintptr
	float32
	float64
	complex64
	complex128
	byte
	rune
	string
	bool
}

func Struct(s Structure) Structure {
	return s
}

func StructWithNan(s Structure) Structure {
	s.float64 = math.NaN()
	return s
}

func ArrayOfInt(array [10]int) [10]int {
	return array
}

func ArrayOfUintPtr(array [10]uintptr) [10]uintptr {
	return array
}

func ArrayOfString(array [10]string) [10]string {
	return array
}

func ArrayOfStructs(array [10]Structure) [10]Structure {
	return array
}

func ArrayOfStructsWithNan(array [10]Structure) [10]Structure {
	array[0].float64 = math.NaN()
	return array
}

func ArrayOfArrayOfUint(array [5][5]uint) [5][5]uint {
	return array
}

func ArrayOfArrayOfStructs(array [5][5]Structure) [5][5]Structure {
	return array
}

func returnErrorOrNil(n int) error {
	if n > 0 {
		return errors.New("error")
	} else {
		return nil
	}
}

func ExternalStruct(match difflib.Match, structure Structure) Structure {
	return structure
}

func ExternalStructWithAlias(match dif.Match) difflib.Match {
	return match
}

package simple

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestDivOrPanicByUtGoFuzzer(t *testing.T) {
	actualVal := DivOrPanic(0, -9223372036854775807)

	assert.Equal(t, 0, actualVal)
}

func TestDivOrPanicPanicsByUtGoFuzzer(t *testing.T) {
	assert.PanicsWithValue(t, "div by 0", func() { DivOrPanic(-9223372036854775807, 0) })
}

func TestExtendedByUtGoFuzzer1(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(9223372036854775807, -9223372036854775808)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-1), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestExtendedByUtGoFuzzer2(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(0, -1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-1), actualVal0)
	assertMultiple.Equal(int64(0), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestArraySumByUtGoFuzzer(t *testing.T) {
	actualVal := ArraySum([5]int{1, 0, 0, 0, 0})

	assert.Equal(t, 1, actualVal)
}

func TestGenerateArrayOfIntegersByUtGoFuzzer(t *testing.T) {
	actualVal := GenerateArrayOfIntegers(9223372036854774783)

	assert.Equal(t, [10]int{9223372036854774783, 9223372036854774783, 9223372036854774783, 9223372036854774783, 9223372036854774783, 9223372036854774783, 9223372036854774783, 9223372036854774783, 9223372036854774783, 9223372036854774783}, actualVal)
}

func TestDistanceBetweenTwoPointsByUtGoFuzzer(t *testing.T) {
	actualVal := DistanceBetweenTwoPoints(Point{}, Point{})

	assert.Equal(t, 0.0, actualVal)
}

func TestGetCoordinatesOfMiddleBetweenTwoPointsByUtGoFuzzer(t *testing.T) {
	actualVal0, actualVal1 := GetCoordinatesOfMiddleBetweenTwoPoints(Point{}, Point{})

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0.0, actualVal0)
	assertMultiple.Equal(0.0, actualVal1)
}

func TestGetCoordinateSumOfPointsByUtGoFuzzer(t *testing.T) {
	actualVal0, actualVal1 := GetCoordinateSumOfPoints([]Point{{}})

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0.0, actualVal0)
	assertMultiple.Equal(0.0, actualVal1)
}

func TestGetAreaOfCircleByUtGoFuzzer(t *testing.T) {
	actualVal := GetAreaOfCircle(Circle{Center: Point{}, Radius: 2.0})

	assert.Equal(t, 12.566370614359172, actualVal)
}

func TestIsIdentityByUtGoFuzzer(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}})

	assert.Equal(t, false, actualVal)
}

func TestBinaryWithNonNilErrorByUtGoFuzzer1(t *testing.T) {
	actualVal, actualErr := Binary(nil, 9223372036854775807, 9223301668110598143, -1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(-1, actualVal)
	assertMultiple.ErrorContains(actualErr, "target not found in array")
}

func TestBinaryWithNonNilErrorByUtGoFuzzer2(t *testing.T) {
	actualVal, actualErr := Binary([]int{1, 9223372036854775807}, 9187343239835810815, 1, 2)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(-1, actualVal)
	assertMultiple.ErrorContains(actualErr, "target not found in array")
}

func TestBinaryWithNonNilErrorByUtGoFuzzer3(t *testing.T) {
	actualVal, actualErr := Binary([]int{-1, 1, 1048578, 1, 1}, 1, 1, 1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(1, actualVal)
	assertMultiple.Nil(actualErr)
}

func TestBinaryPanicsByUtGoFuzzer1(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: index out of range [4611686018427387903] with length 0", func() { Binary(nil, 2, 0, 9223372036854775807) })
}

func TestBinaryPanicsByUtGoFuzzer2(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: index out of range [2] with length 2", func() { Binary([]int{9223372036854775807, 1}, 9223372036854774783, 1, 2) })
}

func TestStringSearchByUtGoFuzzer1(t *testing.T) {
	actualVal := StringSearch("3hllo")

	assert.Equal(t, false, actualVal)
}

func TestStringSearchByUtGoFuzzer2(t *testing.T) {
	actualVal := StringSearch("￴￢ﾪ")

	assert.Equal(t, false, actualVal)
}

func TestStringSearchByUtGoFuzzer3(t *testing.T) {
	actualVal := StringSearch("Ael")

	assert.Equal(t, false, actualVal)
}

func TestStringSearchByUtGoFuzzer4(t *testing.T) {
	actualVal := StringSearch("ABC")

	assert.Equal(t, true, actualVal)
}

func TestStringSearchByUtGoFuzzer5(t *testing.T) {
	actualVal := StringSearch("ABL")

	assert.Equal(t, false, actualVal)
}

package simple

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestDivOrPanicByUtGoFuzzer(t *testing.T) {
	actualVal := DivOrPanic(0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDivOrPanicPanicsByUtGoFuzzer(t *testing.T) {
	assert.PanicsWithValue(t, "div by 0", func() { DivOrPanic(1, 0) })
}

func TestExtendedByUtGoFuzzer1(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(9223372036854775807, 0)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(9223372036854775807), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer2(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(0, -9223372036854775808)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-9223372036854775808), actualVal0)
	assertMultiple.Equal(int64(0), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestArraySumByUtGoFuzzer(t *testing.T) {
	actualVal := ArraySum([5]int{1, 0, 0, 0, 0})

	assert.Equal(t, 1, actualVal)
}

func TestGenerateArrayOfIntegersByUtGoFuzzer(t *testing.T) {
	actualVal := GenerateArrayOfIntegers(9223372036854775807)

	assert.Equal(t, [10]int{9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807}, actualVal)
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

func TestIsIdentityByUtGoFuzzer1(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer2(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 3, 0}, {0, 3, 0}, {0, -9223372036854775808, 2}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer3(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}})

	assert.Equal(t, true, actualVal)
}

func TestBinaryWithNonNilErrorByUtGoFuzzer1(t *testing.T) {
	actualVal, actualErr := Binary([]int{1}, 2, 0, -1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(-1, actualVal)
	assertMultiple.ErrorContains(actualErr, "target not found in array")
}

func TestBinaryByUtGoFuzzer2(t *testing.T) {
	actualVal, actualErr := Binary([]int{9223372036854775807, -1, -1, -1, -1}, 9223372036854775807, 0, 1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0, actualVal)
	assertMultiple.Nil(actualErr)
}

func TestBinaryWithNonNilErrorByUtGoFuzzer3(t *testing.T) {
	actualVal, actualErr := Binary([]int{1, 17592186044417, 257, 1125899906842625}, -9223372036854775808, 1, 2)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(-1, actualVal)
	assertMultiple.ErrorContains(actualErr, "target not found in array")
}

func TestBinaryWithNonNilErrorByUtGoFuzzer4(t *testing.T) {
	actualVal, actualErr := Binary([]int{-1, -1, -1, -1, 9223372036854775807}, 9223372036854775807, 0, 1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(-1, actualVal)
	assertMultiple.ErrorContains(actualErr, "target not found in array")
}

func TestBinaryPanicsByUtGoFuzzer(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: index out of range [-4611686018427387905]", func() { Binary([]int{1}, 2, -9223372036854775808, -1) })
}

func TestStringSearchByUtGoFuzzer1(t *testing.T) {
	actualVal := StringSearch("hello")

	assert.Equal(t, false, actualVal)
}

func TestStringSearchByUtGoFuzzer2(t *testing.T) {
	actualVal := StringSearch("elo")

	assert.Equal(t, false, actualVal)
}

func TestStringSearchByUtGoFuzzer3(t *testing.T) {
	actualVal := StringSearch("Am[")

	assert.Equal(t, false, actualVal)
}

func TestStringSearchByUtGoFuzzer4(t *testing.T) {
	actualVal := StringSearch("AB3")

	assert.Equal(t, false, actualVal)
}

func TestStringSearchByUtGoFuzzer5(t *testing.T) {
	actualVal := StringSearch("ABC")

	assert.Equal(t, true, actualVal)
}
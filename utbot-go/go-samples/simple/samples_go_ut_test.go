package simple

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestDivOrPanicByUtGoFuzzer(t *testing.T) {
	x := 0
	y := -9223372036854775807

	actualVal := DivOrPanic(x, y)

	expectedVal := 0

	assert.Equal(t, expectedVal, actualVal)
}

func TestDivOrPanicPanicsByUtGoFuzzer(t *testing.T) {
	x := -9223372036854775807
	y := 0

	expectedVal := "div by 0"

	assert.PanicsWithValue(t, expectedVal, func() {
		_ = DivOrPanic(x, y)
	})
}

func TestExtendedByUtGoFuzzer1(t *testing.T) {
	a := int64(9223372036854775807)
	b := int64(-9223372036854775808)

	actualVal0, actualVal1, actualVal2 := Extended(a, b)

	expectedVal0 := int64(-1)
	expectedVal1 := int64(1)
	expectedVal2 := int64(1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal0, actualVal0)
	assertMultiple.Equal(expectedVal1, actualVal1)
	assertMultiple.Equal(expectedVal2, actualVal2)
}

func TestExtendedByUtGoFuzzer2(t *testing.T) {
	a := int64(0)
	b := int64(-1)

	actualVal0, actualVal1, actualVal2 := Extended(a, b)

	expectedVal0 := int64(-1)
	expectedVal1 := int64(0)
	expectedVal2 := int64(1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal0, actualVal0)
	assertMultiple.Equal(expectedVal1, actualVal1)
	assertMultiple.Equal(expectedVal2, actualVal2)
}

func TestArraySumByUtGoFuzzer(t *testing.T) {
	array := [5]int{1, 0, 0, 0, 0}

	actualVal := ArraySum(array)

	expectedVal := 1

	assert.Equal(t, expectedVal, actualVal)
}

func TestGenerateArrayOfIntegersByUtGoFuzzer(t *testing.T) {
	num := 9223372036854774783

	actualVal := GenerateArrayOfIntegers(num)

	expectedVal := [10]int{9223372036854774783, 9223372036854774783, 9223372036854774783, 9223372036854774783, 9223372036854774783, 9223372036854774783, 9223372036854774783, 9223372036854774783, 9223372036854774783, 9223372036854774783}

	assert.Equal(t, expectedVal, actualVal)
}

func TestDistanceBetweenTwoPointsByUtGoFuzzer(t *testing.T) {
	a := Point{}
	b := Point{}

	actualVal := DistanceBetweenTwoPoints(a, b)

	expectedVal := 0.0

	assert.Equal(t, expectedVal, actualVal)
}

func TestGetCoordinatesOfMiddleBetweenTwoPointsByUtGoFuzzer(t *testing.T) {
	a := Point{}
	b := Point{}

	actualVal0, actualVal1 := GetCoordinatesOfMiddleBetweenTwoPoints(a, b)

	expectedVal0 := 0.0
	expectedVal1 := 0.0

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal0, actualVal0)
	assertMultiple.Equal(expectedVal1, actualVal1)
}

func TestGetCoordinateSumOfPointsByUtGoFuzzer(t *testing.T) {
	points := []Point{{}}

	actualVal0, actualVal1 := GetCoordinateSumOfPoints(points)

	expectedVal0 := 0.0
	expectedVal1 := 0.0

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal0, actualVal0)
	assertMultiple.Equal(expectedVal1, actualVal1)
}

func TestGetAreaOfCircleByUtGoFuzzer(t *testing.T) {
	circle := Circle{Center: Point{}, Radius: 2.0}

	actualVal := GetAreaOfCircle(circle)

	expectedVal := 12.566370614359172

	assert.Equal(t, expectedVal, actualVal)
}

func TestIsIdentityByUtGoFuzzer1(t *testing.T) {
	matrix := [3][3]int{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}}

	actualVal := IsIdentity(matrix)

	assert.False(t, actualVal)
}

func TestIsIdentityByUtGoFuzzer2(t *testing.T) {
	matrix := [3][3]int{{1, -9223372036854775807, 0}, {1, -9223372036854775807, 0}, {0, 0, 0}}

	actualVal := IsIdentity(matrix)

	assert.False(t, actualVal)
}

func TestIsIdentityByUtGoFuzzer3(t *testing.T) {
	matrix := [3][3]int{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}

	actualVal := IsIdentity(matrix)

	assert.True(t, actualVal)
}

func TestBinaryWithNonNilErrorByUtGoFuzzer1(t *testing.T) {
	array := ([]int)(nil)
	target := 9223372036854775807
	lowIndex := 9223301668110598143
	highIndex := -1

	actualVal, actualErr := Binary(array, target, lowIndex, highIndex)

	expectedVal := -1
	expectedErrorMessage := "target not found in array"

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal, actualVal)
	assertMultiple.ErrorContains(actualErr, expectedErrorMessage)
}

func TestBinaryWithNonNilErrorByUtGoFuzzer2(t *testing.T) {
	array := []int{1, 9223372036854775807}
	target := 9187343239835810815
	lowIndex := 1
	highIndex := 2

	actualVal, actualErr := Binary(array, target, lowIndex, highIndex)

	expectedVal := -1
	expectedErrorMessage := "target not found in array"

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal, actualVal)
	assertMultiple.ErrorContains(actualErr, expectedErrorMessage)
}

func TestBinaryWithNonNilErrorByUtGoFuzzer3(t *testing.T) {
	array := []int{-1, 1, 1048578, 1, 1}
	target := 1
	lowIndex := 1
	highIndex := 1

	actualVal, actualErr := Binary(array, target, lowIndex, highIndex)

	expectedVal := 1

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal, actualVal)
	assertMultiple.Nil(actualErr)
}

func TestBinaryPanicsByUtGoFuzzer1(t *testing.T) {
	array := ([]int)(nil)
	target := 2
	lowIndex := 0
	highIndex := 9223372036854775807

	expectedErrorMessage := "runtime error: index out of range [4611686018427387903] with length 0"

	assert.PanicsWithError(t, expectedErrorMessage, func() {
		_, _ = Binary(array, target, lowIndex, highIndex)
	})
}

func TestBinaryPanicsByUtGoFuzzer2(t *testing.T) {
	array := []int{9223372036854775807, 1}
	target := 9223372036854774783
	lowIndex := 1
	highIndex := 2

	expectedErrorMessage := "runtime error: index out of range [2] with length 2"

	assert.PanicsWithError(t, expectedErrorMessage, func() {
		_, _ = Binary(array, target, lowIndex, highIndex)
	})
}

func TestStringSearchByUtGoFuzzer1(t *testing.T) {
	str := "3hllo"

	actualVal := StringSearch(str)

	assert.False(t, actualVal)
}

func TestStringSearchByUtGoFuzzer2(t *testing.T) {
	str := "￴￢ﾪ"

	actualVal := StringSearch(str)

	assert.False(t, actualVal)
}

func TestStringSearchByUtGoFuzzer3(t *testing.T) {
	str := "Ael"

	actualVal := StringSearch(str)

	assert.False(t, actualVal)
}

func TestStringSearchByUtGoFuzzer4(t *testing.T) {
	str := "ABC"

	actualVal := StringSearch(str)

	assert.True(t, actualVal)
}

func TestStringSearchByUtGoFuzzer5(t *testing.T) {
	str := "ABL"

	actualVal := StringSearch(str)

	assert.False(t, actualVal)
}

func TestSumOfChanElementsByUtGoFuzzer(t *testing.T) {
	c := make(chan int, 3)
	c <- 0
	c <- 0
	c <- 9223372036854775807
	close(c)

	actualVal := SumOfChanElements(c)

	expectedVal := 9223372036854775807

	assert.Equal(t, expectedVal, actualVal)
}

// SumOfChanElements((<-chan int)(nil)) exceeded 1000 ms timeout

package simple

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestDivOrPanicByUtGoFuzzer(t *testing.T) {
	x := 1
	y := 1

	actualVal := DivOrPanic(x, y)

	expectedVal := 1

	assert.Equal(t, expectedVal, actualVal)
}

func TestDivOrPanicPanicsByUtGoFuzzer(t *testing.T) {
	x := 0
	y := 0

	expectedVal := "div by 0"

	assert.PanicsWithValue(t, expectedVal, func() {
		_ = DivOrPanic(x, y)
	})
}

func TestExtendedByUtGoFuzzer1(t *testing.T) {
	a := int64(1)
	b := int64(9)

	actualVal0, actualVal1, actualVal2 := Extended(a, b)

	expectedVal0 := int64(1)
	expectedVal1 := int64(1)
	expectedVal2 := int64(0)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal0, actualVal0)
	assertMultiple.Equal(expectedVal1, actualVal1)
	assertMultiple.Equal(expectedVal2, actualVal2)
}

func TestExtendedByUtGoFuzzer2(t *testing.T) {
	a := int64(0)
	b := int64(0)

	actualVal0, actualVal1, actualVal2 := Extended(a, b)

	expectedVal0 := int64(0)
	expectedVal1 := int64(0)
	expectedVal2 := int64(1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal0, actualVal0)
	assertMultiple.Equal(expectedVal1, actualVal1)
	assertMultiple.Equal(expectedVal2, actualVal2)
}

func TestArraySumByUtGoFuzzer(t *testing.T) {
	array := [5]int{0, 0, 0, 0, 0}

	actualVal := ArraySum(array)

	expectedVal := 0

	assert.Equal(t, expectedVal, actualVal)
}

func TestGenerateArrayOfIntegersByUtGoFuzzer(t *testing.T) {
	num := 1

	actualVal := GenerateArrayOfIntegers(num)

	expectedVal := [10]int{1, 1, 1, 1, 1, 1, 1, 1, 1, 1}

	assert.Equal(t, expectedVal, actualVal)
}

func TestDistanceBetweenTwoPointsByUtGoFuzzer(t *testing.T) {
	a := Point{x: 2.0, y: 2.0}
	b := Point{x: 2.0, y: 2.0}

	actualVal := DistanceBetweenTwoPoints(a, b)

	expectedVal := 0.0

	assert.Equal(t, expectedVal, actualVal)
}

func TestGetCoordinatesOfMiddleBetweenTwoPointsByUtGoFuzzer(t *testing.T) {
	a := Point{x: 2.0, y: 2.0}
	b := Point{x: 2.0, y: 2.0}

	actualVal0, actualVal1 := GetCoordinatesOfMiddleBetweenTwoPoints(a, b)

	expectedVal0 := 2.0
	expectedVal1 := 2.0

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal0, actualVal0)
	assertMultiple.Equal(expectedVal1, actualVal1)
}

func TestGetCoordinateSumOfPointsByUtGoFuzzer1(t *testing.T) {
	points := []Point{{x: 0.0, y: 0.0}}

	actualVal0, actualVal1 := GetCoordinateSumOfPoints(points)

	expectedVal0 := 0.0
	expectedVal1 := 0.0

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal0, actualVal0)
	assertMultiple.Equal(expectedVal1, actualVal1)
}

func TestGetCoordinateSumOfPointsByUtGoFuzzer2(t *testing.T) {
	points := ([]Point)(nil)

	actualVal0, actualVal1 := GetCoordinateSumOfPoints(points)

	expectedVal0 := 0.0
	expectedVal1 := 0.0

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal0, actualVal0)
	assertMultiple.Equal(expectedVal1, actualVal1)
}

func TestGetAreaOfCircleByUtGoFuzzer(t *testing.T) {
	circle := Circle{Center: Point{x: 2.0, y: 2.0}, Radius: 2.0}

	actualVal := GetAreaOfCircle(circle)

	expectedVal := 12.566370614359172

	assert.Equal(t, expectedVal, actualVal)
}

func TestIsIdentityByUtGoFuzzer1(t *testing.T) {
	matrix := [3][3]int{{1, 0, 3}, {0, 0, 0}, {0, 0, 0}}

	actualVal := IsIdentity(matrix)

	assert.False(t, actualVal)
}

func TestIsIdentityByUtGoFuzzer2(t *testing.T) {
	matrix := [3][3]int{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}}

	actualVal := IsIdentity(matrix)

	assert.False(t, actualVal)
}

func TestIsIdentityByUtGoFuzzer3(t *testing.T) {
	matrix := [3][3]int{{1, 0, 0}, {0, 0, 0}, {0, 0, 0}}

	actualVal := IsIdentity(matrix)

	assert.False(t, actualVal)
}

func TestIsIdentityByUtGoFuzzer4(t *testing.T) {
	matrix := [3][3]int{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}

	actualVal := IsIdentity(matrix)

	assert.True(t, actualVal)
}

func TestBinaryWithNonNilErrorByUtGoFuzzer1(t *testing.T) {
	array := ([]int)(nil)
	target := 2
	lowIndex := 2
	highIndex := 1

	actualVal, actualErr := Binary(array, target, lowIndex, highIndex)

	expectedVal := -1
	expectedErrorMessage := "target not found in array"

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal, actualVal)
	assertMultiple.ErrorContains(actualErr, expectedErrorMessage)
}

func TestBinaryByUtGoFuzzer2(t *testing.T) {
	array := []int{0}
	target := 0
	lowIndex := 0
	highIndex := 0

	actualVal, actualErr := Binary(array, target, lowIndex, highIndex)

	expectedVal := 0

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal, actualVal)
	assertMultiple.Nil(actualErr)
}

func TestBinaryWithNonNilErrorByUtGoFuzzer3(t *testing.T) {
	array := []int{1, 33554433}
	target := 16385
	lowIndex := 0
	highIndex := 1

	actualVal, actualErr := Binary(array, target, lowIndex, highIndex)

	expectedVal := -1
	expectedErrorMessage := "target not found in array"

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal, actualVal)
	assertMultiple.ErrorContains(actualErr, expectedErrorMessage)
}

func TestBinaryWithNonNilErrorByUtGoFuzzer4(t *testing.T) {
	array := []int{1}
	target := 2
	lowIndex := 0
	highIndex := 0

	actualVal, actualErr := Binary(array, target, lowIndex, highIndex)

	expectedVal := -1
	expectedErrorMessage := "target not found in array"

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal, actualVal)
	assertMultiple.ErrorContains(actualErr, expectedErrorMessage)
}

func TestBinaryByUtGoFuzzer5(t *testing.T) {
	array := []int{-1, 2}
	target := 2
	lowIndex := -1
	highIndex := 1

	actualVal, actualErr := Binary(array, target, lowIndex, highIndex)

	expectedVal := 1

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal, actualVal)
	assertMultiple.Nil(actualErr)
}

func TestBinaryWithNonNilErrorByUtGoFuzzer6(t *testing.T) {
	array := []int{1}
	target := 0
	lowIndex := 0
	highIndex := 0

	actualVal, actualErr := Binary(array, target, lowIndex, highIndex)

	expectedVal := -1
	expectedErrorMessage := "target not found in array"

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal, actualVal)
	assertMultiple.ErrorContains(actualErr, expectedErrorMessage)
}

func TestBinaryByUtGoFuzzer7(t *testing.T) {
	array := []int{-9151296850630803456, 0, 70}
	target := 0
	lowIndex := -1
	highIndex := 5

	actualVal, actualErr := Binary(array, target, lowIndex, highIndex)

	expectedVal := 1

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal, actualVal)
	assertMultiple.Nil(actualErr)
}

func TestBinaryByUtGoFuzzer8(t *testing.T) {
	array := []int{1, 65}
	target := 1
	lowIndex := 0
	highIndex := 2

	actualVal, actualErr := Binary(array, target, lowIndex, highIndex)

	expectedVal := 0

	assertMultiple := assert.New(t)
	assertMultiple.Equal(expectedVal, actualVal)
	assertMultiple.Nil(actualErr)
}

func TestBinaryPanicsByUtGoFuzzer1(t *testing.T) {
	array := ([]int)(nil)
	target := 2
	lowIndex := 2
	highIndex := 2

	expectedErrorMessage := "runtime error: index out of range [2] with length 0"

	assert.PanicsWithError(t, expectedErrorMessage, func() {
		_, _ = Binary(array, target, lowIndex, highIndex)
	})
}

func TestBinaryPanicsByUtGoFuzzer2(t *testing.T) {
	array := []int{2}
	target := 1
	lowIndex := -1
	highIndex := 2

	expectedErrorMessage := "runtime error: index out of range [-1]"

	assert.PanicsWithError(t, expectedErrorMessage, func() {
		_, _ = Binary(array, target, lowIndex, highIndex)
	})
}

func TestBinaryPanicsByUtGoFuzzer3(t *testing.T) {
	array := []int{1}
	target := 2
	lowIndex := 0
	highIndex := 1

	expectedErrorMessage := "runtime error: index out of range [1] with length 1"

	assert.PanicsWithError(t, expectedErrorMessage, func() {
		_, _ = Binary(array, target, lowIndex, highIndex)
	})
}

func TestStringSearchByUtGoFuzzer1(t *testing.T) {
	str := ""

	actualVal := StringSearch(str)

	assert.False(t, actualVal)
}

func TestStringSearchByUtGoFuzzer2(t *testing.T) {
	str := "°"

	actualVal := StringSearch(str)

	assert.False(t, actualVal)
}

func TestStringSearchByUtGoFuzzer3(t *testing.T) {
	str := "A7j"

	actualVal := StringSearch(str)

	assert.False(t, actualVal)
}

func TestStringSearchByUtGoFuzzer4(t *testing.T) {
	str := "ABY"

	actualVal := StringSearch(str)

	assert.False(t, actualVal)
}

func TestStringSearchByUtGoFuzzer5(t *testing.T) {
	str := "ABC"

	actualVal := StringSearch(str)

	assert.True(t, actualVal)
}

func TestSumOfChanElementsByUtGoFuzzer1(t *testing.T) {
	c := make(chan int, 1)
	c <- 0
	close(c)

	actualVal := SumOfChanElements(c)

	expectedVal := 0

	assert.Equal(t, expectedVal, actualVal)
}

func TestSumOfChanElementsByUtGoFuzzer2(t *testing.T) {
	c := make(chan int, 0)

	close(c)

	actualVal := SumOfChanElements(c)

	expectedVal := 0

	assert.Equal(t, expectedVal, actualVal)
}

// SumOfChanElements((<-chan int)(nil)) exceeded 1000 ms timeout

func TestLenOfListByUtGoFuzzer1(t *testing.T) {
	l := &List{tail: (*List)(nil), val: 1}

	actualVal := LenOfList(l)

	expectedVal := 1

	assert.Equal(t, expectedVal, actualVal)
}

func TestLenOfListByUtGoFuzzer2(t *testing.T) {
	l := (*List)(nil)

	actualVal := LenOfList(l)

	expectedVal := 0

	assert.Equal(t, expectedVal, actualVal)
}

func TestLenOfListByUtGoFuzzer3(t *testing.T) {
	l := &List{tail: &List{}, val: 1}

	actualVal := LenOfList(l)

	expectedVal := 2

	assert.Equal(t, expectedVal, actualVal)
}

func TestGetLastNodeByUtGoFuzzer1(t *testing.T) {
	n := &Node{prev: (*Node)(nil), next: &Node{}, val: 1}

	actualVal := GetLastNode(n)

	expectedVal := &Node{prev: (*Node)(nil), next: (*Node)(nil), val: 0}

	assert.Equal(t, expectedVal, actualVal)
}

func TestGetLastNodeByUtGoFuzzer2(t *testing.T) {
	n := (*Node)(nil)

	actualVal := GetLastNode(n)

	assert.Nil(t, actualVal)
}

func TestGetLastNodeByUtGoFuzzer3(t *testing.T) {
	n := &Node{prev: (*Node)(nil), next: (*Node)(nil), val: 1}

	actualVal := GetLastNode(n)

	expectedVal := &Node{prev: (*Node)(nil), next: (*Node)(nil), val: 1}

	assert.Equal(t, expectedVal, actualVal)
}

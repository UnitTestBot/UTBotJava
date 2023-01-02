package simple

import (
	"github.com/stretchr/testify/assert"
	"math"
	"testing"
)

func TestDivOrPanicByUtGoFuzzer(t *testing.T) {
	actualVal := DivOrPanic(2147483647, -1)

	assert.Equal(t, -2147483647, actualVal)
}

func TestDivOrPanicPanicsByUtGoFuzzer(t *testing.T) {
	assert.PanicsWithValue(t, "div by 0", func() { DivOrPanic(2147483647, 0) })
}

func TestExtendedByUtGoFuzzer(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(2147483647, 4294967295)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(1), actualVal0)
	assertMultiple.Equal(int64(-2), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestArraySumByUtGoFuzzer(t *testing.T) {
	actualVal := ArraySum([5]int{-1, 0, 0, 0, 0})

	assert.Equal(t, -1, actualVal)
}

func TestGenerateArrayOfIntegersByUtGoFuzzer(t *testing.T) {
	actualVal := GenerateArrayOfIntegers(2147483647)

	assert.Equal(t, [10]int{2147483647, 2147483647, 2147483647, 2147483647, 2147483647, 2147483647, 2147483647, 2147483647, 2147483647, 2147483647}, actualVal)
}

func TestDistanceBetweenTwoPointsByUtGoFuzzer(t *testing.T) {
	actualVal := DistanceBetweenTwoPoints(Point{x: math.MaxFloat64, y: math.Inf(-1)}, Point{x: math.Inf(-1), y: math.NaN()})

	assert.True(t, math.IsNaN(actualVal))
}

func TestGetCoordinatesOfMiddleBetweenTwoPointsByUtGoFuzzer(t *testing.T) {
	actualVal0, actualVal1 := GetCoordinatesOfMiddleBetweenTwoPoints(Point{x: math.MaxFloat64, y: math.Inf(-1)}, Point{x: math.Inf(-1), y: math.NaN()})

	assertMultiple := assert.New(t)
	assertMultiple.True(math.IsInf(actualVal0, -1))
	assertMultiple.True(math.IsNaN(actualVal1))
}

func TestGetCoordinateSumOfPointsByUtGoFuzzer(t *testing.T) {
	actualVal0, actualVal1 := GetCoordinateSumOfPoints([10]Point{{x: 0.0, y: -1.1}, {}, {}, {}, {}, {}, {}, {}, {}, {}})

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0.0, actualVal0)
	assertMultiple.Equal(-1.1, actualVal1)
}

func TestGetAreaOfCircleByUtGoFuzzer(t *testing.T) {
	actualVal := GetAreaOfCircle(Circle{Center: Point{x: 0.0, y: -1.1}, Radius: math.MaxFloat64})

	assert.True(t, math.IsInf(actualVal, 1))
}

func TestIsIdentityByUtGoFuzzer1(t *testing.T) {
	actualVal := isIdentity(Matrix{body: [2][2]int{[2]int{2147483647, 0}, [2]int{0, 0}}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer2(t *testing.T) {
	actualVal := isIdentity(Matrix{body: [2][2]int{[2]int{1, 0}, [2]int{0, 0}}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer3(t *testing.T) {
	actualVal := isIdentity(Matrix{body: [2][2]int{[2]int{1, 0}, [2]int{2147483647, 2147483645}}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer4(t *testing.T) {
	actualVal := isIdentity(Matrix{body: [2][2]int{[2]int{1, -2147483647}, [2]int{1, -2147418111}}})

	assert.Equal(t, false, actualVal)
}

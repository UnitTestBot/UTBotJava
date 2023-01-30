package simple

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestDivOrPanicByUtGoFuzzer(t *testing.T) {
	actualVal := DivOrPanic(9223372036854775807, -1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDivOrPanicPanicsByUtGoFuzzer(t *testing.T) {
	assert.PanicsWithValue(t, "div by 0", func() { DivOrPanic(9223372036854775807, 0) })
}

func TestExtendedByUtGoFuzzer1(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(9223372036854775807, -1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-1), actualVal0)
	assertMultiple.Equal(int64(0), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestExtendedByUtGoFuzzer2(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(0, 9223372036854775807)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(9223372036854775807), actualVal0)
	assertMultiple.Equal(int64(0), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestArraySumByUtGoFuzzer(t *testing.T) {
	actualVal := ArraySum([5]int{-1, 0, 0, 0, 0})

	assert.Equal(t, -1, actualVal)
}

func TestGenerateArrayOfIntegersByUtGoFuzzer(t *testing.T) {
	actualVal := GenerateArrayOfIntegers(9223372036854775807)

	assert.Equal(t, [10]int{9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807, 9223372036854775807}, actualVal)
}

func TestDistanceBetweenTwoPointsByUtGoFuzzer(t *testing.T) {
	actualVal := DistanceBetweenTwoPoints(Point{x: 0.730967787376657, y: 0.730967787376657}, Point{x: 0.730967787376657, y: 0.730967787376657})

	assert.Equal(t, 0.0, actualVal)
}

func TestGetCoordinatesOfMiddleBetweenTwoPointsByUtGoFuzzer(t *testing.T) {
	actualVal0, actualVal1 := GetCoordinatesOfMiddleBetweenTwoPoints(Point{x: 0.24053641567148587, y: 0.24053641567148587}, Point{x: 0.24053641567148587, y: 0.24053641567148587})

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0.24053641567148587, actualVal0)
	assertMultiple.Equal(0.24053641567148587, actualVal1)
}

func TestGetCoordinateSumOfPointsByUtGoFuzzer(t *testing.T) {
	actualVal0, actualVal1 := GetCoordinateSumOfPoints([10]Point{{x: 0.6374174253501083, y: 0.6374174253501083}, {}, {}, {}, {}, {}, {}, {}, {}, {}})

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0.6374174253501083, actualVal0)
	assertMultiple.Equal(0.6374174253501083, actualVal1)
}

func TestGetAreaOfCircleByUtGoFuzzer(t *testing.T) {
	actualVal := GetAreaOfCircle(Circle{Center: Point{x: 0.5504370051176339, y: 0.5504370051176339}, Radius: 0.5504370051176339})

	assert.Equal(t, 0.9518425589456255, actualVal)
}

func TestIsIdentityByUtGoFuzzer1(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer2(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 0, -9223372036854775808}, {-9223372036854775808, 1, -9223372036854775808}, {9223372036854775807, -1, 9223372036854775805}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer3(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 0, 0}, {0, 0, 0}, {0, 0, 0}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer4(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 288230376151711745, 513}, {1, 9223372036854775807, 9223372036854775807}, {1, 129, 32769}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer5(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 0, 0}, {0, 1, 0}, {0, 0, 0}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer6(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 0, 0}, {1, 0, 0}, {0, 0, 0}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer7(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 0, 0}, {0, 1, 0}, {1, 0, 0}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer8(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 0, 0}, {0, 1, 0}, {0, 1, 0}})

	assert.Equal(t, false, actualVal)
}

func TestBinaryWithNonNilErrorByUtGoFuzzer1(t *testing.T) {
	actualVal, actualErr := Binary([10]int{-9223372036854775808, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 9223372036854775807, 9223372036854775807, -1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(-1, actualVal)
	assertMultiple.ErrorContains(actualErr, "target not found in array")
}

func TestBinaryWithNonNilErrorByUtGoFuzzer2(t *testing.T) {
	actualVal, actualErr := Binary([10]int{9223372036854775807, -9223372036854775808, 0, 0, 0, 0, 0, 0, 0, 0}, -1, 1, 1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(-1, actualVal)
	assertMultiple.ErrorContains(actualErr, "target not found in array")
}

func TestBinaryWithNonNilErrorByUtGoFuzzer3(t *testing.T) {
	actualVal, actualErr := Binary([10]int{9223372036854775807, 0, 0, 0, 0, 0, 0, 0, 0, 0}, -1, 0, 0)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(-1, actualVal)
	assertMultiple.ErrorContains(actualErr, "target not found in array")
}

func TestBinaryByUtGoFuzzer4(t *testing.T) {
	actualVal, actualErr := Binary([10]int{9223372036854775807, 1, 9223372036854775807, -1, 0, 0, 0, 0, 0, 0}, 9223372036854775807, 0, 1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0, actualVal)
	assertMultiple.Nil(actualErr)
}

func TestBinaryPanicsByUtGoFuzzer(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: index out of range [-9223372036854775808]", func() { Binary([10]int{-1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, -1, -9223372036854775808, 9223372036854775807) })
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

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
	actualVal := DistanceBetweenTwoPoints(Point{x: 0.990722785714783, y: 0.990722785714783}, Point{x: 2.0, y: 2.0})

	assert.Equal(t, 1.4273335246362906, actualVal)
}

func TestGetCoordinatesOfMiddleBetweenTwoPointsByUtGoFuzzer(t *testing.T) {
	actualVal0, actualVal1 := GetCoordinatesOfMiddleBetweenTwoPoints(Point{x: 0.4872328470301428, y: 0.4872328470301428}, Point{x: 2.0, y: 2.0})

	assertMultiple := assert.New(t)
	assertMultiple.Equal(1.2436164235150713, actualVal0)
	assertMultiple.Equal(1.2436164235150713, actualVal1)
}

func TestGetCoordinateSumOfPointsByUtGoFuzzer1(t *testing.T) {
	actualVal0, actualVal1 := GetCoordinateSumOfPoints([]Point{{x: 0.7462414053223305, y: 0.7462414053223305}})

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0.7462414053223305, actualVal0)
	assertMultiple.Equal(0.7462414053223305, actualVal1)
}

func TestGetCoordinateSumOfPointsByUtGoFuzzer2(t *testing.T) {
	actualVal0, actualVal1 := GetCoordinateSumOfPoints([]Point{{x: 0.7462414053223305, y: 0.0}, {x: 0.7462414053223305, y: 0.0}, {x: 0.7462414053223305, y: 2.8480945388892178e-306}, {x: 0.7462414053223305, y: 0.0}})

	assertMultiple := assert.New(t)
	assertMultiple.Equal(2.984965621289322, actualVal0)
	assertMultiple.Equal(2.8480945388892178e-306, actualVal1)
}

func TestGetCoordinateSumOfPointsByUtGoFuzzer3(t *testing.T) {
	actualVal0, actualVal1 := GetCoordinateSumOfPoints([]Point{{x: 0.7462414053223305, y: 0.7462414053223305}, {x: 0.0, y: 0.7462414053223305}})

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0.7462414053223305, actualVal0)
	assertMultiple.Equal(1.492482810644661, actualVal1)
}

func TestGetCoordinateSumOfPointsByUtGoFuzzer4(t *testing.T) {
	actualVal0, actualVal1 := GetCoordinateSumOfPoints([]Point{{x: 0.0, y: 0.0}, {x: 0.0, y: 0.0}, {x: 0.0, y: 0.0}})

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0.0, actualVal0)
	assertMultiple.Equal(0.0, actualVal1)
}

func TestGetCoordinateSumOfPointsByUtGoFuzzer5(t *testing.T) {
	actualVal0, actualVal1 := GetCoordinateSumOfPoints([]Point{{x: 0.0, y: 0.7462414053223305}, {x: 0.0, y: 0.7462414053223305}, {x: 4.7783097267364807e-299, y: 0.7462414053223305}, {x: 0.0, y: 0.7462414053223305}, {x: 0.0, y: 0.7462414053223305}})

	assertMultiple := assert.New(t)
	assertMultiple.Equal(4.7783097267364807e-299, actualVal0)
	assertMultiple.Equal(3.731207026611653, actualVal1)
}

func TestGetCoordinateSumOfPointsByUtGoFuzzer6(t *testing.T) {
	actualVal0, actualVal1 := GetCoordinateSumOfPoints([]Point{})

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0.0, actualVal0)
	assertMultiple.Equal(0.0, actualVal1)
}

func TestGetAreaOfCircleByUtGoFuzzer(t *testing.T) {
	actualVal := GetAreaOfCircle(Circle{Center: Point{x: 0.7331520701949938, y: 0.7331520701949938}, Radius: 2.0})

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
	actualVal := IsIdentity([3][3]int{{1, 0, 0}, {0, 0, 0}, {0, 0, 0}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer4(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 0, 1}, {1, 0, 1}, {0, 1, 1}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer5(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 0, 0}, {0, 1, 2048}, {0, 0, 0}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer6(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 0, 0}, {2199023255552, 1, 2048}, {0, 0, 3}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer7(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 0, 0}, {0, 1, 0}, {0, 0, 0}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer8(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 0, 0}, {0, 1, 0}, {0, 1, 0}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer9(t *testing.T) {
	actualVal := IsIdentity([3][3]int{{1, 0, 0}, {0, 1, 0}, {18014398509481984, 1, 0}})

	assert.Equal(t, false, actualVal)
}

func TestIsIdentityByUtGoFuzzer10(t *testing.T) {
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

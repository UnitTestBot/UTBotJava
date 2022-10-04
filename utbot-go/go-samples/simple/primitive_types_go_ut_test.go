package simple

import (
	"github.com/stretchr/testify/assert"
	"math"
	"testing"
)

func TestAbsByUtGoFuzzer1(t *testing.T) {
	actualVal := Abs(1)

	assert.Equal(t, 1, actualVal)
}

func TestAbsByUtGoFuzzer2(t *testing.T) {
	actualVal := Abs(math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestAbsByUtGoFuzzer3(t *testing.T) {
	actualVal := Abs(math.MaxInt)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestAbsByUtGoFuzzer4(t *testing.T) {
	actualVal := Abs(0)

	assert.Equal(t, 0, actualVal)
}

func TestAbsByUtGoFuzzer5(t *testing.T) {
	actualVal := Abs(-1)

	assert.Equal(t, 1, actualVal)
}

func TestDivOrPanicByUtGoFuzzer1(t *testing.T) {
	actualVal := DivOrPanic(-1, 1)

	assert.Equal(t, -1, actualVal)
}

func TestDivOrPanicByUtGoFuzzer2(t *testing.T) {
	actualVal := DivOrPanic(math.MinInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDivOrPanicByUtGoFuzzer3(t *testing.T) {
	actualVal := DivOrPanic(0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDivOrPanicByUtGoFuzzer4(t *testing.T) {
	actualVal := DivOrPanic(1, 1)

	assert.Equal(t, 1, actualVal)
}

func TestDivOrPanicByUtGoFuzzer5(t *testing.T) {
	actualVal := DivOrPanic(math.MaxInt, 1)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestDivOrPanicByUtGoFuzzer6(t *testing.T) {
	actualVal := DivOrPanic(-1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDivOrPanicByUtGoFuzzer7(t *testing.T) {
	actualVal := DivOrPanic(math.MinInt, math.MaxInt)

	assert.Equal(t, -1, actualVal)
}

func TestDivOrPanicByUtGoFuzzer8(t *testing.T) {
	actualVal := DivOrPanic(0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDivOrPanicByUtGoFuzzer9(t *testing.T) {
	actualVal := DivOrPanic(1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDivOrPanicByUtGoFuzzer10(t *testing.T) {
	actualVal := DivOrPanic(math.MaxInt, math.MaxInt)

	assert.Equal(t, 1, actualVal)
}

func TestDivOrPanicByUtGoFuzzer11(t *testing.T) {
	actualVal := DivOrPanic(-1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDivOrPanicByUtGoFuzzer12(t *testing.T) {
	actualVal := DivOrPanic(math.MinInt, math.MinInt)

	assert.Equal(t, 1, actualVal)
}

func TestDivOrPanicByUtGoFuzzer13(t *testing.T) {
	actualVal := DivOrPanic(0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDivOrPanicByUtGoFuzzer14(t *testing.T) {
	actualVal := DivOrPanic(1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDivOrPanicByUtGoFuzzer15(t *testing.T) {
	actualVal := DivOrPanic(math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDivOrPanicByUtGoFuzzer16(t *testing.T) {
	actualVal := DivOrPanic(-1, -1)

	assert.Equal(t, 1, actualVal)
}

func TestDivOrPanicByUtGoFuzzer17(t *testing.T) {
	actualVal := DivOrPanic(math.MinInt, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDivOrPanicByUtGoFuzzer18(t *testing.T) {
	actualVal := DivOrPanic(0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDivOrPanicByUtGoFuzzer19(t *testing.T) {
	actualVal := DivOrPanic(1, -1)

	assert.Equal(t, -1, actualVal)
}

func TestDivOrPanicByUtGoFuzzer20(t *testing.T) {
	actualVal := DivOrPanic(math.MaxInt, -1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDivOrPanicPanicsByUtGoFuzzer1(t *testing.T) {
	assert.PanicsWithValue(t, "div by 0", func() { DivOrPanic(-1, 0) })
}

func TestDivOrPanicPanicsByUtGoFuzzer2(t *testing.T) {
	assert.PanicsWithValue(t, "div by 0", func() { DivOrPanic(math.MinInt, 0) })
}

func TestDivOrPanicPanicsByUtGoFuzzer3(t *testing.T) {
	assert.PanicsWithValue(t, "div by 0", func() { DivOrPanic(0, 0) })
}

func TestDivOrPanicPanicsByUtGoFuzzer4(t *testing.T) {
	assert.PanicsWithValue(t, "div by 0", func() { DivOrPanic(1, 0) })
}

func TestDivOrPanicPanicsByUtGoFuzzer5(t *testing.T) {
	assert.PanicsWithValue(t, "div by 0", func() { DivOrPanic(math.MaxInt, 0) })
}

func TestCatalanRecursiveByUtGoFuzzer1(t *testing.T) {
	actualVal := CatalanRecursive(1)

	assert.Equal(t, 1, actualVal)
}

func TestCatalanRecursiveByUtGoFuzzer2(t *testing.T) {
	actualVal := CatalanRecursive(0)

	assert.Equal(t, 1, actualVal)
}

// CatalanRecursive(math.MinInt) exceeded 1000 ms timeout

// CatalanRecursive(math.MaxInt) exceeded 1000 ms timeout

// CatalanRecursive(-1) exceeded 1000 ms timeout

func TestBitwiseByUtGoFuzzer1(t *testing.T) {
	actualVal := Bitwise(-1, math.MinInt, math.MaxInt)

	assert.Equal(t, -1, actualVal)
}

func TestBitwiseByUtGoFuzzer2(t *testing.T) {
	actualVal := Bitwise(0, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer3(t *testing.T) {
	actualVal := Bitwise(math.MinInt, 1, 0)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer4(t *testing.T) {
	actualVal := Bitwise(1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer5(t *testing.T) {
	actualVal := Bitwise(1, -1, 1)

	assert.Equal(t, -1, actualVal)
}

func TestBitwiseByUtGoFuzzer6(t *testing.T) {
	actualVal := Bitwise(math.MinInt, math.MinInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer7(t *testing.T) {
	actualVal := Bitwise(-1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer8(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, 1, 0)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestBitwiseByUtGoFuzzer9(t *testing.T) {
	actualVal := Bitwise(0, 1, 1)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer10(t *testing.T) {
	actualVal := Bitwise(-1, -1, 1)

	assert.Equal(t, -1, actualVal)
}

func TestBitwiseByUtGoFuzzer11(t *testing.T) {
	actualVal := Bitwise(1, -1, math.MaxInt)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer12(t *testing.T) {
	actualVal := Bitwise(math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer13(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, math.MinInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer14(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestBitwiseByUtGoFuzzer15(t *testing.T) {
	actualVal := Bitwise(-1, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer16(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, 1, 1)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer17(t *testing.T) {
	actualVal := Bitwise(math.MinInt, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer18(t *testing.T) {
	actualVal := Bitwise(1, 0, math.MaxInt)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer19(t *testing.T) {
	actualVal := Bitwise(1, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer20(t *testing.T) {
	actualVal := Bitwise(math.MinInt, math.MaxInt, 0)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestBitwiseByUtGoFuzzer21(t *testing.T) {
	actualVal := Bitwise(-1, math.MinInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer22(t *testing.T) {
	actualVal := Bitwise(1, math.MinInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer23(t *testing.T) {
	actualVal := Bitwise(math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer24(t *testing.T) {
	actualVal := Bitwise(0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer25(t *testing.T) {
	actualVal := Bitwise(0, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer26(t *testing.T) {
	actualVal := Bitwise(-1, 1, 0)

	assert.Equal(t, -1, actualVal)
}

func TestBitwiseByUtGoFuzzer27(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, -1, 0)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestBitwiseByUtGoFuzzer28(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestBitwiseByUtGoFuzzer29(t *testing.T) {
	actualVal := Bitwise(-1, math.MaxInt, 1)

	assert.Equal(t, -1, actualVal)
}

func TestBitwiseByUtGoFuzzer30(t *testing.T) {
	actualVal := Bitwise(1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestBitwiseByUtGoFuzzer31(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestBitwiseByUtGoFuzzer32(t *testing.T) {
	actualVal := Bitwise(0, 1, 0)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer33(t *testing.T) {
	actualVal := Bitwise(0, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer34(t *testing.T) {
	actualVal := Bitwise(-1, -1, 0)

	assert.Equal(t, -1, actualVal)
}

func TestBitwiseByUtGoFuzzer35(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer36(t *testing.T) {
	actualVal := Bitwise(-1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestBitwiseByUtGoFuzzer37(t *testing.T) {
	actualVal := Bitwise(0, math.MinInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer38(t *testing.T) {
	actualVal := Bitwise(math.MinInt, -1, 0)

	assert.Equal(t, -1, actualVal)
}

func TestBitwiseByUtGoFuzzer39(t *testing.T) {
	actualVal := Bitwise(1, 1, 0)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer40(t *testing.T) {
	actualVal := Bitwise(1, math.MinInt, 1)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer41(t *testing.T) {
	actualVal := Bitwise(math.MinInt, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer42(t *testing.T) {
	actualVal := Bitwise(1, math.MaxInt, 1)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestBitwiseByUtGoFuzzer43(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer44(t *testing.T) {
	actualVal := Bitwise(math.MinInt, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer45(t *testing.T) {
	actualVal := Bitwise(0, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestBitwiseByUtGoFuzzer46(t *testing.T) {
	actualVal := Bitwise(1, -1, 0)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer47(t *testing.T) {
	actualVal := Bitwise(math.MinInt, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer48(t *testing.T) {
	actualVal := Bitwise(1, math.MaxInt, 0)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer49(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer50(t *testing.T) {
	actualVal := Bitwise(0, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer51(t *testing.T) {
	actualVal := Bitwise(-1, 1, 1)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer52(t *testing.T) {
	actualVal := Bitwise(1, 1, math.MaxInt)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer53(t *testing.T) {
	actualVal := Bitwise(math.MinInt, -1, math.MaxInt)

	assert.Equal(t, -1, actualVal)
}

func TestBitwiseByUtGoFuzzer54(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, -1, 1)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestBitwiseByUtGoFuzzer55(t *testing.T) {
	actualVal := Bitwise(math.MinInt, 0, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer56(t *testing.T) {
	actualVal := Bitwise(-1, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer57(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestBitwiseByUtGoFuzzer58(t *testing.T) {
	actualVal := Bitwise(0, -1, 0)

	assert.Equal(t, -1, actualVal)
}

func TestBitwiseByUtGoFuzzer59(t *testing.T) {
	actualVal := Bitwise(0, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer60(t *testing.T) {
	actualVal := Bitwise(-1, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer61(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestBitwiseByUtGoFuzzer62(t *testing.T) {
	actualVal := Bitwise(-1, math.MaxInt, 0)

	assert.Equal(t, -1, actualVal)
}

func TestBitwiseByUtGoFuzzer63(t *testing.T) {
	actualVal := Bitwise(0, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer64(t *testing.T) {
	actualVal := Bitwise(0, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestBitwiseByUtGoFuzzer65(t *testing.T) {
	actualVal := Bitwise(math.MinInt, 1, 1)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer66(t *testing.T) {
	actualVal := Bitwise(1, 0, 1)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer67(t *testing.T) {
	actualVal := Bitwise(-1, 1, math.MaxInt)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer68(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, -1, actualVal)
}

func TestBitwiseByUtGoFuzzer69(t *testing.T) {
	actualVal := Bitwise(math.MinInt, 0, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer70(t *testing.T) {
	actualVal := Bitwise(0, 1, math.MaxInt)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer71(t *testing.T) {
	actualVal := Bitwise(0, math.MaxInt, 0)

	assert.Equal(t, 9223372036854775807, actualVal)
}

func TestBitwiseByUtGoFuzzer72(t *testing.T) {
	actualVal := Bitwise(math.MinInt, -1, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwiseByUtGoFuzzer73(t *testing.T) {
	actualVal := Bitwise(1, 1, 1)

	assert.Equal(t, 1, actualVal)
}

func TestBitwiseByUtGoFuzzer74(t *testing.T) {
	actualVal := Bitwise(-1, -1, math.MaxInt)

	assert.Equal(t, -1, actualVal)
}

func TestBitwiseByUtGoFuzzer75(t *testing.T) {
	actualVal := Bitwise(math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestBitwisePanicsByUtGoFuzzer1(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(1, math.MaxInt, -1) })
}

func TestBitwisePanicsByUtGoFuzzer2(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MaxInt, 0, -1) })
}

func TestBitwisePanicsByUtGoFuzzer3(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(0, 0, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer4(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(-1, 1, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer5(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MaxInt, -1, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer6(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MaxInt, math.MaxInt, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer7(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(-1, math.MaxInt, -1) })
}

func TestBitwisePanicsByUtGoFuzzer8(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(0, math.MinInt, -1) })
}

func TestBitwisePanicsByUtGoFuzzer9(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MinInt, 1, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer10(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(1, 0, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer11(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(-1, math.MaxInt, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer12(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(0, 1, -1) })
}

func TestBitwisePanicsByUtGoFuzzer13(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(0, math.MinInt, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer14(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(-1, -1, -1) })
}

func TestBitwisePanicsByUtGoFuzzer15(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MaxInt, math.MinInt, -1) })
}

func TestBitwisePanicsByUtGoFuzzer16(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MaxInt, math.MaxInt, -1) })
}

func TestBitwisePanicsByUtGoFuzzer17(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MinInt, 0, -1) })
}

func TestBitwisePanicsByUtGoFuzzer18(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(0, math.MaxInt, -1) })
}

func TestBitwisePanicsByUtGoFuzzer19(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(1, 1, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer20(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MinInt, -1, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer21(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MinInt, 1, -1) })
}

func TestBitwisePanicsByUtGoFuzzer22(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(1, 0, -1) })
}

func TestBitwisePanicsByUtGoFuzzer23(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(1, -1, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer24(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MinInt, math.MinInt, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer25(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(-1, 0, -1) })
}

func TestBitwisePanicsByUtGoFuzzer26(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MaxInt, 1, -1) })
}

func TestBitwisePanicsByUtGoFuzzer27(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(0, 1, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer28(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(-1, -1, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer29(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MaxInt, math.MinInt, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer30(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(-1, math.MinInt, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer31(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(0, 0, -1) })
}

func TestBitwisePanicsByUtGoFuzzer32(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(0, -1, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer33(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(-1, 1, -1) })
}

func TestBitwisePanicsByUtGoFuzzer34(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MaxInt, -1, -1) })
}

func TestBitwisePanicsByUtGoFuzzer35(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(-1, math.MinInt, -1) })
}

func TestBitwisePanicsByUtGoFuzzer36(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(0, -1, -1) })
}

func TestBitwisePanicsByUtGoFuzzer37(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MinInt, 0, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer38(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(0, math.MaxInt, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer39(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MinInt, -1, -1) })
}

func TestBitwisePanicsByUtGoFuzzer40(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(1, 1, -1) })
}

func TestBitwisePanicsByUtGoFuzzer41(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(1, math.MinInt, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer42(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MinInt, math.MaxInt, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer43(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(1, math.MaxInt, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer44(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MaxInt, 0, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer45(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(1, -1, -1) })
}

func TestBitwisePanicsByUtGoFuzzer46(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MinInt, math.MinInt, -1) })
}

func TestBitwisePanicsByUtGoFuzzer47(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(-1, 0, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer48(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MaxInt, 1, math.MinInt) })
}

func TestBitwisePanicsByUtGoFuzzer49(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(1, math.MinInt, -1) })
}

func TestBitwisePanicsByUtGoFuzzer50(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: negative shift amount", func() { Bitwise(math.MinInt, math.MaxInt, -1) })
}

func TestFormulaByUtGoFuzzer1(t *testing.T) {
	actualVal := Formula(1)

	assert.Equal(t, uint(1), actualVal)
}

func TestFormulaByUtGoFuzzer2(t *testing.T) {
	actualVal := Formula(math.MaxUint)

	assert.Equal(t, uint(9223372036854775808), actualVal)
}

func TestFormulaByUtGoFuzzer3(t *testing.T) {
	actualVal := Formula(0)

	assert.Equal(t, uint(0), actualVal)
}

func TestExtendedByUtGoFuzzer1(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(-1, 1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-1), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer2(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(math.MinInt64, 1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(1), actualVal0)
	assertMultiple.Equal(int64(0), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestExtendedByUtGoFuzzer3(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(0, 1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(1), actualVal0)
	assertMultiple.Equal(int64(0), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestExtendedByUtGoFuzzer4(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(1, 1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(1), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer5(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(math.MaxInt64, 1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(1), actualVal0)
	assertMultiple.Equal(int64(0), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestExtendedByUtGoFuzzer6(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(-1, math.MaxInt64)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-1), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer7(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(math.MinInt64, math.MaxInt64)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-1), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestExtendedByUtGoFuzzer8(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(0, math.MaxInt64)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(9223372036854775807), actualVal0)
	assertMultiple.Equal(int64(0), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestExtendedByUtGoFuzzer9(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(1, math.MaxInt64)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(1), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer10(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(math.MaxInt64, math.MaxInt64)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(9223372036854775807), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer11(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(-1, math.MinInt64)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-1), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer12(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(math.MinInt64, math.MinInt64)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-9223372036854775808), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer13(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(0, math.MinInt64)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-9223372036854775808), actualVal0)
	assertMultiple.Equal(int64(0), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestExtendedByUtGoFuzzer14(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(1, math.MinInt64)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(1), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer15(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(math.MaxInt64, math.MinInt64)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-1), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestExtendedByUtGoFuzzer16(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(-1, 0)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-1), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer17(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(math.MinInt64, 0)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-9223372036854775808), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer18(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(0, 0)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(0), actualVal0)
	assertMultiple.Equal(int64(0), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestExtendedByUtGoFuzzer19(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(1, 0)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(1), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer20(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(math.MaxInt64, 0)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(9223372036854775807), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer21(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(-1, -1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-1), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer22(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(math.MinInt64, -1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-1), actualVal0)
	assertMultiple.Equal(int64(0), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestExtendedByUtGoFuzzer23(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(0, -1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-1), actualVal0)
	assertMultiple.Equal(int64(0), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestExtendedByUtGoFuzzer24(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(1, -1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(1), actualVal0)
	assertMultiple.Equal(int64(1), actualVal1)
	assertMultiple.Equal(int64(0), actualVal2)
}

func TestExtendedByUtGoFuzzer25(t *testing.T) {
	actualVal0, actualVal1, actualVal2 := Extended(math.MaxInt64, -1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(int64(-1), actualVal0)
	assertMultiple.Equal(int64(0), actualVal1)
	assertMultiple.Equal(int64(1), actualVal2)
}

func TestIterativePowerByUtGoFuzzer1(t *testing.T) {
	actualVal := IterativePower(1, math.MaxUint)

	assert.Equal(t, uint(1), actualVal)
}

func TestIterativePowerByUtGoFuzzer2(t *testing.T) {
	actualVal := IterativePower(math.MaxUint, math.MaxUint)

	assert.Equal(t, uint(18446744073709551615), actualVal)
}

func TestIterativePowerByUtGoFuzzer3(t *testing.T) {
	actualVal := IterativePower(0, math.MaxUint)

	assert.Equal(t, uint(0), actualVal)
}

func TestIterativePowerByUtGoFuzzer4(t *testing.T) {
	actualVal := IterativePower(1, 1)

	assert.Equal(t, uint(1), actualVal)
}

func TestIterativePowerByUtGoFuzzer5(t *testing.T) {
	actualVal := IterativePower(math.MaxUint, 1)

	assert.Equal(t, uint(18446744073709551615), actualVal)
}

func TestIterativePowerByUtGoFuzzer6(t *testing.T) {
	actualVal := IterativePower(0, 1)

	assert.Equal(t, uint(0), actualVal)
}

func TestIterativePowerByUtGoFuzzer7(t *testing.T) {
	actualVal := IterativePower(1, 0)

	assert.Equal(t, uint(1), actualVal)
}

func TestIterativePowerByUtGoFuzzer8(t *testing.T) {
	actualVal := IterativePower(math.MaxUint, 0)

	assert.Equal(t, uint(1), actualVal)
}

func TestIterativePowerByUtGoFuzzer9(t *testing.T) {
	actualVal := IterativePower(0, 0)

	assert.Equal(t, uint(1), actualVal)
}

func TestIsPowOfTwoUseLogByUtGoFuzzer1(t *testing.T) {
	actualVal := IsPowOfTwoUseLog(-1.1)

	assert.Equal(t, false, actualVal)
}

func TestIsPowOfTwoUseLogByUtGoFuzzer2(t *testing.T) {
	actualVal := IsPowOfTwoUseLog(math.Inf(1))

	assert.Equal(t, true, actualVal)
}

func TestIsPowOfTwoUseLogByUtGoFuzzer3(t *testing.T) {
	actualVal := IsPowOfTwoUseLog(math.SmallestNonzeroFloat64)

	assert.Equal(t, true, actualVal)
}

func TestIsPowOfTwoUseLogByUtGoFuzzer4(t *testing.T) {
	actualVal := IsPowOfTwoUseLog(math.NaN())

	assert.Equal(t, false, actualVal)
}

func TestIsPowOfTwoUseLogByUtGoFuzzer5(t *testing.T) {
	actualVal := IsPowOfTwoUseLog(0.0)

	assert.Equal(t, false, actualVal)
}

func TestIsPowOfTwoUseLogByUtGoFuzzer6(t *testing.T) {
	actualVal := IsPowOfTwoUseLog(math.MaxFloat64)

	assert.Equal(t, true, actualVal)
}

func TestIsPowOfTwoUseLogByUtGoFuzzer7(t *testing.T) {
	actualVal := IsPowOfTwoUseLog(1.1)

	assert.Equal(t, false, actualVal)
}

func TestIsPowOfTwoUseLogByUtGoFuzzer8(t *testing.T) {
	actualVal := IsPowOfTwoUseLog(math.Inf(-1))

	assert.Equal(t, false, actualVal)
}

func TestReverseByUtGoFuzzer1(t *testing.T) {
	actualVal := Reverse("   ")

	assert.Equal(t, "   ", actualVal)
}

func TestReverseByUtGoFuzzer2(t *testing.T) {
	actualVal := Reverse("\n\t\r")

	assert.Equal(t, "\r\t\n", actualVal)
}

func TestReverseByUtGoFuzzer3(t *testing.T) {
	actualVal := Reverse("")

	assert.Equal(t, "", actualVal)
}

func TestReverseByUtGoFuzzer4(t *testing.T) {
	actualVal := Reverse("string")

	assert.Equal(t, "gnirts", actualVal)
}

func TestParenthesisByUtGoFuzzer1(t *testing.T) {
	actualVal := Parenthesis("   ")

	assert.Equal(t, true, actualVal)
}

func TestParenthesisByUtGoFuzzer2(t *testing.T) {
	actualVal := Parenthesis("\n\t\r")

	assert.Equal(t, true, actualVal)
}

func TestParenthesisByUtGoFuzzer3(t *testing.T) {
	actualVal := Parenthesis("")

	assert.Equal(t, true, actualVal)
}

func TestParenthesisByUtGoFuzzer4(t *testing.T) {
	actualVal := Parenthesis("string")

	assert.Equal(t, true, actualVal)
}

func TestIsPalindromeByUtGoFuzzer1(t *testing.T) {
	actualVal := IsPalindrome("   ")

	assert.Equal(t, true, actualVal)
}

func TestIsPalindromeByUtGoFuzzer2(t *testing.T) {
	actualVal := IsPalindrome("\n\t\r")

	assert.Equal(t, true, actualVal)
}

func TestIsPalindromeByUtGoFuzzer3(t *testing.T) {
	actualVal := IsPalindrome("")

	assert.Equal(t, true, actualVal)
}

func TestIsPalindromeByUtGoFuzzer4(t *testing.T) {
	actualVal := IsPalindrome("string")

	assert.Equal(t, false, actualVal)
}

func TestCleanStringByUtGoFuzzer1(t *testing.T) {
	actualVal := cleanString("   ")

	assert.Equal(t, "", actualVal)
}

func TestCleanStringByUtGoFuzzer2(t *testing.T) {
	actualVal := cleanString("\n\t\r")

	assert.Equal(t, "", actualVal)
}

func TestCleanStringByUtGoFuzzer3(t *testing.T) {
	actualVal := cleanString("")

	assert.Equal(t, "", actualVal)
}

func TestCleanStringByUtGoFuzzer4(t *testing.T) {
	actualVal := cleanString("string")

	assert.Equal(t, "string", actualVal)
}

func TestDistanceByUtGoFuzzer1(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer2(t *testing.T) {
	actualVal := Distance("", "", 1, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer3(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer4(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, 0, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer5(t *testing.T) {
	actualVal := Distance("string", "   ", 1, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer6(t *testing.T) {
	actualVal := Distance("string", "", 1, 0, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer7(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer8(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer9(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer10(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer11(t *testing.T) {
	actualVal := Distance("   ", "string", 0, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer12(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer13(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer14(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, math.MinInt, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer15(t *testing.T) {
	actualVal := Distance("", "string", 0, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer16(t *testing.T) {
	actualVal := Distance("string", "string", 0, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer17(t *testing.T) {
	actualVal := Distance("string", "string", 1, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer18(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, 1, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer19(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer20(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer21(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer22(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer23(t *testing.T) {
	actualVal := Distance("", "   ", 1, math.MaxInt, -1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer24(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer25(t *testing.T) {
	actualVal := Distance("", "string", -1, 1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer26(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer27(t *testing.T) {
	actualVal := Distance("   ", "", 1, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer28(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, 0, -1)

	assert.Equal(t, -7, actualVal)
}

func TestDistanceByUtGoFuzzer29(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer30(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer31(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer32(t *testing.T) {
	actualVal := Distance("   ", "string", -1, 0, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer33(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, math.MinInt, -1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer34(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer35(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer36(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer37(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer38(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer39(t *testing.T) {
	actualVal := Distance("string", "", -1, math.MaxInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer40(t *testing.T) {
	actualVal := Distance("", "   ", 0, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer41(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer42(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, 1, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer43(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer44(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, -1, -1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer45(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer46(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, math.MinInt, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer47(t *testing.T) {
	actualVal := Distance("", "", 1, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer48(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer49(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, 1, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer50(t *testing.T) {
	actualVal := Distance("string", "   ", 1, math.MinInt, 0)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer51(t *testing.T) {
	actualVal := Distance("string", "", 1, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer52(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer53(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer54(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, 0, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer55(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer56(t *testing.T) {
	actualVal := Distance("   ", "string", 0, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer57(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer58(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, 1, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer59(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer60(t *testing.T) {
	actualVal := Distance("", "string", 0, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer61(t *testing.T) {
	actualVal := Distance("string", "string", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer62(t *testing.T) {
	actualVal := Distance("string", "string", -1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer63(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer64(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer65(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer66(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer67(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer68(t *testing.T) {
	actualVal := Distance("", "   ", -1, 0, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer69(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer70(t *testing.T) {
	actualVal := Distance("", "string", -1, -1, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer71(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, math.MaxInt, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer72(t *testing.T) {
	actualVal := Distance("   ", "", 1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer73(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, 1, 0)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer74(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer75(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer76(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer77(t *testing.T) {
	actualVal := Distance("   ", "string", -1, 1, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer78(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, math.MaxInt, 0)

	assert.Equal(t, -9223372036854775804, actualVal)
}

func TestDistanceByUtGoFuzzer79(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, -1, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer80(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer81(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, 0, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer82(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer83(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer84(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer85(t *testing.T) {
	actualVal := Distance("", "   ", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer86(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer87(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer88(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer89(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer90(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer91(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, -1, math.MaxInt)

	assert.Equal(t, -5, actualVal)
}

func TestDistanceByUtGoFuzzer92(t *testing.T) {
	actualVal := Distance("", "", 1, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer93(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer94(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer95(t *testing.T) {
	actualVal := Distance("string", "   ", 1, -1, math.MaxInt)

	assert.Equal(t, -5, actualVal)
}

func TestDistanceByUtGoFuzzer96(t *testing.T) {
	actualVal := Distance("string", "", 1, 0, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer97(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer98(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer99(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer100(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer101(t *testing.T) {
	actualVal := Distance("   ", "string", 0, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer102(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer103(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer104(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer105(t *testing.T) {
	actualVal := Distance("", "string", 0, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer106(t *testing.T) {
	actualVal := Distance("string", "string", 0, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer107(t *testing.T) {
	actualVal := Distance("string", "string", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer108(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, 1, math.MaxInt)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer109(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, -1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer110(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer111(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer112(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer113(t *testing.T) {
	actualVal := Distance("", "   ", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer114(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer115(t *testing.T) {
	actualVal := Distance("", "string", -1, 1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer116(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer117(t *testing.T) {
	actualVal := Distance("   ", "", 1, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer118(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer119(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer120(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -4, actualVal)
}

func TestDistanceByUtGoFuzzer121(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer122(t *testing.T) {
	actualVal := Distance("   ", "string", -1, 0, math.MaxInt)

	assert.Equal(t, -7, actualVal)
}

func TestDistanceByUtGoFuzzer123(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer124(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer125(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer126(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer127(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer128(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer129(t *testing.T) {
	actualVal := Distance("string", "", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer130(t *testing.T) {
	actualVal := Distance("", "   ", 0, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer131(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer132(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer133(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer134(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer135(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer136(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, 1, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer137(t *testing.T) {
	actualVal := Distance("", "", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer138(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer139(t *testing.T) {
	actualVal := Distance("", "string", -1, math.MaxInt, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer140(t *testing.T) {
	actualVal := Distance("string", "   ", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer141(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer142(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer143(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer144(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer145(t *testing.T) {
	actualVal := Distance("string", "string", -1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer146(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer147(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, 1, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer148(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, math.MaxInt, 0)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer149(t *testing.T) {
	actualVal := Distance("", "   ", -1, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer150(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, 1, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer151(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer152(t *testing.T) {
	actualVal := Distance("string", "string", 0, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer153(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer154(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer155(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer156(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer157(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer158(t *testing.T) {
	actualVal := Distance("", "   ", 0, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer159(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer160(t *testing.T) {
	actualVal := Distance("", "string", 1, 0, 0)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer161(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, -1, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer162(t *testing.T) {
	actualVal := Distance("   ", "", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer163(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, math.MaxInt, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer164(t *testing.T) {
	actualVal := Distance("   ", "", 1, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer165(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer166(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer167(t *testing.T) {
	actualVal := Distance("   ", "string", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer168(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer169(t *testing.T) {
	actualVal := Distance("string", "   ", -1, 0, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer170(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, -1, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer171(t *testing.T) {
	actualVal := Distance("   ", "string", -1, math.MinInt, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer172(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer173(t *testing.T) {
	actualVal := Distance("", "", -1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer174(t *testing.T) {
	actualVal := Distance("string", "", 1, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer175(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer176(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, 0, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer177(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, 0, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer178(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer179(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, 1, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer180(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer181(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, 1, math.MinInt)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer182(t *testing.T) {
	actualVal := Distance("", "", 0, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer183(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer184(t *testing.T) {
	actualVal := Distance("", "string", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer185(t *testing.T) {
	actualVal := Distance("string", "   ", 0, 1, math.MinInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer186(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer187(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer188(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer189(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer190(t *testing.T) {
	actualVal := Distance("string", "string", -1, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer191(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer192(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer193(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer194(t *testing.T) {
	actualVal := Distance("", "   ", -1, -1, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer195(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer196(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer197(t *testing.T) {
	actualVal := Distance("string", "string", 0, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer198(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer199(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer200(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer201(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer202(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer203(t *testing.T) {
	actualVal := Distance("", "   ", 0, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer204(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer205(t *testing.T) {
	actualVal := Distance("", "string", 1, 0, math.MinInt)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer206(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, -1, math.MinInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer207(t *testing.T) {
	actualVal := Distance("   ", "", 0, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer208(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer209(t *testing.T) {
	actualVal := Distance("   ", "", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer210(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer211(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer212(t *testing.T) {
	actualVal := Distance("   ", "string", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer213(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer214(t *testing.T) {
	actualVal := Distance("string", "   ", -1, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer215(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer216(t *testing.T) {
	actualVal := Distance("   ", "string", -1, math.MinInt, math.MinInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer217(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer218(t *testing.T) {
	actualVal := Distance("", "", -1, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer219(t *testing.T) {
	actualVal := Distance("string", "", 1, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer220(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer221(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer222(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer223(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer224(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer225(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer226(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer227(t *testing.T) {
	actualVal := Distance("", "", 0, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer228(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer229(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer230(t *testing.T) {
	actualVal := Distance("string", "   ", 0, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer231(t *testing.T) {
	actualVal := Distance("string", "", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer232(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer233(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer234(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer235(t *testing.T) {
	actualVal := Distance("string", "string", -1, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer236(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer237(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, -1, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer238(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, 0, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer239(t *testing.T) {
	actualVal := Distance("", "   ", -1, math.MinInt, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer240(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, -1, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer241(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, 0, 0)

	assert.Equal(t, -2, actualVal)
}

func TestDistanceByUtGoFuzzer242(t *testing.T) {
	actualVal := Distance("string", "string", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer243(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, 1, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer244(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer245(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer246(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer247(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer248(t *testing.T) {
	actualVal := Distance("", "   ", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer249(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer250(t *testing.T) {
	actualVal := Distance("", "string", 1, 1, 0)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer251(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, math.MinInt, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer252(t *testing.T) {
	actualVal := Distance("   ", "", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer253(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, 0, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer254(t *testing.T) {
	actualVal := Distance("   ", "", -1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer255(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer256(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer257(t *testing.T) {
	actualVal := Distance("   ", "string", 1, 0, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer258(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer259(t *testing.T) {
	actualVal := Distance("string", "   ", -1, 1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer260(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer261(t *testing.T) {
	actualVal := Distance("   ", "string", -1, math.MaxInt, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer262(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer263(t *testing.T) {
	actualVal := Distance("", "", -1, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer264(t *testing.T) {
	actualVal := Distance("string", "", 1, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer265(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer266(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, 1, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer267(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, 1, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer268(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer269(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, -1, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer270(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer271(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer272(t *testing.T) {
	actualVal := Distance("", "", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer273(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer274(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer275(t *testing.T) {
	actualVal := Distance("string", "   ", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer276(t *testing.T) {
	actualVal := Distance("string", "", 0, -1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer277(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer278(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer279(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer280(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer281(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer282(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer283(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, -1, math.MaxInt)

	assert.Equal(t, -5, actualVal)
}

func TestDistanceByUtGoFuzzer284(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer285(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer286(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer287(t *testing.T) {
	actualVal := Distance("string", "string", 1, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer288(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer289(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer290(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer291(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer292(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer293(t *testing.T) {
	actualVal := Distance("", "   ", 1, 1, math.MaxInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer294(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer295(t *testing.T) {
	actualVal := Distance("", "string", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer296(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, 0, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer297(t *testing.T) {
	actualVal := Distance("   ", "", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer298(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer299(t *testing.T) {
	actualVal := Distance("   ", "", -1, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer300(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer301(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer302(t *testing.T) {
	actualVal := Distance("   ", "string", 1, -1, math.MaxInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer303(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, 0, math.MaxInt)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer304(t *testing.T) {
	actualVal := Distance("string", "   ", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer305(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, 0, math.MaxInt)

	assert.Equal(t, -2, actualVal)
}

func TestDistanceByUtGoFuzzer306(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer307(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer308(t *testing.T) {
	actualVal := Distance("", "", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer309(t *testing.T) {
	actualVal := Distance("string", "", -1, 1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer310(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer311(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -7, actualVal)
}

func TestDistanceByUtGoFuzzer312(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer313(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer314(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer315(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer316(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, 0, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer317(t *testing.T) {
	actualVal := Distance("", "", 1, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer318(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer319(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer320(t *testing.T) {
	actualVal := Distance("string", "   ", 1, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer321(t *testing.T) {
	actualVal := Distance("string", "", 0, math.MinInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer322(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer323(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer324(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer325(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer326(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer327(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, 0, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer328(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, math.MinInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer329(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, 1, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer330(t *testing.T) {
	actualVal := Distance("", "string", 0, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer331(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer332(t *testing.T) {
	actualVal := Distance("string", "string", 1, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer333(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer334(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, 0, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer335(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, 1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer336(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer337(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer338(t *testing.T) {
	actualVal := Distance("", "   ", 1, -1, -1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer339(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer340(t *testing.T) {
	actualVal := Distance("", "string", 1, math.MaxInt, -1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer341(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer342(t *testing.T) {
	actualVal := Distance("   ", "", 0, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer343(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, math.MinInt, -1)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer344(t *testing.T) {
	actualVal := Distance("   ", "", -1, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer345(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, -1, -1)

	assert.Equal(t, -5, actualVal)
}

func TestDistanceByUtGoFuzzer346(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer347(t *testing.T) {
	actualVal := Distance("   ", "string", 1, math.MinInt, -1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer348(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, 1, -1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer349(t *testing.T) {
	actualVal := Distance("string", "   ", -1, math.MaxInt, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer350(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer351(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer352(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer353(t *testing.T) {
	actualVal := Distance("", "", -1, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer354(t *testing.T) {
	actualVal := Distance("string", "", -1, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer355(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer356(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer357(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer358(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer359(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, 0, -1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer360(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer361(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer362(t *testing.T) {
	actualVal := Distance("", "", 1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer363(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer364(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, 0, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer365(t *testing.T) {
	actualVal := Distance("string", "   ", 1, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer366(t *testing.T) {
	actualVal := Distance("string", "", 1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer367(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer368(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer369(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer370(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer371(t *testing.T) {
	actualVal := Distance("   ", "string", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer372(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer373(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, 0, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer374(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer375(t *testing.T) {
	actualVal := Distance("", "string", 0, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer376(t *testing.T) {
	actualVal := Distance("string", "string", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer377(t *testing.T) {
	actualVal := Distance("string", "string", 1, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer378(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, 1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer379(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer380(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer381(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer382(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer383(t *testing.T) {
	actualVal := Distance("", "   ", 1, math.MaxInt, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer384(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer385(t *testing.T) {
	actualVal := Distance("", "string", -1, 1, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer386(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, math.MinInt, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer387(t *testing.T) {
	actualVal := Distance("   ", "", 1, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer388(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, 0, 0)

	assert.Equal(t, -2, actualVal)
}

func TestDistanceByUtGoFuzzer389(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer390(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer391(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer392(t *testing.T) {
	actualVal := Distance("   ", "string", -1, 0, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer393(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, math.MinInt, 0)

	assert.Equal(t, -9223372036854775803, actualVal)
}

func TestDistanceByUtGoFuzzer394(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, 1, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer395(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer396(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer397(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, 1, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer398(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer399(t *testing.T) {
	actualVal := Distance("string", "", -1, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer400(t *testing.T) {
	actualVal := Distance("", "   ", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer401(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer402(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, 1, 0)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer403(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer404(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, -1, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer405(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer406(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer407(t *testing.T) {
	actualVal := Distance("", "", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer408(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer409(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer410(t *testing.T) {
	actualVal := Distance("string", "   ", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer411(t *testing.T) {
	actualVal := Distance("string", "", 1, 1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer412(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer413(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer414(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, 0, math.MaxInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer415(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer416(t *testing.T) {
	actualVal := Distance("   ", "string", 0, -1, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer417(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer418(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer419(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer420(t *testing.T) {
	actualVal := Distance("", "string", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer421(t *testing.T) {
	actualVal := Distance("string", "string", 0, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer422(t *testing.T) {
	actualVal := Distance("string", "string", -1, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer423(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, -1, math.MaxInt)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer424(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer425(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer426(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer427(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer428(t *testing.T) {
	actualVal := Distance("", "   ", -1, 0, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer429(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer430(t *testing.T) {
	actualVal := Distance("", "string", -1, -1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer431(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer432(t *testing.T) {
	actualVal := Distance("   ", "", 1, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer433(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer434(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer435(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, 0, math.MaxInt)

	assert.Equal(t, -2, actualVal)
}

func TestDistanceByUtGoFuzzer436(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer437(t *testing.T) {
	actualVal := Distance("   ", "string", -1, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer438(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer439(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer440(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer441(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer442(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, -1, math.MaxInt)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer443(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer444(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer445(t *testing.T) {
	actualVal := Distance("", "   ", 0, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer446(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer447(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer448(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer449(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer450(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer451(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, 0, math.MaxInt)

	assert.Equal(t, -4, actualVal)
}

func TestDistanceByUtGoFuzzer452(t *testing.T) {
	actualVal := Distance("", "", 1, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer453(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer454(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer455(t *testing.T) {
	actualVal := Distance("string", "   ", 1, 0, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer456(t *testing.T) {
	actualVal := Distance("string", "", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer457(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer458(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer459(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer460(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer461(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer462(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer463(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer464(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer465(t *testing.T) {
	actualVal := Distance("", "string", 0, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer466(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -7, actualVal)
}

func TestDistanceByUtGoFuzzer467(t *testing.T) {
	actualVal := Distance("string", "string", 1, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer468(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer469(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, 0, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer470(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer471(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer472(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer473(t *testing.T) {
	actualVal := Distance("", "   ", 1, -1, math.MaxInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer474(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer475(t *testing.T) {
	actualVal := Distance("", "string", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer476(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, 1, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer477(t *testing.T) {
	actualVal := Distance("   ", "", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer478(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -5, actualVal)
}

func TestDistanceByUtGoFuzzer479(t *testing.T) {
	actualVal := Distance("   ", "", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer480(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer481(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer482(t *testing.T) {
	actualVal := Distance("   ", "string", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer483(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775803, actualVal)
}

func TestDistanceByUtGoFuzzer484(t *testing.T) {
	actualVal := Distance("string", "   ", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer485(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer486(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer487(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer488(t *testing.T) {
	actualVal := Distance("", "", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer489(t *testing.T) {
	actualVal := Distance("string", "", -1, -1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer490(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer491(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -8, actualVal)
}

func TestDistanceByUtGoFuzzer492(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer493(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer494(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer495(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer496(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, math.MinInt, 1)

	assert.Equal(t, -9223372036854775804, actualVal)
}

func TestDistanceByUtGoFuzzer497(t *testing.T) {
	actualVal := Distance("", "", 0, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer498(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer499(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer500(t *testing.T) {
	actualVal := Distance("string", "   ", 0, math.MinInt, 1)

	assert.Equal(t, -9223372036854775803, actualVal)
}

func TestDistanceByUtGoFuzzer501(t *testing.T) {
	actualVal := Distance("string", "", 0, 1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer502(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer503(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, math.MaxInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer504(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer505(t *testing.T) {
	actualVal := Distance("string", "string", -1, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer506(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer507(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, math.MinInt, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer508(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer509(t *testing.T) {
	actualVal := Distance("", "   ", -1, math.MaxInt, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer510(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer511(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, 1, 1)

	assert.Equal(t, -9223372036854775803, actualVal)
}

func TestDistanceByUtGoFuzzer512(t *testing.T) {
	actualVal := Distance("string", "string", 1, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer513(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer514(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer515(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer516(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer517(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, math.MinInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer518(t *testing.T) {
	actualVal := Distance("", "   ", 1, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer519(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer520(t *testing.T) {
	actualVal := Distance("", "string", 1, -1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer521(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, math.MaxInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer522(t *testing.T) {
	actualVal := Distance("   ", "", 0, -1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer523(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, 1, 1)

	assert.Equal(t, -9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer524(t *testing.T) {
	actualVal := Distance("   ", "", -1, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer525(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, 0, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer526(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer527(t *testing.T) {
	actualVal := Distance("   ", "string", 1, 1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer528(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer529(t *testing.T) {
	actualVal := Distance("string", "   ", -1, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer530(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer531(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, 0, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer532(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, -1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer533(t *testing.T) {
	actualVal := Distance("", "", -1, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer534(t *testing.T) {
	actualVal := Distance("string", "", -1, 0, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer535(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer536(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, -1, 1)

	assert.Equal(t, -5, actualVal)
}

func TestDistanceByUtGoFuzzer537(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, -1, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer538(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer539(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, math.MinInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer540(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer541(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, 1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer542(t *testing.T) {
	actualVal := Distance("", "", 1, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer543(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer544(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer545(t *testing.T) {
	actualVal := Distance("string", "   ", 1, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer546(t *testing.T) {
	actualVal := Distance("string", "", 0, math.MaxInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer547(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer548(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer549(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer550(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer551(t *testing.T) {
	actualVal := Distance("   ", "string", 0, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer552(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, 1, -1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer553(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, math.MaxInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer554(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, -1, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer555(t *testing.T) {
	actualVal := Distance("", "string", 0, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer556(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer557(t *testing.T) {
	actualVal := Distance("string", "string", 1, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer558(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, 0, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer559(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, 1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer560(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer561(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer562(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer563(t *testing.T) {
	actualVal := Distance("", "   ", 1, math.MinInt, -1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer564(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer565(t *testing.T) {
	actualVal := Distance("", "string", -1, 0, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer566(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer567(t *testing.T) {
	actualVal := Distance("   ", "", 1, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer568(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer569(t *testing.T) {
	actualVal := Distance("   ", "", -1, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer570(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer571(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer572(t *testing.T) {
	actualVal := Distance("   ", "string", 1, math.MaxInt, -1)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer573(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer574(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer575(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer576(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer577(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer578(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer579(t *testing.T) {
	actualVal := Distance("string", "", -1, math.MinInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer580(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer581(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer582(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, 0, -1)

	assert.Equal(t, -4, actualVal)
}

func TestDistanceByUtGoFuzzer583(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer584(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, 1, -1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer585(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer586(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, math.MinInt, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer587(t *testing.T) {
	actualVal := Distance("", "", 0, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer588(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer589(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer590(t *testing.T) {
	actualVal := Distance("string", "   ", 0, math.MinInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer591(t *testing.T) {
	actualVal := Distance("string", "", 0, 1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer592(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer593(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer594(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer595(t *testing.T) {
	actualVal := Distance("string", "string", -1, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer596(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer597(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, math.MinInt, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer598(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, 1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer599(t *testing.T) {
	actualVal := Distance("", "   ", -1, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer600(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer601(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer602(t *testing.T) {
	actualVal := Distance("string", "string", 1, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer603(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer604(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer605(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer606(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer607(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer608(t *testing.T) {
	actualVal := Distance("", "   ", 1, 0, -1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer609(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer610(t *testing.T) {
	actualVal := Distance("", "string", 1, -1, -1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer611(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, math.MaxInt, -1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer612(t *testing.T) {
	actualVal := Distance("   ", "", 0, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer613(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer614(t *testing.T) {
	actualVal := Distance("   ", "", -1, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer615(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, 0, -1)

	assert.Equal(t, -4, actualVal)
}

func TestDistanceByUtGoFuzzer616(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer617(t *testing.T) {
	actualVal := Distance("   ", "string", 1, 1, -1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer618(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer619(t *testing.T) {
	actualVal := Distance("string", "   ", -1, -1, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer620(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer621(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, 0, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer622(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer623(t *testing.T) {
	actualVal := Distance("", "", -1, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer624(t *testing.T) {
	actualVal := Distance("string", "", -1, 0, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer625(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer626(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, -1, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer627(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer628(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer629(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, math.MinInt, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer630(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer631(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, math.MinInt, 0)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer632(t *testing.T) {
	actualVal := Distance("", "", 0, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer633(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer634(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer635(t *testing.T) {
	actualVal := Distance("string", "   ", 0, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer636(t *testing.T) {
	actualVal := Distance("string", "", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer637(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer638(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer639(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer640(t *testing.T) {
	actualVal := Distance("string", "string", -1, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer641(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer642(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, math.MinInt, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer643(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, 1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer644(t *testing.T) {
	actualVal := Distance("", "   ", -1, math.MaxInt, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer645(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer646(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, 1, 0)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer647(t *testing.T) {
	actualVal := Distance("string", "string", 1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer648(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer649(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer650(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer651(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer652(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer653(t *testing.T) {
	actualVal := Distance("", "   ", 1, 0, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer654(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer655(t *testing.T) {
	actualVal := Distance("", "string", 1, -1, 0)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer656(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, math.MaxInt, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer657(t *testing.T) {
	actualVal := Distance("   ", "", 0, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer658(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, 1, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer659(t *testing.T) {
	actualVal := Distance("   ", "", -1, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer660(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, 0, 0)

	assert.Equal(t, -2, actualVal)
}

func TestDistanceByUtGoFuzzer661(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer662(t *testing.T) {
	actualVal := Distance("   ", "string", 1, 1, 0)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer663(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer664(t *testing.T) {
	actualVal := Distance("string", "   ", -1, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer665(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer666(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, 0, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer667(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer668(t *testing.T) {
	actualVal := Distance("", "", -1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer669(t *testing.T) {
	actualVal := Distance("string", "", -1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer670(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer671(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, -1, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer672(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, -1, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer673(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer674(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer675(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer676(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer677(t *testing.T) {
	actualVal := Distance("", "", 0, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer678(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer679(t *testing.T) {
	actualVal := Distance("", "string", -1, math.MinInt, 1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer680(t *testing.T) {
	actualVal := Distance("string", "   ", 0, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer681(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer682(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, -1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer683(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer684(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, -1, 1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer685(t *testing.T) {
	actualVal := Distance("string", "string", -1, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer686(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer687(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, 0, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer688(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, math.MinInt, 1)

	assert.Equal(t, -9223372036854775804, actualVal)
}

func TestDistanceByUtGoFuzzer689(t *testing.T) {
	actualVal := Distance("", "   ", -1, 1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer690(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, 0, 1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer691(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, math.MinInt, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer692(t *testing.T) {
	actualVal := Distance("string", "string", 0, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer693(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775804, actualVal)
}

func TestDistanceByUtGoFuzzer694(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer695(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, 1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer696(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer697(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer698(t *testing.T) {
	actualVal := Distance("", "   ", 0, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer699(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, -1, 1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer700(t *testing.T) {
	actualVal := Distance("", "string", 0, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer701(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer702(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer703(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, math.MinInt, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer704(t *testing.T) {
	actualVal := Distance("   ", "", 1, math.MinInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer705(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, -1, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer706(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer707(t *testing.T) {
	actualVal := Distance("   ", "string", 0, math.MinInt, 1)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer708(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer709(t *testing.T) {
	actualVal := Distance("string", "   ", 1, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer710(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, 1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer711(t *testing.T) {
	actualVal := Distance("   ", "string", -1, -1, 1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer712(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer713(t *testing.T) {
	actualVal := Distance("", "", 1, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer714(t *testing.T) {
	actualVal := Distance("string", "", 1, -1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer715(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer716(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer717(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer718(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer719(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, 0, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer720(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer721(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, 0, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer722(t *testing.T) {
	actualVal := Distance("", "", 0, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer723(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer724(t *testing.T) {
	actualVal := Distance("", "string", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer725(t *testing.T) {
	actualVal := Distance("string", "   ", 0, 0, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer726(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer727(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer728(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer729(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer730(t *testing.T) {
	actualVal := Distance("string", "string", -1, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer731(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer732(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, 0, math.MaxInt)

	assert.Equal(t, -7, actualVal)
}

func TestDistanceByUtGoFuzzer733(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer734(t *testing.T) {
	actualVal := Distance("", "   ", -1, 1, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer735(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer736(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -5, actualVal)
}

func TestDistanceByUtGoFuzzer737(t *testing.T) {
	actualVal := Distance("string", "string", 0, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer738(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer739(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer740(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer741(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer742(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer743(t *testing.T) {
	actualVal := Distance("", "   ", 0, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer744(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer745(t *testing.T) {
	actualVal := Distance("", "string", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer746(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, 1, math.MaxInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer747(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer748(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer749(t *testing.T) {
	actualVal := Distance("   ", "", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer750(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer751(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer752(t *testing.T) {
	actualVal := Distance("   ", "string", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer753(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer754(t *testing.T) {
	actualVal := Distance("string", "   ", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer755(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer756(t *testing.T) {
	actualVal := Distance("   ", "string", -1, -1, math.MaxInt)

	assert.Equal(t, -8, actualVal)
}

func TestDistanceByUtGoFuzzer757(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer758(t *testing.T) {
	actualVal := Distance("", "", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer759(t *testing.T) {
	actualVal := Distance("string", "", 1, -1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer760(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer761(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer762(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer763(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer764(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer765(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer766(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer767(t *testing.T) {
	actualVal := Distance("", "", 0, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer768(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer769(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer770(t *testing.T) {
	actualVal := Distance("string", "   ", 0, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer771(t *testing.T) {
	actualVal := Distance("string", "", 0, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer772(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer773(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer774(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer775(t *testing.T) {
	actualVal := Distance("string", "string", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer776(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer777(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, math.MinInt, math.MinInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer778(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, 1, math.MinInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer779(t *testing.T) {
	actualVal := Distance("", "   ", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer780(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer781(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer782(t *testing.T) {
	actualVal := Distance("string", "string", 1, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer783(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, -1, math.MinInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer784(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer785(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer786(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer787(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer788(t *testing.T) {
	actualVal := Distance("", "   ", 1, 0, math.MinInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer789(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer790(t *testing.T) {
	actualVal := Distance("", "string", 1, -1, math.MinInt)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer791(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer792(t *testing.T) {
	actualVal := Distance("   ", "", 0, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer793(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer794(t *testing.T) {
	actualVal := Distance("   ", "", -1, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer795(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer796(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer797(t *testing.T) {
	actualVal := Distance("   ", "string", 1, 1, math.MinInt)

	assert.Equal(t, -9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer798(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer799(t *testing.T) {
	actualVal := Distance("string", "   ", -1, -1, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer800(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer801(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer802(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer803(t *testing.T) {
	actualVal := Distance("", "", -1, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer804(t *testing.T) {
	actualVal := Distance("string", "", -1, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer805(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer806(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer807(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, -1, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer808(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer809(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer810(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer811(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer812(t *testing.T) {
	actualVal := Distance("", "", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer813(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer814(t *testing.T) {
	actualVal := Distance("", "string", -1, math.MinInt, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer815(t *testing.T) {
	actualVal := Distance("string", "   ", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer816(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer817(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer818(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer819(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, -1, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer820(t *testing.T) {
	actualVal := Distance("string", "string", -1, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer821(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer822(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, 0, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer823(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, math.MinInt, 0)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer824(t *testing.T) {
	actualVal := Distance("", "   ", -1, 1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer825(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, 0, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer826(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer827(t *testing.T) {
	actualVal := Distance("string", "string", 0, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer828(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer829(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer830(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer831(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer832(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer833(t *testing.T) {
	actualVal := Distance("", "   ", 0, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer834(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer835(t *testing.T) {
	actualVal := Distance("", "string", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer836(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, 1, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer837(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer838(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, math.MinInt, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer839(t *testing.T) {
	actualVal := Distance("   ", "", 1, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer840(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, -1, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer841(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer842(t *testing.T) {
	actualVal := Distance("   ", "string", 0, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer843(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer844(t *testing.T) {
	actualVal := Distance("string", "   ", 1, math.MaxInt, 0)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer845(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, 1, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer846(t *testing.T) {
	actualVal := Distance("   ", "string", -1, -1, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer847(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer848(t *testing.T) {
	actualVal := Distance("", "", 1, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer849(t *testing.T) {
	actualVal := Distance("string", "", 1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer850(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer851(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer852(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, math.MaxInt, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer853(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer854(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, 0, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer855(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer856(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, math.MinInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer857(t *testing.T) {
	actualVal := Distance("", "", 1, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer858(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer859(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, 1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer860(t *testing.T) {
	actualVal := Distance("string", "   ", 1, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer861(t *testing.T) {
	actualVal := Distance("string", "", 1, 1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer862(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer863(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer864(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, 0, -1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer865(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer866(t *testing.T) {
	actualVal := Distance("   ", "string", 0, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer867(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer868(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, 1, -1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer869(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer870(t *testing.T) {
	actualVal := Distance("", "string", 0, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer871(t *testing.T) {
	actualVal := Distance("string", "string", 0, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer872(t *testing.T) {
	actualVal := Distance("string", "string", -1, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer873(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, -1, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer874(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, math.MinInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer875(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer876(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer877(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer878(t *testing.T) {
	actualVal := Distance("", "   ", -1, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer879(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer880(t *testing.T) {
	actualVal := Distance("", "string", -1, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer881(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer882(t *testing.T) {
	actualVal := Distance("   ", "", 1, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer883(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer884(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer885(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer886(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer887(t *testing.T) {
	actualVal := Distance("   ", "string", -1, 1, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer888(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, math.MaxInt, -1)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer889(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer890(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer891(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, 0, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer892(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer893(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer894(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, 0, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer895(t *testing.T) {
	actualVal := Distance("", "   ", 0, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer896(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer897(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, -1, -1)

	assert.Equal(t, -5, actualVal)
}

func TestDistanceByUtGoFuzzer898(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer899(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer900(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer901(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer902(t *testing.T) {
	actualVal := Distance("", "", 1, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer903(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer904(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer905(t *testing.T) {
	actualVal := Distance("string", "   ", 1, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer906(t *testing.T) {
	actualVal := Distance("string", "", 0, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer907(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer908(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer909(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer910(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer911(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer912(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer913(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, math.MinInt, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer914(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer915(t *testing.T) {
	actualVal := Distance("", "string", 0, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer916(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer917(t *testing.T) {
	actualVal := Distance("string", "string", 1, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer918(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer919(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer920(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer921(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer922(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer923(t *testing.T) {
	actualVal := Distance("", "   ", 1, -1, math.MinInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer924(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer925(t *testing.T) {
	actualVal := Distance("", "string", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer926(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, 1, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer927(t *testing.T) {
	actualVal := Distance("   ", "", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer928(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer929(t *testing.T) {
	actualVal := Distance("   ", "", -1, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer930(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer931(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer932(t *testing.T) {
	actualVal := Distance("   ", "string", 1, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer933(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, 1, math.MinInt)

	assert.Equal(t, -9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer934(t *testing.T) {
	actualVal := Distance("string", "   ", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer935(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer936(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, -1, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer937(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer938(t *testing.T) {
	actualVal := Distance("", "", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer939(t *testing.T) {
	actualVal := Distance("string", "", -1, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer940(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer941(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer942(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer943(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer944(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer945(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer946(t *testing.T) {
	actualVal := Distance("", "", 1, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer947(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer948(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer949(t *testing.T) {
	actualVal := Distance("string", "   ", 1, 1, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer950(t *testing.T) {
	actualVal := Distance("string", "", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer951(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer952(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer953(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer954(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer955(t *testing.T) {
	actualVal := Distance("   ", "string", 0, 0, math.MaxInt)

	assert.Equal(t, -2, actualVal)
}

func TestDistanceByUtGoFuzzer956(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer957(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer958(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer959(t *testing.T) {
	actualVal := Distance("", "string", 0, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer960(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -8, actualVal)
}

func TestDistanceByUtGoFuzzer961(t *testing.T) {
	actualVal := Distance("string", "string", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer962(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, 0, math.MaxInt)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer963(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, 1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer964(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer965(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer966(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer967(t *testing.T) {
	actualVal := Distance("", "   ", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer968(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer969(t *testing.T) {
	actualVal := Distance("", "string", -1, 0, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer970(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, -1, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer971(t *testing.T) {
	actualVal := Distance("   ", "", 1, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer972(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer973(t *testing.T) {
	actualVal := Distance("   ", "", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer974(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer975(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer976(t *testing.T) {
	actualVal := Distance("   ", "string", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer977(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, -1, math.MaxInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer978(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer979(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, -1, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer980(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer981(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, 0, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer982(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer983(t *testing.T) {
	actualVal := Distance("string", "", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer984(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer985(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer986(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer987(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer988(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer989(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer990(t *testing.T) {
	actualVal := Distance("", "", 1, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer991(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer992(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer993(t *testing.T) {
	actualVal := Distance("string", "   ", 1, 1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer994(t *testing.T) {
	actualVal := Distance("string", "", 0, math.MaxInt, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer995(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer996(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, -1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer997(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer998(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer999(t *testing.T) {
	actualVal := Distance("   ", "string", 0, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1000(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, 1, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1001(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1002(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, -1, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1003(t *testing.T) {
	actualVal := Distance("", "string", 0, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1004(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1005(t *testing.T) {
	actualVal := Distance("string", "string", 1, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1006(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1007(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, 1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1008(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, -1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1009(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1010(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1011(t *testing.T) {
	actualVal := Distance("", "   ", 1, math.MinInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1012(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, math.MinInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1013(t *testing.T) {
	actualVal := Distance("", "string", -1, 0, 1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1014(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, -1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1015(t *testing.T) {
	actualVal := Distance("   ", "", 1, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1016(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1017(t *testing.T) {
	actualVal := Distance("   ", "", -1, math.MaxInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1018(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1019(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1020(t *testing.T) {
	actualVal := Distance("   ", "string", 1, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1021(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1022(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, 0, 1)

	assert.Equal(t, -9223372036854775804, actualVal)
}

func TestDistanceByUtGoFuzzer1023(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, -1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1024(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1025(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1026(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1027(t *testing.T) {
	actualVal := Distance("string", "", -1, math.MinInt, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1028(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1029(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1030(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, 0, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1031(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1032(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, 1, 1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1033(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, -1, math.MinInt)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1034(t *testing.T) {
	actualVal := Distance("", "", 0, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1035(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1036(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1037(t *testing.T) {
	actualVal := Distance("string", "   ", 0, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1038(t *testing.T) {
	actualVal := Distance("string", "", 0, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1039(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1040(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1041(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1042(t *testing.T) {
	actualVal := Distance("string", "string", -1, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1043(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1044(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, -1, math.MinInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1045(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1046(t *testing.T) {
	actualVal := Distance("", "   ", -1, math.MinInt, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1047(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1048(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1049(t *testing.T) {
	actualVal := Distance("string", "string", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1050(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, 1, math.MinInt)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1051(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1052(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1053(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1054(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1055(t *testing.T) {
	actualVal := Distance("", "   ", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1056(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1057(t *testing.T) {
	actualVal := Distance("", "string", 1, 1, math.MinInt)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1058(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, math.MinInt, math.MinInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1059(t *testing.T) {
	actualVal := Distance("   ", "", 0, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1060(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1061(t *testing.T) {
	actualVal := Distance("   ", "", -1, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1062(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1063(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1064(t *testing.T) {
	actualVal := Distance("   ", "string", 1, 0, math.MinInt)

	assert.Equal(t, -9223372036854775804, actualVal)
}

func TestDistanceByUtGoFuzzer1065(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1066(t *testing.T) {
	actualVal := Distance("string", "   ", -1, 1, math.MinInt)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1067(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, -5, actualVal)
}

func TestDistanceByUtGoFuzzer1068(t *testing.T) {
	actualVal := Distance("   ", "string", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1069(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1070(t *testing.T) {
	actualVal := Distance("", "", -1, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1071(t *testing.T) {
	actualVal := Distance("string", "", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1072(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1073(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1074(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1075(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1076(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1077(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, math.MinInt, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1078(t *testing.T) {
	actualVal := Distance("", "", 1, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1079(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1080(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1081(t *testing.T) {
	actualVal := Distance("string", "   ", 1, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1082(t *testing.T) {
	actualVal := Distance("string", "", 1, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1083(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1084(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1085(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, 0, math.MinInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1086(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1087(t *testing.T) {
	actualVal := Distance("   ", "string", 0, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1088(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1089(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1090(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1091(t *testing.T) {
	actualVal := Distance("", "string", 0, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1092(t *testing.T) {
	actualVal := Distance("string", "string", 0, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1093(t *testing.T) {
	actualVal := Distance("string", "string", -1, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1094(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, -1, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1095(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1096(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1097(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1098(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1099(t *testing.T) {
	actualVal := Distance("", "   ", -1, 0, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1100(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1101(t *testing.T) {
	actualVal := Distance("", "string", -1, -1, math.MinInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1102(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1103(t *testing.T) {
	actualVal := Distance("   ", "", 1, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1104(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1105(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1106(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1107(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1108(t *testing.T) {
	actualVal := Distance("   ", "string", -1, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1109(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1110(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, -1, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1111(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1112(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1113(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, -1, math.MinInt)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1114(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1115(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1116(t *testing.T) {
	actualVal := Distance("", "   ", 0, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1117(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1118(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1119(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1120(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1121(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, 1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1122(t *testing.T) {
	actualVal := Distance("", "", 1, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1123(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1124(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1125(t *testing.T) {
	actualVal := Distance("string", "   ", 1, 1, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1126(t *testing.T) {
	actualVal := Distance("string", "", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1127(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1128(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1129(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1130(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1131(t *testing.T) {
	actualVal := Distance("   ", "string", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1132(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, 1, 0)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1133(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, math.MaxInt, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1134(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, -1, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1135(t *testing.T) {
	actualVal := Distance("", "string", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1136(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1137(t *testing.T) {
	actualVal := Distance("string", "string", 1, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1138(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, 0, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1139(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1140(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1141(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1142(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1143(t *testing.T) {
	actualVal := Distance("", "   ", 1, math.MinInt, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1144(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1145(t *testing.T) {
	actualVal := Distance("", "string", -1, 0, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1146(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1147(t *testing.T) {
	actualVal := Distance("   ", "", 1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1148(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1149(t *testing.T) {
	actualVal := Distance("   ", "", -1, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1150(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1151(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1152(t *testing.T) {
	actualVal := Distance("   ", "string", 1, math.MaxInt, 0)

	assert.Equal(t, -9223372036854775804, actualVal)
}

func TestDistanceByUtGoFuzzer1153(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1154(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, 0, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1155(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1156(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1157(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1158(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1159(t *testing.T) {
	actualVal := Distance("string", "", -1, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1160(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1161(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1162(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, 0, 0)

	assert.Equal(t, -2, actualVal)
}

func TestDistanceByUtGoFuzzer1163(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1164(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, 1, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1165(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1166(t *testing.T) {
	actualVal := Distance("", "", 1, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1167(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1168(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1169(t *testing.T) {
	actualVal := Distance("string", "   ", 1, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1170(t *testing.T) {
	actualVal := Distance("string", "", 0, math.MinInt, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1171(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, -1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1172(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1173(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1174(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1175(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1176(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, 0, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1177(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, math.MinInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1178(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, 1, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1179(t *testing.T) {
	actualVal := Distance("", "string", 0, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1180(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1181(t *testing.T) {
	actualVal := Distance("string", "string", 1, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1182(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1183(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1184(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, 1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1185(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1186(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1187(t *testing.T) {
	actualVal := Distance("", "   ", 1, -1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1188(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, -1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1189(t *testing.T) {
	actualVal := Distance("", "string", 1, math.MaxInt, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1190(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, 1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1191(t *testing.T) {
	actualVal := Distance("   ", "", 0, math.MaxInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1192(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, math.MinInt, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1193(t *testing.T) {
	actualVal := Distance("   ", "", -1, math.MinInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1194(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, -1, 1)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer1195(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1196(t *testing.T) {
	actualVal := Distance("   ", "string", 1, math.MinInt, 1)

	assert.Equal(t, -9223372036854775801, actualVal)
}

func TestDistanceByUtGoFuzzer1197(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, 1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1198(t *testing.T) {
	actualVal := Distance("string", "   ", -1, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1199(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1200(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, -1, 1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1201(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1202(t *testing.T) {
	actualVal := Distance("", "", -1, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1203(t *testing.T) {
	actualVal := Distance("string", "", -1, -1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1204(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1205(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1206(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1207(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1208(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, 0, 1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1209(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1210(t *testing.T) {
	actualVal := Distance("", "", 0, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1211(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1212(t *testing.T) {
	actualVal := Distance("", "string", -1, math.MaxInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1213(t *testing.T) {
	actualVal := Distance("string", "   ", 0, 1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1214(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1215(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1216(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1217(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1218(t *testing.T) {
	actualVal := Distance("string", "string", -1, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1219(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1220(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, 1, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1221(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1222(t *testing.T) {
	actualVal := Distance("", "   ", -1, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1223(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, 1, -1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1224(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1225(t *testing.T) {
	actualVal := Distance("string", "string", 0, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1226(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1227(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1228(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1229(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1230(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1231(t *testing.T) {
	actualVal := Distance("", "   ", 0, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1232(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer1233(t *testing.T) {
	actualVal := Distance("", "string", 1, 0, -1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1234(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, -1, -1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1235(t *testing.T) {
	actualVal := Distance("   ", "", 0, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1236(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, math.MaxInt, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1237(t *testing.T) {
	actualVal := Distance("   ", "", 1, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1238(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, math.MinInt, -1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1239(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1240(t *testing.T) {
	actualVal := Distance("   ", "string", 0, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1241(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1242(t *testing.T) {
	actualVal := Distance("string", "   ", -1, 0, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1243(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, -1, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1244(t *testing.T) {
	actualVal := Distance("   ", "string", -1, math.MinInt, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1245(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1246(t *testing.T) {
	actualVal := Distance("", "", -1, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1247(t *testing.T) {
	actualVal := Distance("string", "", 1, math.MinInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1248(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1249(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, 0, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1250(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1251(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1252(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, 1, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1253(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, math.MinInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1254(t *testing.T) {
	actualVal := Distance("", "", 1, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1255(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MaxInt, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1256(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, 1, 1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1257(t *testing.T) {
	actualVal := Distance("string", "   ", 1, math.MinInt, 1)

	assert.Equal(t, -9223372036854775801, actualVal)
}

func TestDistanceByUtGoFuzzer1258(t *testing.T) {
	actualVal := Distance("string", "", 1, 1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1259(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1260(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, math.MaxInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1261(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1262(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1263(t *testing.T) {
	actualVal := Distance("   ", "string", 0, -1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1264(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1265(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, 1, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1266(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1267(t *testing.T) {
	actualVal := Distance("", "string", 0, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1268(t *testing.T) {
	actualVal := Distance("string", "string", 0, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1269(t *testing.T) {
	actualVal := Distance("string", "string", -1, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1270(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1271(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, math.MinInt, 1)

	assert.Equal(t, -9223372036854775803, actualVal)
}

func TestDistanceByUtGoFuzzer1272(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1273(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1274(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1275(t *testing.T) {
	actualVal := Distance("", "   ", -1, 0, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1276(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1277(t *testing.T) {
	actualVal := Distance("", "string", -1, -1, 1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1278(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, math.MaxInt, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1279(t *testing.T) {
	actualVal := Distance("   ", "", 1, -1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1280(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, 1, 1)

	assert.Equal(t, -9223372036854775803, actualVal)
}

func TestDistanceByUtGoFuzzer1281(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1282(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1283(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1284(t *testing.T) {
	actualVal := Distance("   ", "string", -1, 1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1285(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1286(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, -1, 1)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1287(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1288(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, 0, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1289(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, -1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1290(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1291(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, 0, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1292(t *testing.T) {
	actualVal := Distance("", "   ", 0, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1293(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1294(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, -1, 1)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer1295(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1296(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1297(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, 0, math.MinInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1298(t *testing.T) {
	actualVal := Distance("", "", 0, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1299(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1300(t *testing.T) {
	actualVal := Distance("", "string", -1, math.MinInt, math.MinInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1301(t *testing.T) {
	actualVal := Distance("string", "   ", 0, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1302(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1303(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1304(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1305(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1306(t *testing.T) {
	actualVal := Distance("string", "string", -1, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1307(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1308(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, 0, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1309(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1310(t *testing.T) {
	actualVal := Distance("", "   ", -1, 1, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1311(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1312(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1313(t *testing.T) {
	actualVal := Distance("string", "string", 0, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1314(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer1315(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1316(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1317(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1318(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1319(t *testing.T) {
	actualVal := Distance("", "   ", 0, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1320(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1321(t *testing.T) {
	actualVal := Distance("", "string", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1322(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, 1, math.MinInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1323(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1324(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, math.MinInt, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1325(t *testing.T) {
	actualVal := Distance("   ", "", 1, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1326(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, -1, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1327(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1328(t *testing.T) {
	actualVal := Distance("   ", "string", 0, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1329(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1330(t *testing.T) {
	actualVal := Distance("string", "   ", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer1331(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1332(t *testing.T) {
	actualVal := Distance("   ", "string", -1, -1, math.MinInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1333(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1334(t *testing.T) {
	actualVal := Distance("", "", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1335(t *testing.T) {
	actualVal := Distance("string", "", 1, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1336(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1337(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1338(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1339(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1340(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1341(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1342(t *testing.T) {
	actualVal := Distance("", "", 0, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1343(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1344(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1345(t *testing.T) {
	actualVal := Distance("string", "   ", 0, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1346(t *testing.T) {
	actualVal := Distance("string", "", 0, 0, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1347(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1348(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1349(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1350(t *testing.T) {
	actualVal := Distance("string", "string", -1, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1351(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1352(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, -1, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1353(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, 0, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1354(t *testing.T) {
	actualVal := Distance("", "   ", -1, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1355(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, -1, -1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1356(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, 0, -1)

	assert.Equal(t, -7, actualVal)
}

func TestDistanceByUtGoFuzzer1357(t *testing.T) {
	actualVal := Distance("string", "string", 0, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1358(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1359(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1360(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, math.MinInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1361(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1362(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1363(t *testing.T) {
	actualVal := Distance("", "   ", 0, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1364(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer1365(t *testing.T) {
	actualVal := Distance("", "string", 1, 1, -1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1366(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, math.MinInt, -1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1367(t *testing.T) {
	actualVal := Distance("   ", "", 0, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1368(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1369(t *testing.T) {
	actualVal := Distance("   ", "", -1, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1370(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1371(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1372(t *testing.T) {
	actualVal := Distance("   ", "string", 1, 0, -1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1373(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1374(t *testing.T) {
	actualVal := Distance("string", "   ", -1, 1, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1375(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1376(t *testing.T) {
	actualVal := Distance("   ", "string", -1, math.MaxInt, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1377(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1378(t *testing.T) {
	actualVal := Distance("", "", -1, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1379(t *testing.T) {
	actualVal := Distance("string", "", 1, math.MaxInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1380(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1381(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, 1, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1382(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, 1, -1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1383(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1384(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, -1, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1385(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, math.MaxInt, 0)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1386(t *testing.T) {
	actualVal := Distance("", "", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1387(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1388(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1389(t *testing.T) {
	actualVal := Distance("string", "   ", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1390(t *testing.T) {
	actualVal := Distance("string", "", 0, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1391(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1392(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1393(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1394(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1395(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1396(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, math.MaxInt, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1397(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1398(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, 0, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1399(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1400(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1401(t *testing.T) {
	actualVal := Distance("string", "string", 1, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1402(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, math.MinInt, 0)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1403(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1404(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1405(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1406(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1407(t *testing.T) {
	actualVal := Distance("", "   ", 1, 1, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1408(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1409(t *testing.T) {
	actualVal := Distance("", "string", 1, math.MinInt, 0)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1410(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, 0, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1411(t *testing.T) {
	actualVal := Distance("   ", "", 0, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1412(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, -1, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1413(t *testing.T) {
	actualVal := Distance("   ", "", -1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1414(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, 1, 0)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1415(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1416(t *testing.T) {
	actualVal := Distance("   ", "string", 1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1417(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, 0, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1418(t *testing.T) {
	actualVal := Distance("string", "   ", -1, math.MinInt, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1419(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1420(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, 1, 0)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1421(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1422(t *testing.T) {
	actualVal := Distance("", "", -1, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1423(t *testing.T) {
	actualVal := Distance("string", "", -1, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1424(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1425(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1426(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1427(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1428(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1429(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1430(t *testing.T) {
	actualVal := Distance("", "", 0, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1431(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1432(t *testing.T) {
	actualVal := Distance("", "string", -1, math.MinInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1433(t *testing.T) {
	actualVal := Distance("string", "   ", 0, 0, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1434(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1435(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1436(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1437(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1438(t *testing.T) {
	actualVal := Distance("string", "string", -1, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1439(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1440(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, 0, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1441(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, math.MinInt, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1442(t *testing.T) {
	actualVal := Distance("", "   ", -1, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1443(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, 0, -1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1444(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, math.MinInt, -1)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1445(t *testing.T) {
	actualVal := Distance("string", "string", 0, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1446(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, math.MaxInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1447(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1448(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, 1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1449(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1450(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1451(t *testing.T) {
	actualVal := Distance("", "   ", 0, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1452(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, -1, -1)

	assert.Equal(t, -8, actualVal)
}

func TestDistanceByUtGoFuzzer1453(t *testing.T) {
	actualVal := Distance("", "string", 0, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1454(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, 1, -1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1455(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1456(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, math.MinInt, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1457(t *testing.T) {
	actualVal := Distance("   ", "", 1, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1458(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1459(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1460(t *testing.T) {
	actualVal := Distance("   ", "string", 0, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1461(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1462(t *testing.T) {
	actualVal := Distance("string", "   ", 1, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1463(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, 1, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1464(t *testing.T) {
	actualVal := Distance("   ", "string", -1, -1, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1465(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1466(t *testing.T) {
	actualVal := Distance("", "", 1, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1467(t *testing.T) {
	actualVal := Distance("string", "", 1, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1468(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1469(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1470(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, math.MaxInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1471(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1472(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, 0, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1473(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1474(t *testing.T) {
	actualVal := Distance("", "", 0, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1475(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1476(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1477(t *testing.T) {
	actualVal := Distance("string", "   ", 0, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775804, actualVal)
}

func TestDistanceByUtGoFuzzer1478(t *testing.T) {
	actualVal := Distance("string", "", 0, -1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1479(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1480(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1481(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1482(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1483(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1484(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, math.MaxInt, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1485(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, -1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1486(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, 0, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1487(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1488(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, -1, 1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1489(t *testing.T) {
	actualVal := Distance("string", "string", 1, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1490(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, math.MinInt, 1)

	assert.Equal(t, -9223372036854775801, actualVal)
}

func TestDistanceByUtGoFuzzer1491(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1492(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, 0, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1493(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1494(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1495(t *testing.T) {
	actualVal := Distance("", "   ", 1, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1496(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1497(t *testing.T) {
	actualVal := Distance("", "string", 1, math.MinInt, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1498(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, 0, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1499(t *testing.T) {
	actualVal := Distance("   ", "", 0, math.MinInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1500(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, -1, 1)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1501(t *testing.T) {
	actualVal := Distance("   ", "", -1, -1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1502(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, 1, 1)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1503(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1504(t *testing.T) {
	actualVal := Distance("   ", "string", 1, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1505(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1506(t *testing.T) {
	actualVal := Distance("string", "   ", -1, math.MinInt, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1507(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1508(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, 1, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1509(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, math.MinInt, 1)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1510(t *testing.T) {
	actualVal := Distance("", "", -1, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1511(t *testing.T) {
	actualVal := Distance("string", "", -1, 1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1512(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1513(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1514(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, math.MinInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1515(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1516(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1517(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1518(t *testing.T) {
	actualVal := Distance("", "", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1519(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1520(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1521(t *testing.T) {
	actualVal := Distance("string", "   ", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer1522(t *testing.T) {
	actualVal := Distance("string", "", 0, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1523(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1524(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1525(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1526(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1527(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1528(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1529(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, -1, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1530(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1531(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1532(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1533(t *testing.T) {
	actualVal := Distance("string", "string", 1, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1534(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1535(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1536(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1537(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1538(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1539(t *testing.T) {
	actualVal := Distance("", "   ", 1, 1, math.MinInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1540(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1541(t *testing.T) {
	actualVal := Distance("", "string", 1, math.MinInt, math.MinInt)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1542(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, 0, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1543(t *testing.T) {
	actualVal := Distance("   ", "", 0, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1544(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, -1, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1545(t *testing.T) {
	actualVal := Distance("   ", "", -1, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1546(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer1547(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1548(t *testing.T) {
	actualVal := Distance("   ", "string", 1, -1, math.MinInt)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1549(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, 0, math.MinInt)

	assert.Equal(t, -9223372036854775804, actualVal)
}

func TestDistanceByUtGoFuzzer1550(t *testing.T) {
	actualVal := Distance("string", "   ", -1, math.MinInt, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1551(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1552(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1553(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1554(t *testing.T) {
	actualVal := Distance("", "", -1, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1555(t *testing.T) {
	actualVal := Distance("string", "", -1, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1556(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1557(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, -5, actualVal)
}

func TestDistanceByUtGoFuzzer1558(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1559(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1560(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1561(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, -1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1562(t *testing.T) {
	actualVal := Distance("", "", 0, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1563(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1564(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1565(t *testing.T) {
	actualVal := Distance("string", "   ", 0, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1566(t *testing.T) {
	actualVal := Distance("string", "", 0, 0, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1567(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1568(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, math.MinInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1569(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1570(t *testing.T) {
	actualVal := Distance("string", "string", -1, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1571(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1572(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, -1, 1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1573(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1574(t *testing.T) {
	actualVal := Distance("", "   ", -1, math.MinInt, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1575(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, -1, 1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1576(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, 0, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1577(t *testing.T) {
	actualVal := Distance("string", "string", 0, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1578(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, 1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1579(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1580(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, math.MinInt, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1581(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1582(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, -1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1583(t *testing.T) {
	actualVal := Distance("", "   ", 0, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1584(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1585(t *testing.T) {
	actualVal := Distance("", "string", 1, 1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1586(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, math.MinInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1587(t *testing.T) {
	actualVal := Distance("   ", "", 0, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1588(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, 0, 1)

	assert.Equal(t, -9223372036854775804, actualVal)
}

func TestDistanceByUtGoFuzzer1589(t *testing.T) {
	actualVal := Distance("   ", "", -1, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1590(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1591(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1592(t *testing.T) {
	actualVal := Distance("   ", "string", 1, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1593(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, math.MinInt, 1)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1594(t *testing.T) {
	actualVal := Distance("string", "   ", -1, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1595(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1596(t *testing.T) {
	actualVal := Distance("   ", "string", -1, math.MaxInt, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1597(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1598(t *testing.T) {
	actualVal := Distance("", "", -1, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1599(t *testing.T) {
	actualVal := Distance("string", "", 1, math.MaxInt, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1600(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1601(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, 1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1602(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, 1, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1603(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1604(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, -1, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1605(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, -1, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1606(t *testing.T) {
	actualVal := Distance("", "", 1, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1607(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1608(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1609(t *testing.T) {
	actualVal := Distance("string", "   ", 1, -1, math.MinInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1610(t *testing.T) {
	actualVal := Distance("string", "", 1, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1611(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1612(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1613(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1614(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1615(t *testing.T) {
	actualVal := Distance("   ", "string", 0, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1616(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, -1, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1617(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1618(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1619(t *testing.T) {
	actualVal := Distance("", "string", 0, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1620(t *testing.T) {
	actualVal := Distance("string", "string", 0, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1621(t *testing.T) {
	actualVal := Distance("string", "string", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1622(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, 1, math.MinInt)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1623(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1624(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1625(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1626(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1627(t *testing.T) {
	actualVal := Distance("", "   ", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1628(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1629(t *testing.T) {
	actualVal := Distance("", "string", -1, 1, math.MinInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1630(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, math.MinInt, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1631(t *testing.T) {
	actualVal := Distance("   ", "", 1, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1632(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1633(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1634(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1635(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1636(t *testing.T) {
	actualVal := Distance("   ", "string", -1, 0, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1637(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1638(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1639(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1640(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1641(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, 1, math.MinInt)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1642(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1643(t *testing.T) {
	actualVal := Distance("string", "", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1644(t *testing.T) {
	actualVal := Distance("", "   ", 0, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1645(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1646(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer1647(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1648(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1649(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1650(t *testing.T) {
	actualVal := Distance("", "", 0, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1651(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1652(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1653(t *testing.T) {
	actualVal := Distance("string", "   ", 0, math.MaxInt, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1654(t *testing.T) {
	actualVal := Distance("string", "", 0, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1655(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1656(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1657(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1658(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1659(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1660(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, math.MaxInt, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1661(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1662(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, 0, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1663(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1664(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, -1, -1)

	assert.Equal(t, -8, actualVal)
}

func TestDistanceByUtGoFuzzer1665(t *testing.T) {
	actualVal := Distance("string", "string", 1, 1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1666(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1667(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1668(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, 0, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1669(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, 0, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1670(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1671(t *testing.T) {
	actualVal := Distance("", "   ", 1, 1, -1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1672(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, 1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1673(t *testing.T) {
	actualVal := Distance("", "string", 1, math.MinInt, -1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1674(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1675(t *testing.T) {
	actualVal := Distance("   ", "", 0, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1676(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, -1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1677(t *testing.T) {
	actualVal := Distance("   ", "", -1, -1, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1678(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, 1, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1679(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, math.MaxInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1680(t *testing.T) {
	actualVal := Distance("   ", "string", 1, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1681(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, 0, -1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1682(t *testing.T) {
	actualVal := Distance("string", "   ", -1, math.MinInt, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1683(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, 0, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1684(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, 1, -1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1685(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, math.MinInt, -1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1686(t *testing.T) {
	actualVal := Distance("", "", -1, math.MinInt, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1687(t *testing.T) {
	actualVal := Distance("string", "", -1, 1, -1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1688(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1689(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, math.MinInt, -1)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1690(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, math.MinInt, -1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1691(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, -1, -1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1692(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, math.MaxInt, -1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1693(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, 0, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1694(t *testing.T) {
	actualVal := Distance("", "", 1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1695(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1696(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1697(t *testing.T) {
	actualVal := Distance("string", "   ", 1, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1698(t *testing.T) {
	actualVal := Distance("string", "", 0, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1699(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1700(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1701(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1702(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1703(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1704(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, 0, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1705(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, math.MinInt, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1706(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, 1, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1707(t *testing.T) {
	actualVal := Distance("", "string", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1708(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1709(t *testing.T) {
	actualVal := Distance("string", "string", 1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1710(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, math.MaxInt, 0)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1711(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1712(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1713(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1714(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1715(t *testing.T) {
	actualVal := Distance("", "   ", 1, -1, 0)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1716(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1717(t *testing.T) {
	actualVal := Distance("", "string", 1, math.MaxInt, 0)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1718(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, 1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1719(t *testing.T) {
	actualVal := Distance("   ", "", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1720(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, math.MinInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1721(t *testing.T) {
	actualVal := Distance("   ", "", -1, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1722(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, -1, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1723(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, 0, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1724(t *testing.T) {
	actualVal := Distance("   ", "string", 1, math.MinInt, 0)

	assert.Equal(t, -9223372036854775803, actualVal)
}

func TestDistanceByUtGoFuzzer1725(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, 1, 0)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1726(t *testing.T) {
	actualVal := Distance("string", "   ", -1, math.MaxInt, 0)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1727(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, 1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1728(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1729(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1730(t *testing.T) {
	actualVal := Distance("", "", -1, math.MaxInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1731(t *testing.T) {
	actualVal := Distance("string", "", -1, -1, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1732(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1733(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, math.MaxInt, 0)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1734(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, math.MaxInt, 0)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1735(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, math.MinInt, 0)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1736(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, 0, 0)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1737(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1738(t *testing.T) {
	actualVal := Distance("", "", 0, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1739(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1740(t *testing.T) {
	actualVal := Distance("", "string", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1741(t *testing.T) {
	actualVal := Distance("string", "   ", 0, 1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1742(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -4, actualVal)
}

func TestDistanceByUtGoFuzzer1743(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1744(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1745(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1746(t *testing.T) {
	actualVal := Distance("string", "string", -1, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1747(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1748(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1749(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1750(t *testing.T) {
	actualVal := Distance("", "   ", -1, -1, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1751(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1752(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1753(t *testing.T) {
	actualVal := Distance("string", "string", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1754(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, 0, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1755(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1756(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1757(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1758(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1759(t *testing.T) {
	actualVal := Distance("", "   ", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1760(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -7, actualVal)
}

func TestDistanceByUtGoFuzzer1761(t *testing.T) {
	actualVal := Distance("", "string", 1, 0, math.MaxInt)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1762(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, -1, math.MaxInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1763(t *testing.T) {
	actualVal := Distance("   ", "", 0, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1764(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1765(t *testing.T) {
	actualVal := Distance("   ", "", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1766(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1767(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1768(t *testing.T) {
	actualVal := Distance("   ", "string", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1769(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, -1, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1770(t *testing.T) {
	actualVal := Distance("string", "   ", -1, 0, math.MaxInt)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1771(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer1772(t *testing.T) {
	actualVal := Distance("   ", "string", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer1773(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, 0, math.MaxInt)

	assert.Equal(t, -2, actualVal)
}

func TestDistanceByUtGoFuzzer1774(t *testing.T) {
	actualVal := Distance("", "", -1, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1775(t *testing.T) {
	actualVal := Distance("string", "", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1776(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1777(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer1778(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1779(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1780(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1781(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, -1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1782(t *testing.T) {
	actualVal := Distance("", "", 1, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1783(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1784(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, 0, 1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1785(t *testing.T) {
	actualVal := Distance("string", "   ", 1, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1786(t *testing.T) {
	actualVal := Distance("string", "", 1, 0, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1787(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1788(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, math.MinInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1789(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1790(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1791(t *testing.T) {
	actualVal := Distance("   ", "string", 0, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1792(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, -1, 1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1793(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, 0, 1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1794(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, math.MinInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1795(t *testing.T) {
	actualVal := Distance("", "string", 0, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1796(t *testing.T) {
	actualVal := Distance("string", "string", 0, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1797(t *testing.T) {
	actualVal := Distance("string", "string", 1, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1798(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1799(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1800(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1801(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1802(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, -1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1803(t *testing.T) {
	actualVal := Distance("", "   ", 1, math.MaxInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1804(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, math.MaxInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1805(t *testing.T) {
	actualVal := Distance("", "string", -1, 1, 1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1806(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, math.MinInt, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1807(t *testing.T) {
	actualVal := Distance("   ", "", 1, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1808(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, 0, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1809(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1810(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, 9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1811(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1812(t *testing.T) {
	actualVal := Distance("   ", "string", -1, 0, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1813(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, math.MinInt, 1)

	assert.Equal(t, -9223372036854775801, actualVal)
}

func TestDistanceByUtGoFuzzer1814(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, 1, 1)

	assert.Equal(t, -9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1815(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, math.MinInt, 1)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1816(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer1817(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1818(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1819(t *testing.T) {
	actualVal := Distance("string", "", -1, math.MaxInt, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1820(t *testing.T) {
	actualVal := Distance("", "   ", 0, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1821(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1822(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, 1, 1)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1823(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1824(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, -1, 1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1825(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", -1, 1, math.MinInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1826(t *testing.T) {
	actualVal := Distance("", "", 1, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1827(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1828(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1829(t *testing.T) {
	actualVal := Distance("string", "   ", 1, 1, math.MinInt)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1830(t *testing.T) {
	actualVal := Distance("string", "", 0, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1831(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1832(t *testing.T) {
	actualVal := Distance("\n\t\r", "", -1, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1833(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1834(t *testing.T) {
	actualVal := Distance("string", "string", math.MinInt, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1835(t *testing.T) {
	actualVal := Distance("   ", "string", 0, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1836(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MinInt, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1837(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1838(t *testing.T) {
	actualVal := Distance("", "   ", math.MinInt, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1839(t *testing.T) {
	actualVal := Distance("", "string", 0, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1840(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1841(t *testing.T) {
	actualVal := Distance("string", "string", 1, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1842(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1843(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 0, 1, math.MinInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1844(t *testing.T) {
	actualVal := Distance("string", "", math.MaxInt, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1845(t *testing.T) {
	actualVal := Distance("   ", "   ", 1, -1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1846(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1847(t *testing.T) {
	actualVal := Distance("", "   ", 1, math.MinInt, math.MinInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1848(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1849(t *testing.T) {
	actualVal := Distance("", "string", -1, 0, math.MinInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1850(t *testing.T) {
	actualVal := Distance("", "\n\t\r", -1, -1, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1851(t *testing.T) {
	actualVal := Distance("   ", "", 1, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1852(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1853(t *testing.T) {
	actualVal := Distance("   ", "", -1, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1854(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1855(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MinInt, 1, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1856(t *testing.T) {
	actualVal := Distance("   ", "string", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1857(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 1, -1, math.MinInt)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1858(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, 0, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1859(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 0, -1, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1860(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, math.MinInt, math.MinInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1861(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, 0, math.MinInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1862(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1863(t *testing.T) {
	actualVal := Distance("string", "", -1, math.MinInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1864(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1865(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, 0, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1866(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, 0, math.MinInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1867(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, math.MaxInt, math.MinInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1868(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MaxInt, 1, math.MinInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1869(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, -1, math.MaxInt)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer1870(t *testing.T) {
	actualVal := Distance("", "", 0, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1871(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1872(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1873(t *testing.T) {
	actualVal := Distance("string", "   ", 0, -1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1874(t *testing.T) {
	actualVal := Distance("string", "", 0, 0, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1875(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1876(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1877(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1878(t *testing.T) {
	actualVal := Distance("string", "string", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1879(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1880(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, -1, math.MaxInt)

	assert.Equal(t, -8, actualVal)
}

func TestDistanceByUtGoFuzzer1881(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, 0, math.MaxInt)

	assert.Equal(t, -4, actualVal)
}

func TestDistanceByUtGoFuzzer1882(t *testing.T) {
	actualVal := Distance("", "   ", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1883(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1884(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer1885(t *testing.T) {
	actualVal := Distance("string", "string", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1886(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, 1, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1887(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1888(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1889(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1890(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1891(t *testing.T) {
	actualVal := Distance("", "   ", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1892(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -8, actualVal)
}

func TestDistanceByUtGoFuzzer1893(t *testing.T) {
	actualVal := Distance("", "string", 1, 1, math.MaxInt)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1894(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1895(t *testing.T) {
	actualVal := Distance("   ", "", 0, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1896(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1897(t *testing.T) {
	actualVal := Distance("   ", "", -1, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1898(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1899(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1900(t *testing.T) {
	actualVal := Distance("   ", "string", 1, 0, math.MaxInt)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1901(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1902(t *testing.T) {
	actualVal := Distance("string", "   ", -1, 1, math.MaxInt)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1903(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -7, actualVal)
}

func TestDistanceByUtGoFuzzer1904(t *testing.T) {
	actualVal := Distance("   ", "string", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer1905(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1906(t *testing.T) {
	actualVal := Distance("", "", -1, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1907(t *testing.T) {
	actualVal := Distance("string", "", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1908(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1909(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer1910(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, -1, actualVal)
}

func TestDistanceByUtGoFuzzer1911(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1912(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1913(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, math.MinInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1914(t *testing.T) {
	actualVal := Distance("", "", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1915(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1916(t *testing.T) {
	actualVal := Distance("", "string", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1917(t *testing.T) {
	actualVal := Distance("string", "   ", 0, math.MinInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1918(t *testing.T) {
	actualVal := Distance("string", "", 0, 1, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1919(t *testing.T) {
	actualVal := Distance("   ", "", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1920(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1921(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 0, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1922(t *testing.T) {
	actualVal := Distance("string", "string", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1923(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1924(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer1925(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", -1, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1926(t *testing.T) {
	actualVal := Distance("", "   ", -1, math.MaxInt, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1927(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1928(t *testing.T) {
	actualVal := Distance("string", "   ", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer1929(t *testing.T) {
	actualVal := Distance("string", "string", 1, 0, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1930(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, -1, math.MaxInt)

	assert.Equal(t, -5, actualVal)
}

func TestDistanceByUtGoFuzzer1931(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1932(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1933(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1934(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1935(t *testing.T) {
	actualVal := Distance("", "   ", 1, 0, math.MaxInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1936(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 0, 0, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1937(t *testing.T) {
	actualVal := Distance("", "string", 1, -1, math.MaxInt)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1938(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, math.MaxInt, math.MaxInt)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1939(t *testing.T) {
	actualVal := Distance("   ", "", 0, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1940(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MinInt, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775802, actualVal)
}

func TestDistanceByUtGoFuzzer1941(t *testing.T) {
	actualVal := Distance("   ", "", -1, 1, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1942(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MaxInt, 0, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1943(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, math.MinInt, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1944(t *testing.T) {
	actualVal := Distance("   ", "string", 1, 1, math.MaxInt)

	assert.Equal(t, -9223372036854775803, actualVal)
}

func TestDistanceByUtGoFuzzer1945(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, math.MaxInt, math.MaxInt)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1946(t *testing.T) {
	actualVal := Distance("string", "   ", -1, -1, math.MaxInt)

	assert.Equal(t, -9, actualVal)
}

func TestDistanceByUtGoFuzzer1947(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, math.MaxInt, math.MaxInt)

	assert.Equal(t, -8, actualVal)
}

func TestDistanceByUtGoFuzzer1948(t *testing.T) {
	actualVal := Distance("   ", "string", math.MinInt, 0, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1949(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, -1, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1950(t *testing.T) {
	actualVal := Distance("", "", -1, -1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1951(t *testing.T) {
	actualVal := Distance("string", "", -1, 0, math.MaxInt)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1952(t *testing.T) {
	actualVal := Distance("", "", math.MaxInt, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1953(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, -1, math.MaxInt)

	assert.Equal(t, 9223372036854775799, actualVal)
}

func TestDistanceByUtGoFuzzer1954(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, -1, math.MaxInt)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1955(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 1, 1, math.MaxInt)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1956(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, math.MinInt, math.MaxInt)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1957(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", 1, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1958(t *testing.T) {
	actualVal := Distance("", "", 0, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1959(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", -1, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1960(t *testing.T) {
	actualVal := Distance("", "string", -1, math.MaxInt, 1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1961(t *testing.T) {
	actualVal := Distance("string", "   ", 0, 1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1962(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MaxInt, math.MaxInt, 1)

	assert.Equal(t, 9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1963(t *testing.T) {
	actualVal := Distance("   ", "", math.MinInt, math.MinInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1964(t *testing.T) {
	actualVal := Distance("\n\t\r", "", 1, -1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1965(t *testing.T) {
	actualVal := Distance("", "string", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, -6, actualVal)
}

func TestDistanceByUtGoFuzzer1966(t *testing.T) {
	actualVal := Distance("string", "string", -1, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1967(t *testing.T) {
	actualVal := Distance("   ", "   ", math.MaxInt, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1968(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", -1, 1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1969(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 1, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1970(t *testing.T) {
	actualVal := Distance("", "   ", -1, -1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1971(t *testing.T) {
	actualVal := Distance("", "   ", math.MaxInt, 1, 1)

	assert.Equal(t, 9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1972(t *testing.T) {
	actualVal := Distance("string", "   ", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1973(t *testing.T) {
	actualVal := Distance("string", "string", 0, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1974(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", 1, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1975(t *testing.T) {
	actualVal := Distance("string", "string", math.MaxInt, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1976(t *testing.T) {
	actualVal := Distance("string", "", math.MinInt, -1, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1977(t *testing.T) {
	actualVal := Distance("   ", "   ", 0, -1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1978(t *testing.T) {
	actualVal := Distance("\n\t\r", "", math.MinInt, 1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1979(t *testing.T) {
	actualVal := Distance("", "   ", 0, math.MinInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1980(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", math.MaxInt, math.MinInt, 1)

	assert.Equal(t, -9223372036854775805, actualVal)
}

func TestDistanceByUtGoFuzzer1981(t *testing.T) {
	actualVal := Distance("", "string", 1, 0, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1982(t *testing.T) {
	actualVal := Distance("", "\n\t\r", 1, -1, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1983(t *testing.T) {
	actualVal := Distance("   ", "", 0, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1984(t *testing.T) {
	actualVal := Distance("string", "\n\t\r", -1, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775806, actualVal)
}

func TestDistanceByUtGoFuzzer1985(t *testing.T) {
	actualVal := Distance("   ", "", 1, math.MaxInt, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1986(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", math.MinInt, math.MinInt, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestDistanceByUtGoFuzzer1987(t *testing.T) {
	actualVal := Distance("   ", "   ", -1, 1, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1988(t *testing.T) {
	actualVal := Distance("   ", "string", 0, math.MaxInt, 1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1989(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", 0, -1, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1990(t *testing.T) {
	actualVal := Distance("string", "   ", -1, 0, 1)

	assert.Equal(t, 3, actualVal)
}

func TestDistanceByUtGoFuzzer1991(t *testing.T) {
	actualVal := Distance("   ", "string", math.MaxInt, -1, 1)

	assert.Equal(t, -5, actualVal)
}

func TestDistanceByUtGoFuzzer1992(t *testing.T) {
	actualVal := Distance("   ", "string", -1, math.MinInt, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1993(t *testing.T) {
	actualVal := Distance("\n\t\r", "   ", 0, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1994(t *testing.T) {
	actualVal := Distance("", "", -1, 0, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1995(t *testing.T) {
	actualVal := Distance("string", "", 1, math.MinInt, 1)

	assert.Equal(t, 6, actualVal)
}

func TestDistanceByUtGoFuzzer1996(t *testing.T) {
	actualVal := Distance("", "", math.MinInt, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer1997(t *testing.T) {
	actualVal := Distance("\n\t\r", "string", math.MaxInt, 0, 1)

	assert.Equal(t, -3, actualVal)
}

func TestDistanceByUtGoFuzzer1998(t *testing.T) {
	actualVal := Distance("   ", "\n\t\r", math.MinInt, 0, 1)

	assert.Equal(t, -9223372036854775807, actualVal)
}

func TestDistanceByUtGoFuzzer1999(t *testing.T) {
	actualVal := Distance("\n\t\r", "\n\t\r", 0, math.MaxInt, 1)

	assert.Equal(t, 0, actualVal)
}

func TestDistanceByUtGoFuzzer2000(t *testing.T) {
	actualVal := Distance("", "\n\t\r", math.MinInt, 1, 1)

	assert.Equal(t, -9223372036854775808, actualVal)
}

func TestGeneratePanicsByUtGoFuzzer1(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(math.MinInt, 1) })
}

func TestGeneratePanicsByUtGoFuzzer2(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(1, 1) })
}

func TestGeneratePanicsByUtGoFuzzer3(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(math.MaxInt, 1) })
}

func TestGeneratePanicsByUtGoFuzzer4(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(-1, math.MaxInt) })
}

func TestGeneratePanicsByUtGoFuzzer5(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(math.MinInt, math.MaxInt) })
}

func TestGeneratePanicsByUtGoFuzzer6(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: makeslice: len out of range", func() { Generate(0, math.MaxInt) })
}

func TestGeneratePanicsByUtGoFuzzer7(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: makeslice: len out of range", func() { Generate(1, math.MaxInt) })
}

func TestGeneratePanicsByUtGoFuzzer8(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(math.MaxInt, math.MaxInt) })
}

func TestGeneratePanicsByUtGoFuzzer9(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(-1, math.MinInt) })
}

func TestGeneratePanicsByUtGoFuzzer10(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(math.MinInt, math.MinInt) })
}

func TestGeneratePanicsByUtGoFuzzer11(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(0, math.MinInt) })
}

func TestGeneratePanicsByUtGoFuzzer12(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: makeslice: len out of range", func() { Generate(1, math.MinInt) })
}

func TestGeneratePanicsByUtGoFuzzer13(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: makeslice: len out of range", func() { Generate(math.MaxInt, math.MinInt) })
}

func TestGeneratePanicsByUtGoFuzzer14(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: makeslice: len out of range", func() { Generate(-1, 0) })
}

func TestGeneratePanicsByUtGoFuzzer15(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(math.MinInt, 0) })
}

func TestGeneratePanicsByUtGoFuzzer16(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(0, 0) })
}

func TestGeneratePanicsByUtGoFuzzer17(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(1, 0) })
}

func TestGeneratePanicsByUtGoFuzzer18(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(math.MaxInt, 0) })
}

func TestGeneratePanicsByUtGoFuzzer19(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(-1, -1) })
}

func TestGeneratePanicsByUtGoFuzzer20(t *testing.T) {
	assert.PanicsWithError(t, "runtime error: makeslice: len out of range", func() { Generate(math.MinInt, -1) })
}

func TestGeneratePanicsByUtGoFuzzer21(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(0, -1) })
}

func TestGeneratePanicsByUtGoFuzzer22(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(1, -1) })
}

func TestGeneratePanicsByUtGoFuzzer23(t *testing.T) {
	assert.PanicsWithValue(t, "crypto/rand: argument to Int is <= 0", func() { Generate(math.MaxInt, -1) })
}

// Generate(-1, 1) exceeded 1000 ms timeout

// Generate(0, 1) exceeded 1000 ms timeout

func TestMonteCarloPiConcurrentWithNonNilErrorByUtGoFuzzer1(t *testing.T) {
	actualVal, actualErr := MonteCarloPiConcurrent(1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0.0, actualVal)
	assertMultiple.ErrorContains(actualErr, "x must be < n - given values are x=1, n=4")
}

func TestMonteCarloPiConcurrentWithNonNilErrorByUtGoFuzzer2(t *testing.T) {
	actualVal, actualErr := MonteCarloPiConcurrent(math.MinInt)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0.0, actualVal)
	assertMultiple.ErrorContains(actualErr, "x must be < n - given values are x=-9223372036854775808, n=4")
}

func TestMonteCarloPiConcurrentWithNonNilErrorByUtGoFuzzer3(t *testing.T) {
	actualVal, actualErr := MonteCarloPiConcurrent(0)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0.0, actualVal)
	assertMultiple.ErrorContains(actualErr, "x must be < n - given values are x=0, n=4")
}

func TestMonteCarloPiConcurrentWithNonNilErrorByUtGoFuzzer4(t *testing.T) {
	actualVal, actualErr := MonteCarloPiConcurrent(-1)

	assertMultiple := assert.New(t)
	assertMultiple.Equal(0.0, actualVal)
	assertMultiple.ErrorContains(actualErr, "x must be < n - given values are x=-1, n=4")
}

// MonteCarloPiConcurrent(math.MaxInt) exceeded 1000 ms timeout

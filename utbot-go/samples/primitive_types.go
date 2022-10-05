// Most of the algorithm examples are taken from https://github.com/TheAlgorithms/Go

package samples

import (
	cryptorand "crypto/rand"
	"fmt"
	f "github.com/TheAlgorithms/Go/math/factorial"
	"github.com/TheAlgorithms/Go/math/gcd"
	"io"
	"math"
	"math/big"
	"math/rand"
	"regexp"
	"runtime"
	"strings"
	"time"
)

// Abs returns absolute value
func Abs(n int) int {
	if n < 0 {
		return -n
	}
	return n
}

// DivOrPanic divides x by y or panics if y is 0
func DivOrPanic(x int, y int) int {
	if y == 0 {
		panic("div by 0")
	}
	return x / y
}

// Bitwise computes using bitwise operator the maximum of all the integer input and returns it
func Bitwise(a int, b int, base int) int {
	z := a - b
	i := (z >> base) & 1
	return a - (i * z)
}

// CatalanNumber This function returns the `nth` Catalan number
func CatalanNumber(n int) int {
	return f.Iterative(n*2) / (f.Iterative(n) * f.Iterative(n+1))
}

// Formula This function calculates the n-th fibonacci number using the [formula](https://en.wikipedia.org/wiki/Fibonacci_number#Relation_to_the_golden_ratio)
// Attention! Tests for large values fall due to rounding error of floating point numbers, works well, only on small numbers
func Formula(n uint) uint {
	sqrt5 := math.Sqrt(5)
	phi := (sqrt5 + 1) / 2
	powPhi := math.Pow(phi, float64(n))
	return uint(powPhi/sqrt5 + 0.5)
}

// Extended simple extended gcd
func Extended(a, b int64) (int64, int64, int64) {
	if a == 0 {
		return b, 0, 1
	}
	gcd, xPrime, yPrime := Extended(b%a, a)
	return gcd, yPrime - (b/a)*xPrime, xPrime
}

// Lcm returns the lcm of two numbers using the fact that lcm(a,b) * gcd(a,b) = | a * b |
func Lcm(a, b int64) int64 {
	return int64(math.Abs(float64(a*b)) / float64(gcd.Iterative(a, b)))
}

// IterativePower is iterative O(logn) function for pow(x, y)
func IterativePower(n uint, power uint) uint {
	var res uint = 1
	for power > 0 {
		if (power & 1) != 0 {
			res = res * n
		}

		power = power >> 1
		n *= n
	}
	return res
}

// IsPowOfTwoUseLog This function checks if a number is a power of two using the logarithm.
// The limiting degree can be from 0 to 63.
// See alternatives in the binary package.
func IsPowOfTwoUseLog(number float64) bool {
	if number == 0 || math.Round(number) == math.MaxInt64 {
		return false
	}
	log := math.Log2(number)
	return log == math.Round(log)
}

// Reverse function that will take string,
// and returns the reverse of that string.
func Reverse(str string) string {
	rStr := []rune(str)
	for i, j := 0, len(rStr)-1; i < len(rStr)/2; i, j = i+1, j-1 {
		rStr[i], rStr[j] = rStr[j], rStr[i]
	}
	return string(rStr)
}

// Parenthesis algorithm checks if every opened parenthesis
// is closed correctly

// when parcounter is less than 0 is because a closing
// parenthesis is detected without an opening parenthesis
// that surrounds it

// parcounter will be 0 if all open parenthesis are closed
// correctly
func Parenthesis(text string) bool {
	parcounter := 0

	for _, r := range text {
		switch r {
		case '(':
			parcounter++
		case ')':
			parcounter--
		}
		if parcounter < 0 {
			return false
		}
	}
	return parcounter == 0
}

// IsPalindrome checks if text is palindrome
func IsPalindrome(text string) bool {
	clean_text := cleanString(text)
	var i, j int
	rune := []rune(clean_text)
	for i = 0; i < len(rune)/2; i++ {
		j = len(rune) - 1 - i
		if string(rune[i]) != string(rune[j]) {
			return false
		}
	}
	return true
}

func cleanString(text string) string {
	clean_text := strings.ToLower(text)
	clean_text = strings.Join(strings.Fields(clean_text), "") // Remove spaces
	regex, _ := regexp.Compile(`[^\p{L}\p{N} ]+`)             // Regular expression for alphanumeric only characters
	return regex.ReplaceAllString(clean_text, "")
}

// Distance Function that gives Levenshtein Distance
func Distance(str1, str2 string, icost, scost, dcost int) int {
	row1 := make([]int, len(str2)+1)
	row2 := make([]int, len(str2)+1)

	for i := 1; i <= len(str2); i++ {
		row1[i] = i * icost
	}

	for i := 1; i <= len(str1); i++ {
		row2[0] = i * dcost

		for j := 1; j <= len(str2); j++ {
			if str1[i-1] == str2[j-1] {
				row2[j] = row1[j-1]
			} else {
				ins := row2[j-1] + icost
				del := row1[j] + dcost
				sub := row1[j-1] + scost

				if ins < del && ins < sub {
					row2[j] = ins
				} else if del < sub {
					row2[j] = del
				} else {
					row2[j] = sub
				}
			}
		}
		row1, row2 = row2, row1
	}

	return row1[len(row1)-1]
}

// Generate returns a newly generated password
func Generate(minLength int, maxLength int) string {
	var chars = []byte("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+,.?/:;{}[]`~")

	length, err := cryptorand.Int(cryptorand.Reader, big.NewInt(int64(maxLength-minLength)))
	if err != nil {
		panic(err) // handle this gracefully
	}
	length.Add(length, big.NewInt(int64(minLength)))

	intLength := int(length.Int64())

	newPassword := make([]byte, intLength)
	randomData := make([]byte, intLength+intLength/4)
	clen := byte(len(chars))
	maxrb := byte(256 - (256 % len(chars)))
	i := 0
	for {
		if _, err := io.ReadFull(cryptorand.Reader, randomData); err != nil {
			panic(err)
		}
		for _, c := range randomData {
			if c >= maxrb {
				continue
			}
			newPassword[i] = chars[c%clen]
			i++
			if i == intLength {
				return string(newPassword)
			}
		}
	}
}

// MonteCarloPiConcurrent approximates the value of pi using the Monte Carlo method.
// Unlike the MonteCarloPi function (first version), this implementation uses
// goroutines and channels to parallelize the computation.
// More details on the Monte Carlo method available at https://en.wikipedia.org/wiki/Monte_Carlo_method.
// More details on goroutines parallelization available at https://go.dev/doc/effective_go#parallel.
func MonteCarloPiConcurrent(n int) (float64, error) {
	numCPU := runtime.GOMAXPROCS(0)
	c := make(chan int, numCPU)
	pointsToDraw, err := splitInt(n, numCPU) // split the task in sub-tasks of approximately equal sizes
	if err != nil {
		return 0, err
	}

	// launch numCPU parallel tasks
	for _, p := range pointsToDraw {
		go drawPoints(p, c)
	}

	// collect the tasks results
	inside := 0
	for i := 0; i < numCPU; i++ {
		inside += <-c
	}
	return float64(inside) / float64(n) * 4, nil
}

// drawPoints draws n random two-dimensional points in the interval [0, 1), [0, 1) and sends through c
// the number of points which where within the circle of center 0 and radius 1 (unit circle)
func drawPoints(n int, c chan<- int) {
	rnd := rand.New(rand.NewSource(time.Now().UnixNano()))
	inside := 0
	for i := 0; i < n; i++ {
		x, y := rnd.Float64(), rnd.Float64()
		if x*x+y*y <= 1 {
			inside++
		}
	}
	c <- inside
}

// splitInt takes an integer x and splits it within an integer slice of length n in the most uniform
// way possible.
// For example, splitInt(10, 3) will return []int{4, 3, 3}, nil
func splitInt(x int, n int) ([]int, error) {
	if x < n {
		return nil, fmt.Errorf("x must be < n - given values are x=%d, n=%d", x, n)
	}
	split := make([]int, n)
	if x%n == 0 {
		for i := 0; i < n; i++ {
			split[i] = x / n
		}
	} else {
		limit := x % n
		for i := 0; i < limit; i++ {
			split[i] = x/n + 1
		}
		for i := limit; i < n; i++ {
			split[i] = x / n
		}
	}
	return split, nil
}

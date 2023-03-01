// Most of the algorithm examples are taken from https://github.com/TheAlgorithms/Go

package simple

import (
	"errors"
	"math"
)

// DivOrPanic divides x by y or panics if y is 0
func DivOrPanic(x int, y int) int {
	if y == 0 {
		panic("div by 0")
	}
	return x / y
}

// Extended simple extended gcd
func Extended(a, b int64) (int64, int64, int64) {
	if a == 0 {
		return b, 0, 1
	}
	gcd, xPrime, yPrime := Extended(b%a, a)
	return gcd, yPrime - (b/a)*xPrime, xPrime
}

func ArraySum(array [5]int) int {
	sum := 0
	for _, elem := range array {
		sum += elem
	}
	return sum
}

func GenerateArrayOfIntegers(num int) [10]int {
	result := [10]int{}
	for i := range result {
		result[i] = num
	}
	return result
}

type Point struct {
	x, y float64
}

func DistanceBetweenTwoPoints(a, b Point) float64 {
	return math.Sqrt(math.Pow(a.x-b.x, 2) + math.Pow(a.y-b.y, 2))
}

func GetCoordinatesOfMiddleBetweenTwoPoints(a, b Point) (float64, float64) {
	return (a.x + b.x) / 2, (a.y + b.y) / 2
}

func GetCoordinateSumOfPoints(points []Point) (float64, float64) {
	sumX := 0.0
	sumY := 0.0
	for _, point := range points {
		sumX += point.x
		sumY += point.y
	}
	return sumX, sumY
}

type Circle struct {
	Center Point
	Radius float64
}

func GetAreaOfCircle(circle Circle) float64 {
	return math.Pi * math.Pow(circle.Radius, 2)
}

func IsIdentity(matrix [3][3]int) bool {
	for i := 0; i < 3; i++ {
		for j := 0; j < 3; j++ {
			if i == j && matrix[i][j] != 1 {
				return false
			}

			if i != j && matrix[i][j] != 0 {
				return false
			}
		}
	}
	return true
}

var ErrNotFound = errors.New("target not found in array")

// Binary search for target within a sorted array by repeatedly dividing the array in half and comparing the midpoint with the target.
// This function uses recursive call to itself.
// If a target is found, the index of the target is returned. Else the function return -1 and ErrNotFound.
func Binary(array []int, target int, lowIndex int, highIndex int) (int, error) {
	if highIndex < lowIndex {
		return -1, ErrNotFound
	}
	mid := lowIndex + (highIndex-lowIndex)/2
	if array[mid] > target {
		return Binary(array, target, lowIndex, mid-1)
	} else if array[mid] < target {
		return Binary(array, target, mid+1, highIndex)
	} else {
		return mid, nil
	}
}

func StringSearch(str string) bool {
	if len(str) != 3 {
		return false
	}
	if str[0] == 'A' {
		if str[1] == 'B' {
			if str[2] == 'C' {
				return true
			}
		}
	}
	return false
}

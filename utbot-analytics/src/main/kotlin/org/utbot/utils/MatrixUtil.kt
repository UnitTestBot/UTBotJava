package org.utbot.utils

import java.io.File
import java.io.FileNotFoundException
import java.util.ArrayList
import java.util.Scanner


object MatrixUtil {


    fun loadMatrix(file: String, swapAxis: Boolean): Array<DoubleArray> {
        val scanner = Scanner(File(file))
        val shape = scanner.nextLine().split(",").toTypedArray()
        val firstDim = shape[0].toInt()
        val secondDim = shape[1].toInt()
        val matrix: Array<DoubleArray>
        matrix = if (swapAxis) {
            Array(secondDim) { DoubleArray(firstDim) }
        } else {
            Array(firstDim) { DoubleArray(secondDim) }
        }
        for (i in 0 until firstDim) {
            val row = scanner.nextLine().split(",").toTypedArray()
            for (j in 0 until secondDim) {
                if (swapAxis) {
                    matrix[j][i] = row[j].toDouble()
                } else {
                    matrix[i][j] = row[j].toDouble()
                }
            }
        }
        return matrix
    }

    /**
     * Load vector from file
     * @param file - file with vector
     * @return vector
     */
    @Throws(FileNotFoundException::class)
    fun loadVector(file: String): DoubleArray {
        val scanner = Scanner(File(file))
        val dim = scanner.nextLine().toInt()
        val vector = DoubleArray(dim)
        vector.forEachIndexed { i, _ -> vector[i] = scanner.nextLine().toDouble() }

        return vector
    }

    /**
     * Matrix-vector product - Ax
     * [n, m] * [m,] = [n,]
     * @param vector - [m,]
     * @param matrix - [n, m]
     * @return [n,]
     */
    fun mmul(vector: DoubleArray, matrix: Array<DoubleArray>): DoubleArray {
        val firstDim = matrix.size
        val secondDim: Int = matrix[0].size
        val res = DoubleArray(firstDim)
        for (i in 0 until firstDim) {
            for (j in 0 until secondDim) {
                res[i] += vector[j] * matrix[i][j]
            }
        }
        return res
    }

    /**
     * Matrix-vector product with bias - Ax + b
     * [n, m] * [m,] = [n,]
     * @param vector - [m,]
     * @param matrix - [n, m]
     * @param bias - [n,]
     * @return [n,]
     */
    fun mmulBias(vector: DoubleArray, matrix: Array<DoubleArray>, bias: DoubleArray): DoubleArray? {
        val firstDim = matrix.size
        val secondDim: Int = matrix[0].size
        val res = DoubleArray(firstDim)
        for (i in 0 until firstDim) {
            for (j in 0 until secondDim) {
                res[i] += vector[j] * matrix[i][j]
            }
            res[i] += bias[i]
        }
        return res
    }

    /**
     * Matrix-vector1.concat(vector2) product - Ax.
     * @param vector1 - [m,]
     * @param vector2 - [m,]
     * @param matrix - [n, 2*m]
     * @return [n,]
     */
    fun mmul(vector1: DoubleArray, vector2: DoubleArray, matrix: Array<DoubleArray>): DoubleArray {
        val dim = vector1.size
        val res = DoubleArray(dim)
        for (i in 0 until dim) {
            for (j in 0 until dim) {
                res[i] += vector1[j] * matrix[i][j] + vector2[j] * matrix[i][dim + j]
            }
        }
        return res
    }

    /**
     * Sum of Matrix-vector product
     * @param input - [k, m]
     * @param matrix - [n, m]
     * @return [n]
     */
    fun mmulSum(input: ArrayList<DoubleArray>, matrix: Array<DoubleArray>): DoubleArray {
        val firstDim = matrix.size
        val secondDim: Int = matrix[0].size
        val res = DoubleArray(firstDim)
        for (vector in input) {
            for (i in 0 until firstDim) {
                for (j in 0 until secondDim) {
                    res[i] += vector[j] * matrix[i][j]
                }
            }
        }
        return res
    }

}

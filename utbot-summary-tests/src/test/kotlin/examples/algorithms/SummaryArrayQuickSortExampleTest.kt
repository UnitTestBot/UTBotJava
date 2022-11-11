package examples.algorithms

import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.utbot.examples.algorithms.ArraysQuickSort
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtClusterInfo
import org.utbot.testing.DoNotCalculate

@Tag("slow")
class SummaryArrayQuickSortExampleTest : SummaryTestCaseGeneratorTest(
    ArraysQuickSort::class,
) {
    @Test
    fun testSort() {
        val summary1 = "Test does not iterate for(int k = left; k < right; run[count] = k), for(int lo = run[count] - 1, hi = k; ++lo < --hi; ), while(++k <= right && a[k - 1] >= a[k]), while(++k <= right && a[k - 1] <= a[k]), while(k < right && a[k] == a[k + 1]), executes conditions:\n" +
                "    (right - left < 286): False,\n" +
                "    (count == 0): True\n" +
                "returns from: return;\n"
        val summary2 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): True,\n" +
                "        (leftmost): True\n" +
                "    returns from: return;\n" +
                "    \n" +
                "Test further returns from: return;\n"
        val summary3 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it executes conditions:\n" +
                "        (length < 47): True,\n" +
                "        (leftmost): True\n" +
                "    iterates the loop for(int i = left, j = i; i < right; j = ++i) once. \n" +
                "    Test further does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), returns from: return;\n" +
                "    \n" +
                "Test further returns from: return;\n"
        val summary4 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it executes conditions:\n" +
                "        (length < 47): True,\n" +
                "        (leftmost): True\n" +
                "    iterates the loop for(int i = left, j = i; i < right; j = ++i) once,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "        (j-- == left): True\n" +
                "    Test then does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), returns from: return;\n" +
                "    \n" +
                "Test later returns from: return;\n"
        val summary5 = "Test executes conditions:\n" +
                "    (right - left < 286): False\n" +
                "iterates the loop while(k < right && a[k] == a[k + 1]) once. \n" +
                "Test throws ArrayIndexOutOfBoundsException in: while(k < right && a[k] == a[k + 1])\n"
        val summary6 = "Test executes conditions:\n" +
                "    (right - left < 286): False\n" +
                "iterates the loop while(k < right && a[k] == a[k + 1]) once. \n" +
                "Test throws NullPointerException in: while(k < right && a[k] == a[k + 1])\n"
        val summary7 = "Test executes conditions:\n" +
                "    (right - left < 286): False\n" +
                "iterates the loop while(k < right && a[k] == a[k + 1]) once. \n" +
                "Test throws ArrayIndexOutOfBoundsException in: while(k < right && a[k] == a[k + 1])\n"
        val summary8 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): False\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary9 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): False\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary10 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): False\n" +
                "    \n" +
                "Test throws NullPointerException in: internalSort(a, left, right, true);\n"
        val summary11 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it executes conditions:\n" +
                "        (length < 47): True,\n" +
                "        (leftmost): True\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary12 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it executes conditions:\n" +
                "        (length < 47): True,\n" +
                "        (leftmost): True\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary13 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it executes conditions:\n" +
                "        (length < 47): True,\n" +
                "        (leftmost): True\n" +
                "    \n" +
                "Test throws NullPointerException in: internalSort(a, left, right, true);\n"
        val summary14 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): False,\n" +
                "        (a[e3] < a[e2]): False,\n" +
                "        (a[e4] < a[e3]): False\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary15 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): False,\n" +
                "        (a[e3] < a[e2]): False\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary16 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate for(int k = less - 1; ++k <= great; ), while(a[great] == pivot2), while(a[less] == pivot1), for(int k = less - 1; ++k <= great; ), while(a[--great] > pivot2), while(a[++less] < pivot1), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): False,\n" +
                "        (a[e3] < a[e2]): False,\n" +
                "        (a[e4] < a[e3]): False,\n" +
                "        (a[e5] < a[e4]): True,\n" +
                "        (t < a[e3]): True,\n" +
                "        (if (t < a[e2]) {\n" +
                "    a[e3] = a[e2];\n" +
                "    a[e2] = t;\n" +
                "    if (t < a[e1]) {\n" +
                "        a[e2] = a[e1];\n" +
                "        a[e1] = t;\n" +
                "    }\n" +
                "}): False,\n" +
                "        (a[e1] != a[e2]): False\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary17 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate for(int k = less - 1; ++k <= great; ), while(a[great] == pivot2), while(a[less] == pivot1), for(int k = less - 1; ++k <= great; ), while(a[--great] > pivot2), while(a[++less] < pivot1), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): False,\n" +
                "        (a[e3] < a[e2]): False,\n" +
                "        (a[e4] < a[e3]): False,\n" +
                "        (a[e5] < a[e4]): True,\n" +
                "        (t < a[e3]): True,\n" +
                "        (if (t < a[e2]) {\n" +
                "    a[e3] = a[e2];\n" +
                "    a[e2] = t;\n" +
                "    if (t < a[e1]) {\n" +
                "        a[e2] = a[e1];\n" +
                "        a[e1] = t;\n" +
                "    }\n" +
                "}): True,\n" +
                "        (if (t < a[e1]) {\n" +
                "    a[e2] = a[e1];\n" +
                "    a[e1] = t;\n" +
                "}): False,\n" +
                "        (a[e1] != a[e2]): False\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary18 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): False,\n" +
                "        (a[e3] < a[e2]): False,\n" +
                "        (a[e4] < a[e3]): False,\n" +
                "        (a[e5] < a[e4]): True,\n" +
                "        (t < a[e3]): True,\n" +
                "        (if (t < a[e2]) {\n" +
                "    a[e3] = a[e2];\n" +
                "    a[e2] = t;\n" +
                "    if (t < a[e1]) {\n" +
                "        a[e2] = a[e1];\n" +
                "        a[e1] = t;\n" +
                "    }\n" +
                "}): True,\n" +
                "        (if (t < a[e1]) {\n" +
                "    a[e2] = a[e1];\n" +
                "    a[e1] = t;\n" +
                "}): True,\n" +
                "        (a[e1] != a[e2]): True,\n" +
                "        (a[e2] != a[e3]): True,\n" +
                "        (a[e3] != a[e4]): True,\n" +
                "        (a[e4] != a[e5]): True\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary19 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate for(int k = less - 1; ++k <= great; ), while(a[great] == pivot2), while(a[less] == pivot1), for(int k = less - 1; ++k <= great; ), while(a[--great] > pivot2), while(a[++less] < pivot1), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): False,\n" +
                "        (a[e3] < a[e2]): False,\n" +
                "        (a[e4] < a[e3]): False,\n" +
                "        (a[e5] < a[e4]): True,\n" +
                "        (t < a[e3]): False,\n" +
                "        (a[e1] != a[e2]): True,\n" +
                "        (a[e2] != a[e3]): False,\n" +
                "        (a[k] == pivot): True,\n" +
                "        (a[k] == pivot): False,\n" +
                "        (ak < pivot): False\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary20 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate for(int k = less - 1; ++k <= great; ), while(a[great] == pivot2), while(a[less] == pivot1), for(int k = less - 1; ++k <= great; ), while(a[--great] > pivot2), while(a[++less] < pivot1), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): False,\n" +
                "        (a[e3] < a[e2]): False,\n" +
                "        (a[e4] < a[e3]): True,\n" +
                "        (t < a[e2]): True,\n" +
                "        (if (t < a[e1]) {\n" +
                "    a[e2] = a[e1];\n" +
                "    a[e1] = t;\n" +
                "}): True,\n" +
                "        (a[e5] < a[e4]): False,\n" +
                "        (a[e1] != a[e2]): True,\n" +
                "        (a[e2] != a[e3]): True,\n" +
                "        (a[e3] != a[e4]): False,\n" +
                "        (ak < pivot): True,\n" +
                "        (ak < pivot): False\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary21 = "Test executes conditions:\n" +
                "    (right - left < 286): False\n" +
                "iterates the loop while(k < right && a[k] == a[k + 1]) twice. \n" +
                "Test throws ArrayIndexOutOfBoundsException in: while(k < right && a[k] == a[k + 1])\n"
        val summary22 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): True,\n" +
                "        (a[e3] < a[e2]): True,\n" +
                "        (t < a[e1]): False\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary23 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): True,\n" +
                "        (a[e3] < a[e2]): True,\n" +
                "        (t < a[e1]): False,\n" +
                "        (a[e4] < a[e3]): True,\n" +
                "        (t < a[e2]): True,\n" +
                "        (if (t < a[e1]) {\n" +
                "    a[e2] = a[e1];\n" +
                "    a[e1] = t;\n" +
                "}): False\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary24 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): True,\n" +
                "        (a[e3] < a[e2]): True,\n" +
                "        (t < a[e1]): False,\n" +
                "        (a[e4] < a[e3]): True,\n" +
                "        (t < a[e2]): True,\n" +
                "        (if (t < a[e1]) {\n" +
                "    a[e2] = a[e1];\n" +
                "    a[e1] = t;\n" +
                "}): True\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary25 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): False\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary26 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): False,\n" +
                "        (a[e3] < a[e2]): True,\n" +
                "        (t < a[e1]): True\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary27 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): False,\n" +
                "        (a[e3] < a[e2]): True,\n" +
                "        (t < a[e1]): False,\n" +
                "        (a[e4] < a[e3]): False,\n" +
                "        (a[e5] < a[e4]): False,\n" +
                "        (a[e1] != a[e2]): True,\n" +
                "        (a[e2] != a[e3]): True,\n" +
                "        (a[e3] != a[e4]): True,\n" +
                "        (a[e4] != a[e5]): True\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary28 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): False,\n" +
                "        (a[e3] < a[e2]): True,\n" +
                "        (t < a[e1]): False,\n" +
                "        (a[e4] < a[e3]): False,\n" +
                "        (a[e5] < a[e4]): True,\n" +
                "        (t < a[e3]): False,\n" +
                "        (a[e1] != a[e2]): True,\n" +
                "        (a[e2] != a[e3]): True,\n" +
                "        (a[e3] != a[e4]): True,\n" +
                "        (a[e4] != a[e5]): True\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary29 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate while(last < a[--right]), for(int k = left; ++left <= right; k = ++left), while(a2 < a[--k]), while(a1 < a[--k]), for(int i = left, j = i; i < right; j = ++i), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): True,\n" +
                "        (a[e3] < a[e2]): False,\n" +
                "        (a[e4] < a[e3]): True,\n" +
                "        (t < a[e2]): False\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary30 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate for(int k = less - 1; ++k <= great; ), while(a[great] == pivot2), while(a[less] == pivot1), for(int k = less - 1; ++k <= great; ), while(a[--great] > pivot2), while(a[++less] < pivot1), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): True,\n" +
                "        (a[e3] < a[e2]): False,\n" +
                "        (a[e4] < a[e3]): False,\n" +
                "        (a[e5] < a[e4]): False,\n" +
                "        (a[e1] != a[e2]): True,\n" +
                "        (a[e2] != a[e3]): True,\n" +
                "        (a[e3] != a[e4]): False,\n" +
                "        (a[k] == pivot): False,\n" +
                "        (ak < pivot): False\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary31 = "Test executes conditions:\n" +
                "    (right - left < 286): True\n" +
                "calls {@link org.utbot.examples.algorithms.ArraysQuickSort#internalSort(int[],int,int,boolean)},\n" +
                "    there it does not iterate for(int k = less - 1; ++k <= great; ), while(a[great] == pivot2), while(a[less] == pivot1), for(int k = less - 1; ++k <= great; ), while(a[--great] > pivot2), while(a[++less] < pivot1), executes conditions:\n" +
                "        (length < 47): False,\n" +
                "        (a[e2] < a[e1]): True,\n" +
                "        (a[e3] < a[e2]): False,\n" +
                "        (a[e4] < a[e3]): False,\n" +
                "        (a[e5] < a[e4]): False,\n" +
                "        (a[e1] != a[e2]): True,\n" +
                "        (a[e2] != a[e3]): True,\n" +
                "        (a[e3] != a[e4]): True,\n" +
                "        (a[e4] != a[e5]): False,\n" +
                "        (a[k] == pivot): False,\n" +
                "        (ak < pivot): False\n" +
                "    \n" +
                "Test throws ArrayIndexOutOfBoundsException in: internalSort(a, left, right, true);\n"
        val summary32 = "Test executes conditions:\n" +
                "    (right - left < 286): False\n" +
                "iterates the loop for(int k = left; k < right; run[count] = k) once,\n" +
                "    inside this loop, the test iterates the loop while(k < right && a[k] == a[k + 1]) once. \n" +
                "Test then executes conditions:\n" +
                "    (k == right): False,\n" +
                "    (a[k] < a[k + 1]): True\n" +
                "iterates the loop while(++k <= right && a[k - 1] <= a[k])\n" +
                "Test throws ArrayIndexOutOfBoundsException in: while(++k <= right && a[k - 1] <= a[k])\n"
        val summary33 = "Test executes conditions:\n" +
                "    (right - left < 286): False\n" +
                "iterates the loop for(int k = left; k < right; run[count] = k) once,\n" +
                "    inside this loop, the test iterates the loop while(k < right && a[k] == a[k + 1]) twice. \n" +
                "Test then does not iterate while(++k <= right && a[k - 1] <= a[k]), executes conditions:\n" +
                "    (k == right): False\n" +
                "    (a[k] < a[k + 1]): False\n" +
                "    (a[k] > a[k + 1]): True\n" +
                "iterates the loop while(++k <= right && a[k - 1] >= a[k])\n" +
                "Test throws ArrayIndexOutOfBoundsException in: while(++k <= right && a[k - 1] >= a[k])\n"

        val methodName1 = "testSort_CountEqualsZero"
        val methodName2 = "testSort_Leftmost"
        val methodName3 = "testSort_AiGreaterOrEqualJOfA"
        val methodName4 = "testSort_PostfixDecrementJEqualsLeft"
        val methodName5 = "testSort_ThrowArrayIndexOutOfBoundsException"
        val methodName6 = "testSort_ThrowNullPointerException"
        val methodName7 = "testSort_ThrowArrayIndexOutOfBoundsException_1"
        val methodName8 = "testSort_ThrowArrayIndexOutOfBoundsException_2"
        val methodName9 = "testSort_ThrowArrayIndexOutOfBoundsException_3"
        val methodName10 = "testSort_ThrowNullPointerException_1"
        val methodName11 = "testSort_ThrowArrayIndexOutOfBoundsException_4"
        val methodName12 = "testSort_ThrowArrayIndexOutOfBoundsException_5"
        val methodName13 = "testSort_ThrowNullPointerException_2"
        val methodName14 = "testSort_ThrowArrayIndexOutOfBoundsException_6"
        val methodName15 = "testSort_ThrowArrayIndexOutOfBoundsException_7"
        val methodName16 = "testSort_TGreaterOrEqualE2OfA"
        val methodName17 = "testSort_TGreaterOrEqualE1OfA"
        val methodName18 = "testSort_TLessThanE1OfA"
        val methodName19 = "testSort_KOfAEqualsPivot"
        val methodName20 = "testSort_AkLessThanPivot"
        val methodName21 = "testSort_ThrowArrayIndexOutOfBoundsException_8"
        val methodName22 = "testSort_ThrowArrayIndexOutOfBoundsException_9"
        val methodName23 = "testSort_TGreaterOrEqualE1OfA_1"
        val methodName24 = "testSort_ThrowArrayIndexOutOfBoundsException_10"
        val methodName25 = "testSort_ThrowArrayIndexOutOfBoundsException_11"
        val methodName26 = "testSort_TLessThanE1OfA_1"
        val methodName27 = "testSort_ThrowArrayIndexOutOfBoundsException_12"
        val methodName28 = "testSort_ThrowArrayIndexOutOfBoundsException_13"
        val methodName29 = "testSort_TGreaterOrEqualE2OfA_1"
        val methodName30 = "testSort_ThrowArrayIndexOutOfBoundsException_14"
        val methodName31 = "testSort_E4OfAEqualsE5OfA"
        val methodName32 = "testSort_PrefixIncrementKLessOrEqualRightAndK1OfALessOrEqualKOfA"
        val methodName33 = "testSort_PrefixIncrementKLessOrEqualRightAndK1OfAGreaterOrEqualKOfA"

        val displayName1 = "right - left < 286 : False -> return"
        val displayName2 = "length < 47 : True -> return"
        val displayName3 = "while(ai < a[j]) -> return"
        val displayName4 = "while(ai < a[j]) -> return"
        val displayName5 = "while(k < right && a[k] == a[k + 1]) -> ThrowArrayIndexOutOfBoundsException"
        val displayName6 = "while(k < right && a[k] == a[k + 1]) -> ThrowNullPointerException"
        val displayName7 = "while(k < right && a[k] == a[k + 1]) -> ThrowArrayIndexOutOfBoundsException"
        val displayName8 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName9 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName10 = "internalSort(a, left, right, true) : True -> ThrowNullPointerException"
        val displayName11 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName12 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName13 = "internalSort(a, left, right, true) : True -> ThrowNullPointerException"
        val displayName14 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName15 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName16 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName17 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName18 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName19 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName20 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName21 = "while(k < right && a[k] == a[k + 1]) -> ThrowArrayIndexOutOfBoundsException"
        val displayName22 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName23 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName24 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName25 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName26 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName27 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName28 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName29 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName30 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName31 = "internalSort(a, left, right, true) : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName32 = "while(++k <= right && a[k - 1] <= a[k]) -> ThrowArrayIndexOutOfBoundsException"
        val displayName33 = "while(++k <= right && a[k - 1] >= a[k]) -> ThrowArrayIndexOutOfBoundsException"

        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3,
            summary4,
            summary5,
            summary6,
            summary7,
            summary8,
            summary9,
            summary10,
            summary11,
            summary12,
            summary13,
            summary14,
            summary15,
            summary16,
            summary17,
            summary18,
            summary19,
            summary20,
            summary21,
            summary22,
            summary23,
            summary24,
            summary25,
            summary26,
            summary27,
            summary28,
            summary29,
            summary30,
            summary31,
            summary32,
            summary33
        )

        val displayNames = listOf(
            displayName1,
            displayName2,
            displayName3,
            displayName4,
            displayName5,
            displayName6,
            displayName7,
            displayName8,
            displayName9,
            displayName10,
            displayName11,
            displayName12,
            displayName13,
            displayName14,
            displayName15,
            displayName16,
            displayName17,
            displayName18,
            displayName19,
            displayName20,
            displayName21,
            displayName22,
            displayName23,
            displayName24,
            displayName25,
            displayName26,
            displayName27,
            displayName28,
            displayName29,
            displayName30,
            displayName31,
            displayName32,
            displayName33
        )

        val methodNames = listOf(
            methodName1,
            methodName2,
            methodName3,
            methodName4,
            methodName5,
            methodName6,
            methodName7,
            methodName8,
            methodName9,
            methodName10,
            methodName11,
            methodName12,
            methodName13,
            methodName14,
            methodName15,
            methodName16,
            methodName17,
            methodName18,
            methodName19,
            methodName20,
            methodName21,
            methodName22,
            methodName23,
            methodName24,
            methodName25,
            methodName26,
            methodName27,
            methodName28,
            methodName29,
            methodName30,
            methodName31,
            methodName32,
            methodName33
        )

        val clusterInfo = listOf(
            Pair(UtClusterInfo("SYMBOLIC EXECUTION ENGINE: SUCCESSFUL EXECUTIONS for method sort(int[], int, int, int[], int, int)", null), 4),
            Pair(
                UtClusterInfo(
                    "SYMBOLIC EXECUTION ENGINE: ERROR SUITE for method sort(int[], int, int, int[], int, int)", null
                ), 29
            )
        )


        val method = ArraysQuickSort::sort
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames, clusterInfo)
    }
}
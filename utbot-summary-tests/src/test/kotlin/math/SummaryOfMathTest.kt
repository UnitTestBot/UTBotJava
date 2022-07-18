package math

import examples.SummaryTestCaseGeneratorTest
import guava.examples.math.Stats
import org.junit.jupiter.api.Test
import org.utbot.examples.DoNotCalculate
import org.utbot.framework.plugin.api.MockStrategyApi

/**
 * It runs test generation for the poor analogue of the Stats.of method ported from the guava-26.0 framework
 * and validates generated docs, display names and test method names.
 *
 * @see <a href="https://github.com/UnitTestBot/UTBotJava/issues/198">Related issue</a>
 */
class SummaryOfMathTest : SummaryTestCaseGeneratorTest(
    Stats::class,
) {
    @Test
    fun testOfInts() {
        val summary1 = "Test calls {@link guava.examples.math.StatsAccumulator#addAll(int[])},\n" +
                "    there it triggers recursion of addAll once, \n" +
                "Test throws NullPointerException in: acummulator.addAll(values);\n"
        val summary2 = "Test calls {@link guava.examples.math.StatsAccumulator#addAll(int[])},\n" +
                "    there it does not iterate for(int value: values), \n" +
                "Test later calls {@link guava.examples.math.StatsAccumulator#snapshot()},\n" +
                "    there it returns from: return new Stats(count, mean, sumOfSquaresOfDeltas, min, max);\n" +
                "    \n" +
                "Test then returns from: return acummulator.snapshot();"
        val summary3 = "Test calls {@link guava.examples.math.StatsAccumulator#addAll(int[])},\n" +
                "    there it iterates the loop for(int value: values) once. \n" +
                "Test later calls {@link guava.examples.math.StatsAccumulator#snapshot()},\n" +
                "    there it returns from: return new Stats(count, mean, sumOfSquaresOfDeltas, min, max);\n" +
                "    \n" +
                "Test then returns from: return acummulator.snapshot();"
        val summary4 = "Test calls {@link guava.examples.math.StatsAccumulator#addAll(int[])},\n" +
                "    there it iterates the loop for(int value: values) twice. \n" +
                "Test later calls {@link guava.examples.math.StatsAccumulator#snapshot()},\n" +
                "    there it returns from: return new Stats(count, mean, sumOfSquaresOfDeltas, min, max);\n" +
                "    \n" +
                "Test later returns from: return acummulator.snapshot();\n"

        val methodName1 = "testOfInts_StatsAccumulatorAddAll"
        val methodName2 = "testOfInts_snapshot"
        val methodName3 = "testOfInts_IterateForEachLoop"
        val methodName4 = "testOfInts_IterateForEachLoop_1"

        val displayName1 = "acummulator.addAll(values) : True -> ThrowNullPointerException"
        val displayName2 = "snapshot -> return new Stats(count, mean, sumOfSquaresOfDeltas, min, max)"
        val displayName3 = "addAll -> return new Stats(count, mean, sumOfSquaresOfDeltas, min, max)"
        val displayName4 = "addAll -> return new Stats(count, mean, sumOfSquaresOfDeltas, min, max)"

        val method = Stats::ofInts
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3,
            summary4
        )

        val displayNames = listOf(
            displayName1,
            displayName2,
            displayName3,
            displayName4
        )

        val methodNames = listOf(
            methodName1,
            methodName2,
            methodName3,
            methodName4
        )

        check(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testOfDoubles() {
        val summary1 = "Test calls {@link guava.examples.math.StatsAccumulator#addAll(double[])},\n" +
                "    there it triggers recursion of addAll once, \n" +
                "Test throws NullPointerException in: acummulator.addAll(values);\n"
        val summary2 = "Test calls {@link guava.examples.math.StatsAccumulator#addAll(double[])},\n" +
                "    there it does not iterate for(double value: values), \n" +
                "Test next calls {@link guava.examples.math.StatsAccumulator#snapshot()},\n" +
                "    there it returns from: return new Stats(count, mean, sumOfSquaresOfDeltas, min, max);\n" +
                "    \n" +
                "Test later returns from: return acummulator.snapshot();\n"
        val summary3 = "Test calls {@link guava.examples.math.StatsAccumulator#addAll(double[])},\n" +
                "    there it iterates the loop for(double value: values) twice,\n" +
                "        inside this loop, the test calls {@link guava.examples.math.StatsAccumulator#add(double)},\n" +
                "        there it executes conditions:\n" +
                "            (count == 0): True\n" +
                "            (!isFinite(value)): True\n" +
                "        calls {@link guava.examples.math.StatsAccumulator#add(double)},\n" +
                "        there it executes conditions:\n" +
                "            (count == 0): False\n" +
                "            (isFinite(value) && isFinite(mean)): True\n" +
                "            (if (isFinite(value) && isFinite(mean)) {\n" +
                "    double delta = value - mean;\n" +
                "    mean += delta / count;\n" +
                "    sumOfSquaresOfDeltas += delta * (value - mean);\n" +
                "} else {\n" +
                "    mean = calculateNewMeanNonFinite(mean, value);\n" +
                "    sumOfSquaresOfDeltas = NaN;\n" +
                "}): False\n" +
                "Test afterwards calls {@link guava.examples.math.StatsAccumulator#snapshot()},\n" +
                "    there it returns from: return new Stats(count, mean, sumOfSquaresOfDeltas, min, max);\n" +
                "    \n" +
                "Test afterwards returns from: return acummulator.snapshot();\n"
        val summary4 = "Test calls {@link guava.examples.math.StatsAccumulator#addAll(double[])},\n" +
                "    there it iterates the loop for(double value: values) twice,\n" +
                "        inside this loop, the test calls {@link guava.examples.math.StatsAccumulator#add(double)},\n" +
                "        there it executes conditions:\n" +
                "            (!isFinite(value)): False\n" +
                "Test next calls {@link guava.examples.math.StatsAccumulator#snapshot()},\n" +
                "    there it returns from: return new Stats(count, mean, sumOfSquaresOfDeltas, min, max);\n" +
                "    \n" +
                "Test then returns from: return acummulator.snapshot();\n"
        val summary5 = "Test calls {@link guava.examples.math.StatsAccumulator#addAll(double[])},\n" +
                "    there it iterates the loop for(double value: values) twice,\n" +
                "        inside this loop, the test calls {@link guava.examples.math.StatsAccumulator#add(double)},\n" +
                "        there it executes conditions:\n" +
                "            (count == 0): True\n" +
                "            (!isFinite(value)): False\n" +
                "        calls {@link guava.examples.math.StatsAccumulator#add(double)},\n" +
                "        there it executes conditions:\n" +
                "            (count == 0): False\n" +
                "            (isFinite(value) && isFinite(mean)): True\n" +
                "            (if (isFinite(value) && isFinite(mean)) {\n" +
                "    double delta = value - mean;\n" +
                "    mean += delta / count;\n" +
                "    sumOfSquaresOfDeltas += delta * (value - mean);\n" +
                "} else {\n" +
                "    mean = calculateNewMeanNonFinite(mean, value);\n" +
                "    sumOfSquaresOfDeltas = NaN;\n" +
                "}): True\n" +
                "Test later calls {@link guava.examples.math.StatsAccumulator#snapshot()},\n" +
                "    there it returns from: return new Stats(count, mean, sumOfSquaresOfDeltas, min, max);\n" +
                "    \n" +
                "Test then returns from: return acummulator.snapshot();\n"
        val summary6 = "Test calls {@link guava.examples.math.StatsAccumulator#addAll(double[])},\n" +
                "    there it iterates the loop for(double value: values) twice,\n" +
                "        inside this loop, the test calls StatsAccumulator::add,\n" +
                "        there it executes conditions:\n" +
                "            (!isFinite(value)): True\n" +
                "Test afterwards calls {@link guava.examples.math.StatsAccumulator#snapshot()},\n" +
                "    there it returns from: return new Stats(count, mean, sumOfSquaresOfDeltas, min, max);\n" +
                "    \n" +
                "Test then returns from: return acummulator.snapshot();\n"
        val summary7 = "Test calls {@link guava.examples.math.StatsAccumulator#addAll(double[])},\n" +
                "    there it iterates the loop for(double value: values) twice,\n" +
                "        inside this loop, the test calls {@link guava.examples.math.StatsAccumulator#add(double)},\n" +
                "        there it executes conditions:\n" +
                "            (!isFinite(value)): True\n" +
                "Test later calls {@link guava.examples.math.StatsAccumulator#snapshot()},\n" +
                "    there it returns from: return new Stats(count, mean, sumOfSquaresOfDeltas, min, max);\n" +
                "    \n" +
                "Test further returns from: return acummulator.snapshot();\n"

        val methodName1 = "testOfDoubles_StatsAccumulatorAddAll"
        val methodName2 = "testOfDoubles_snapshot"
        val methodName3 = "testOfDoubles_IsFiniteAndIsFinite"
        val methodName4 = "testOfDoubles_IsFinite"
        val methodName5 = "testOfDoubles_IsFiniteAndIsFinite_1"
        val methodName6 = "testOfDoubles_NotIsFinite"
        val methodName7 = "testOfDoubles_NotIsFinite_1"

        val displayName1 = "acummulator.addAll(values) : True -> ThrowNullPointerException"
        val displayName2 = "snapshot -> return new Stats(count, mean, sumOfSquaresOfDeltas, min, max)"
        val displayName3 = "!isFinite(value) : True -> StatsAccumulatorCalculateNewMeanNonFinite"
        val displayName4 = "add -> !isFinite(value) : False"
        val displayName5 = "!isFinite(value) : False -> isFinite(value) && isFinite(mean)"
        val displayName6 = "add -> !isFinite(value) : True"
        val displayName7 = "add -> !isFinite(value) : True"

        val method = Stats::ofDoubles
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3,
            summary4,
            summary5,
            summary6,
            summary7
        )

        val displayNames = listOf(
            displayName1,
            displayName2,
            displayName3,
            displayName4,
            displayName5,
            displayName6,
            displayName7
        )

        val methodNames = listOf(
            methodName1,
            methodName2,
            methodName3,
            methodName4,
            methodName5,
            methodName6,
            methodName7
        )

        check(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}
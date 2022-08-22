package org.utbot.features

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.eq
import org.utbot.examples.withFeaturePath
import java.io.File
import java.io.FileInputStream

class FeatureProcessorWithRepetitionTest : UtValueTestCaseChecker(OnePath::class, false) {
    companion object {
        const val featureDir = "src/test/resources/features"
        fun reward(coverage: Double, time: Double) = RewardEstimator.reward(coverage, time)

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            File(featureDir).mkdir()
            EngineAnalyticsContext.featureProcessorFactory = FeatureProcessorWithStatesRepetitionFactory()
            EngineAnalyticsContext.featureExtractorFactory = FeatureExtractorFactoryImpl()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            File(featureDir).deleteRecursively()
        }
    }

    @Test
    fun testCalculateRewards() {
        val statesToInt = mapOf(
            "a0" to 1,
            "c0" to 2,
            "f0" to 3,
            "g0" to 4,
            "c1" to 5,
            "f1" to 6,
            "g1" to 7,
            "b0" to 8,
            "d0" to 9
        )

        val expectedRewards: Map<String, Double> = mapOf(
            "a0" to reward(6.0, 15.0),
            "c0" to reward(4.0, 10.0),
            "f0" to reward(4.0, 8.0),
            "g0" to reward(4.0, 2.0),
            "c1" to reward(0.0, 4.0),
            "f1" to reward(0.0, 3.0),
            "g1" to reward(0.0, 2.0),
            "b0" to reward(2.0, 4.0),
            "d0" to reward(2.0, 2.0)
        )

        val rewardEstimator = RewardEstimator()
        val testCases = listOf(
            TestCase(
                listOf(
                    statesToInt["g0"]!! to 2L,
                    statesToInt["f0"]!! to 2L,
                    statesToInt["c0"]!! to 2L,
                    statesToInt["a0"]!! to 1L
                ),
                newCoverage = 4, testIndex = 0
            ),
            TestCase(
                listOf(
                    statesToInt["g1"]!! to 2L,
                    statesToInt["f1"]!! to 1L,
                    statesToInt["c1"]!! to 1L,
                    statesToInt["f0"]!! to 2L,
                    statesToInt["c0"]!! to 2L,
                    statesToInt["a0"]!! to 1L
                ),
                newCoverage = 0,
                testIndex = 1
            ),
            TestCase(
                listOf(statesToInt["d0"]!! to 2L, statesToInt["b0"]!! to 2L, statesToInt["a0"]!! to 1L),
                newCoverage = 2,
                testIndex = 2
            )
        )

        val rewards = rewardEstimator.calculateRewards(testCases)
        Assertions.assertEquals(9, rewards.size)
        expectedRewards.forEach {
            Assertions.assertEquals(it.value, rewards[statesToInt[it.key]])
        }
    }

    /**
     * Test, that we correctly add test cases and dump them into file
     */
    @Test
    fun addTestCaseTest() {
        withFeaturePath(featureDir) {
            check(
                OnePath::onePath,
                eq(1)
            )

            Assertions.assertTrue(FileInputStream("$featureDir/0.csv").bufferedReader().readLines().size > 1)
        }
    }
}

package org.utbot.summary.clustering.dbscan

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.utbot.summary.clustering.dbscan.neighbor.LinearRangeQuery
import java.lang.IllegalArgumentException
import kotlin.math.sqrt

internal class DBSCANTrainerTest {
    /** Helper test class for keeping ```(x, y)``` data. */
    data class Point(val x: Float, val y: Float)

    /** Helper [Metric] interface implementation, emulates the Euclidean distance. */
    class TestEuclideanMetric : Metric<Point> {
        override fun compute(object1: Point, object2: Point): Double {
            return sqrt((object2.y - object1.y) * (object2.y - object1.y) + (object2.x - object1.x) * (object2.x - object1.x)).toDouble();
        }
    }

    @Test
    fun emptyData() {
        val testData = arrayOf<Point>()

        val dbscan = DBSCANTrainer(
            eps = 0.3f,
            minSamples = 10,
            metric = TestEuclideanMetric(),
            rangeQuery = LinearRangeQuery()
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            dbscan.fit(testData)
        }

        assertEquals(
            "Nothing to learn, data is empty.",
            exception.message
        )
    }

    /**
     * Basic training on the synthetic data produced by the following Python script
     *
     * ```
     * import numpy as np
     *
     * from sklearn.cluster import DBSCAN
     * from sklearn.datasets import make_blobs
     * from sklearn.preprocessing import StandardScaler
     * centers = [[1, 1], [-1, -1], [1, -1]]
     * X, labels_true = make_blobs( n_samples=150, centers=centers, cluster_std=0.4, random_state=0)
     * X = StandardScaler().fit_transform(X)
     * ```
     */
    @Test
    fun fit() {
        val testData = arrayOf(
            Point(0.51306161f, 1.1471073f),
            Point(0.65512213f, -0.97066103f),
            Point(1.26449613f, 1.83734944f),
            Point(0.21216956f, -0.378767f),
            Point(-1.14479616f, -1.11145131f),
            Point(-1.58153887f, -0.08196208f),
            Point(0.68254979f, 1.1919578f),
            Point(0.8696672f, -0.64867363f),
            Point(0.61143818f, -0.24018834f),
            Point(1.00293973f, 0.97573626f),
            Point(-1.31881688f, -0.01560197f),
            Point(0.19938146f, -0.88057948f),
            Point(0.70288688f, -0.45600334f),
            Point(0.39380809f, -0.08454808f),
            Point(0.72528092f, 1.41221765f),
            Point(0.65361304f, 1.43176371f),
            Point(0.32385524f, 1.03936418f),
            Point(0.46518951f, 1.09421048f),
            Point(-0.9317319f, -0.55894622f),
            Point(0.96247469f, 1.31228971f),
            Point(1.39551198f, 0.88413591f),
            Point(-0.55513847f, -1.20821209f),
            Point(-0.13006728f, 0.12120668f),
            Point(0.34633163f, -1.25444427f),
            Point(-1.17539483f, -0.16636096f),
            Point(0.65798122f, -0.5354049f),
            Point(0.40147441f, 1.12480245f),
            Point(-1.08732589f, -0.74995774f),
            Point(1.02084117f, -0.5595343f),
            Point(0.83145875f, -0.41939857f),
            Point(0.25429041f, 0.71164368f),
            Point(0.82080917f, -1.76332956f),
            Point(0.54271592f, 1.28676704f),
            Point(-1.5439909f, -1.54936442f),
            Point(0.4647383f, 0.80490875f),
            Point(0.93527623f, -0.41244765f),
            Point(0.29053258f, -0.81791807f),
            Point(0.97237203f, -0.86484064f),
            Point(0.24560256f, 1.675701f),
            Point(-1.58357069f, -1.00510479f),
            Point(0.43127435f, -0.70360332f),
            Point(1.24950949f, -1.48959247f),
            Point(-1.47038338f, -0.67631311f),
            Point(0.78716138f, 0.93212787f),
            Point(-1.30748385f, -1.1382141f),
            Point(1.35500499f, 1.42078681f),
            Point(-1.79807073f, -0.57907958f),
            Point(0.84687941f, 0.66636195f),
            Point(1.12595818f, 1.19478593f),
            Point(-1.62915162f, 0.06104132f),
            Point(0.29503262f, -0.84287903f),
            Point(0.17436004f, 1.56779641f),
            Point(-1.78931547f, -0.30544452f),
            Point(0.40932172f, -0.83543907f),
            Point(0.73407798f, 1.10835044f),
            Point(-1.69686198f, -0.41757271f),
            Point(-1.02900758f, -0.52437524f),
            Point(-0.44552695f, -0.1624096f),
            Point(0.04515838f, -0.44531824f),
            Point(0.41639988f, 1.12356039f),
            Point(0.41883977f, -0.87053195f),
            Point(-1.06646137f, -0.76427654f),
            Point(-1.75121296f, 0.07411488f),
            Point(0.66875136f, 1.96066291f),
            Point(0.74615069f, 1.64538505f),
            Point(-1.4539805f, -0.9743326f),
            Point(0.83834828f, 1.39488498f),
            Point(1.14611708f, 1.73333403f),
            Point(0.02666318f, 1.44518563f),
            Point(0.61263928f, -0.79914282f),
            Point(-0.5612403f, -0.33012658f),
            Point(0.71430928f, 1.42150062f),
            Point(-0.8271744f, -0.55964167f),
            Point(1.11054723f, 0.78379483f),
            Point(0.20866016f, 1.61584836f),
            Point(-1.74117296f, -0.8536984f),
            Point(0.45219304f, -0.52102926f),
            Point(0.03304239f, 1.18200098f),
            Point(-1.46240807f, 0.03735307f),
            Point(-1.6835453f, -1.28496829f),
            Point(0.52848656f, 1.32579874f),
            Point(0.62424741f, 1.42485476f),
            Point(-0.92140293f, -0.7435152f),
            Point(0.72019561f, -0.80753388f),
            Point(-1.77168534f, -0.35415786f),
            Point(-0.99006985f, -0.36228449f),
            Point(1.43008949f, -0.53114204f),
            Point(-1.39699376f, -0.37048473f),
            Point(-0.33447176f, 1.51953577f),
            Point(-1.54094919f, -0.41958353f),
            Point(1.24707045f, 2.00352637f),
            Point(-1.05179021f, -0.32382983f),
            Point(0.80410635f, 1.54016696f),
            Point(0.77419081f, -0.72136257f),
            Point(0.48321364f, -0.49553707f),
            Point(-1.22688273f, -0.43571376f),
            Point(-0.35946552f, -0.31515231f),
            Point(-1.56393f, -0.74142087f),
            Point(-0.85120093f, -1.10386605f),
            Point(0.54370978f, -1.33609677f),
            Point(-1.80709156f, -0.86295711f),
            Point(-1.4306462f, -1.21880623f),
            Point(1.56628119f, -1.09610687f),
            Point(0.5429767f, -0.64517576f),
            Point(0.7210137f, 1.8314722f),
            Point(1.0476718f, 2.13794048f),
            Point(0.82209878f, 0.99808183f),
            Point(0.72589108f, -0.59266492f),
            Point(0.31720674f, 0.49316348f),
            Point(-0.95678938f, -0.93676362f),
            Point(0.38067925f, -1.22208381f),
            Point(0.50685865f, 1.74115147f),
            Point(0.62138202f, -0.28566211f),
            Point(0.31420085f, 1.41562276f),
            Point(1.24935081f, 1.18495494f),
            Point(-0.09312197f, -0.60957458f),
            Point(0.25558171f, -0.21125889f),
            Point(0.94997215f, 1.31513688f),
            Point(-0.92055416f, -0.64901292f),
            Point(0.34641694f, 0.59232248f),
            Point(-0.00310758f, 2.02491012f),
            Point(-1.33063994f, -0.94161521f),
            Point(-0.53956611f, -0.1063121f),
            Point(0.50831758f, -0.53894866f),
            Point(-1.64934396f, -0.2479317f),
            Point(1.54882393f, -0.69958647f),
            Point(-1.13713306f, -1.10898152f),
            Point(1.11560774f, -0.2625019f),
            Point(1.09499453f, -0.42783123f),
            Point(0.91515798f, -1.31309166f),
            Point(-1.04742583f, -1.30728723f),
            Point(0.93460287f, -0.17592166f),
            Point(0.10733517f, -0.87532123f),
            Point(0.69067372f, 1.38272846f),
            Point(-1.87571495f, -0.51193531f),
            Point(0.77670292f, -0.44591649f),
            Point(1.03645977f, 1.20591592f),
            Point(0.30957047f, 1.28512294f),
            Point(-1.60652529f, -0.95177271f),
            Point(-1.59341756f, -0.47303068f),
            Point(0.41518085f, -0.83790075f),
            Point(0.06165044f, -0.65847604f),
            Point(0.85786827f, -0.7283573f),
            Point(0.86856118f, -0.90745093f),
            Point(-1.55601094f, -0.67072178f),
            Point(-1.48701576f, 0.06862574f),
            Point(1.55291185f, 0.69826175f),
            Point(0.43088221f, -0.7758177f),
            Point(-1.7243115f, -0.66279942f),
            Point(0.52016266f, -0.77638553f)
        )

        val dbscan = DBSCANTrainer(
            eps = 0.3f,
            minSamples = 10,
            metric = TestEuclideanMetric(),
            rangeQuery = LinearRangeQuery()
        )

        val dbscanModel = dbscan.fit(testData)
        val clusterLabels = dbscanModel.clusterLabels

        assertEquals(150, clusterLabels.size)
        assertEquals(27, clusterLabels.count { it == 0 })
        assertEquals(35, clusterLabels.count { it == 1 })
        assertEquals(18, clusterLabels.count { it == 2 })
        assertEquals(70, clusterLabels.count { it == Int.MIN_VALUE })
    }
}
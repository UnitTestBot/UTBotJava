package org.utbot.analytics

import org.utbot.engine.pc.UtSolverStatusKind


interface UtBotAbstractPredictor<TIn, TOut> {
    /**
     * Initialization signal from controller
     * Predictor can load pre-calculated NN model from file/database, calibrate predictions for this machine
     */
    fun init() {}

    /**
     * Termination signal from controller.
     * Predictor can finalize it's in-memory state into file/database
     */
    fun terminate() {}

    /**
     * Provide information to Predictor: for special [input] you sent 1. Previously predicted (by asking [predict]] beforehand)
     * [expectedResult] and 2. [actualResult] as a result of calculation
     */
    fun provide(input: TIn, expectedResult: TOut, actualResult: TOut) {}

    /**
     *  Ask this predictor to predict by [input]
     */
    fun predict(input: TIn): TOut
}

/**
 * Predicts execution time of some request with input [TIn] in nanoseconds
 * @see Predictors.smt
 */
interface UtBotNanoTimePredictor<TIn> : UtBotAbstractPredictor<TIn, Long> {
    override fun predict(input: TIn) = 0L //Zero for default predictor
}

/**
 * Embrace [block()] inside this method to ask prediction before execution and send actual result after execution
 */
inline fun <TIn, T> UtBotNanoTimePredictor<TIn>.learnOn(input: TIn, block: () -> T): T {
    val expectedResultNano = predict(input)
    val startTimeNano = System.nanoTime()
    try {
        return block()
    } finally {
        provide(input, expectedResultNano, System.nanoTime() - startTimeNano)
    }
}

/**
 * Predicts sat/unsat state of some request with input [TIn]
 * @see Predictors.smt
 */
interface IUtBotSatPredictor<TIn> : UtBotAbstractPredictor<TIn, UtSolverStatusKind> {
    override fun predict(input: TIn) = UtSolverStatusKind.UNSAT //Zero for default predictor
}


/**
 * Embrace [block()] inside this method to ask prediction before execution and send actual result after execution
 */
fun <TIn> IUtBotSatPredictor<TIn>.learnOn(input: TIn, result: UtSolverStatusKind) {
    val expectedResult = predict(input)

    provide(input, expectedResult, result)
}
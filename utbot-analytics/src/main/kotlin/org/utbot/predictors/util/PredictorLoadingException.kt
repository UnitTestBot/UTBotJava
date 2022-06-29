package org.utbot.predictors.util

sealed class PredictorLoadingException(msg: String?, cause: Throwable? = null) : Exception(msg, cause)

class WeightsLoadingException(e: Throwable) : PredictorLoadingException("Error while loading weights", e)

class ModelLoadingException(e: Throwable) : PredictorLoadingException("Error while loading model", e)

class ScalerLoadingException(e: Throwable) : PredictorLoadingException("Error while loading scaler", e)

class ModelBuildingException(msg: String) : PredictorLoadingException(msg)

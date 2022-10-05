package org.utbot.analytics

import mu.KotlinLogging
import org.utbot.framework.PathSelectorType
import org.utbot.framework.UtSettings

private val logger = KotlinLogging.logger {}

object AnalyticsConfigureUtil {
    /**
     * Configures utbot-analytics models for the better path selection.
     *
     * NOTE: If analytics configuration for the NN Path Selector could not be loaded,
     * it switches to the [PathSelectorType.INHERITORS_SELECTOR].
     */
    fun configureML() {
        logger.info { "PathSelectorType: ${UtSettings.pathSelectorType}" }

        if (UtSettings.pathSelectorType == PathSelectorType.ML_SELECTOR) {
            val analyticsConfigurationClassPath = UtSettings.analyticsConfigurationClassPath
            tryToSetUpMLSelector(analyticsConfigurationClassPath)
        }

        if (UtSettings.pathSelectorType == PathSelectorType.TORCH_SELECTOR) {
            val analyticsConfigurationClassPath = UtSettings.analyticsTorchConfigurationClassPath
            tryToSetUpMLSelector(analyticsConfigurationClassPath)
        }
    }

    private fun tryToSetUpMLSelector(analyticsConfigurationClassPath: String) {
        try {
            Class.forName(analyticsConfigurationClassPath)
            Predictors.stateRewardPredictor = EngineAnalyticsContext.mlPredictorFactory()

            logger.info { "RewardModelPath: ${UtSettings.modelPath}" }
        } catch (e: ClassNotFoundException) {
            logger.error {
                "Configuration of the predictors from the utbot-analytics module described in the class: " +
                        "$analyticsConfigurationClassPath is not found!"
            }

            logger.info(e) {
                "Error while initialization of ${UtSettings.pathSelectorType}. Changing pathSelectorType on INHERITORS_SELECTOR"
            }
            UtSettings.pathSelectorType = PathSelectorType.INHERITORS_SELECTOR
        }
        catch (e: Exception) { // engine not found, for example
            logger.error { e.message }
            UtSettings.pathSelectorType = PathSelectorType.INHERITORS_SELECTOR
        }
    }

}
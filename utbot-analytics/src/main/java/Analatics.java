import org.utbot.analytics.EngineAnalyticsContext;
import org.utbot.predictors.StateRewardPredictorFactoryImpl;

public class Analatics {
    static {

    }

    public Analatics() {
        EngineAnalyticsContext.stateRewardPredictorFactory.put(1, new StateRewardPredictorFactoryImpl());
    }
}

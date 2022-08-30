To enable support of the `utbot-analytics-torch` models in `utbot-intellij` module the following steps should be made:

- change the row `api project(':utbot-analytics-torch')` to the `api project(':utbot-analytics-torch')` in the `build.gradle` file in the `utbot-intellij` module 
- change the `pathSelectorType` in the `UtSettings.kt` to the `PathSelectorType.TORCH_SELECTOR`
- don't forget the put the Torch model in the path ruled by the setting `modelPath` in the `UtSettings.kt`
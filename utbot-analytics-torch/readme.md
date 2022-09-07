To enable support of the `utbot-analytics-torch` models in `utbot-intellij` module the following steps should be made:

- change the row `api project(':utbot-analytics')` to the `api project(':utbot-analytics-torch')` in the `build.gradle` file in the `utbot-intellij` module and uncomment it, if it's commented.
- change the `pathSelectorType` in the `UtSettings.kt` to the `PathSelectorType.TORCH_SELECTOR`
- don't forget the put the Torch model in the path ruled by the setting `modelPath` in the `UtSettings.kt`

NOTE: for Windows you could obtain the error message related to the "engine not found problem" from DJL library during the Torch model initialization.
The proposed solution from DJL authors includes the installation of the [Microsoft Visual C++ Redistributable.](https://docs.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist?view=msvc-170)

But at this moment it doesn't work on Windows at all.
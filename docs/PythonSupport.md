# UnitTestBot Python

[UnitTestBot](https://www.utbot.org/) is the tool for automated unit test generation available as an IntelliJ IDEA plugin, or a command-line interface.

Now UnitTestBot provides fuzzing-based support for Python.

## Requirements

1. IntelliJ IDEA — for compatibility, see [UnitTestBot on JetBrains Marketplace](https://plugins.jetbrains.com/plugin/19445-unittestbot/versions).
2. Python 3.8 or later
3. [Python plugin](https://plugins.jetbrains.com/plugin/631-python) for IntelliJ IDEA

If you already have a Python project, you usually have no need to install or configure anything else, but if you 
have trouble with launching UnitTestBot for Python code, please refer to [advanced requirements section](../utbot-python/docs/CLI.md#requirements).

## How to install and use

To try UnitTestBot Python in your IntelliJ IDEA:
1. To install the plugin, please refer to [UnitTestBot user guide](https://github.com/UnitTestBot/UTBotJava/wiki/Install-or-update-plugin).
2. Configure the Python interpreter for your project and make sure IntelliJ IDEA resolves all the imports.
3. In your IntelliJ IDEA, go to **File** > **Settings** > **Tools**, choose **UnitTestBot** and enable **Experimental languages support**.

    **(!) NOTE:** be sure to enable this option for **_each_** project.

4. To generate tests, place the caret at the required function and press **Alt+Shift+U**. To find the appropriate shortcut for the OS you are using, check the context menu.

To use UnitTestBot Python via command-line interface, please refer to the [CLI guide](../utbot-python/docs/CLI.md).

## How to contribute and get support

To get information on contributing and getting support, please see [UnitTestBot Java Readme](https://github.com/UnitTestBot/UTBotJava#readme).

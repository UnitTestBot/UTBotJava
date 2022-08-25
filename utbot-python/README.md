# UTBot for Python

UTBot is the tool for automated unit test generation. You can read more about this project [on the official website](https://www.utbot.org/).

This is the support of UTBot for Python.

UTBot tries to maximize the code coverage while minimizing the number of tests. For now, we use only the fuzzing technique for Python.

## Get started

There are two ways to use UTBot: as an IntelliJ IDEA plugin or through a command line interface.

You can download both archives [here](https://github.com/UnitTestBot/UTBotJava/actions/runs/2924526925).

### IntelliJ IDEA plugin

IntelliJ IDEA version should be 2022.1.

Make sure you already have the Python plugin installed.

Download the archive with the plugin and install it following [this instruction](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk).

Configure the Python interpreter for your project and make sure that IDEA resolves all imports.

After indexing has finished, move the cursor to a function, press ALT+SHIFT+U (or ALT+U, ALT+T in Ubuntu), and generate tests.

### Command line interface

You can find documentation on CLI usage [here](docs/CLI.md).

## Contribute

Read more in [UTBot Java Readme](../README.md#contribute-to-utbot-java).

## Support

Read more in [UTBot Java Readme](../README.md#find-support).

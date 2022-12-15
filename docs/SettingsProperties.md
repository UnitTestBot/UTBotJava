# UnitTestBot settings

First, let's define "**settings**" as the set of "**properties**". 
Each property is a _key=value_ pair, and it may be represented as source code or plain text stored in a file.
This property set affects the key aspects of UnitTestBot behavior.

UnitTestBot is available as
- an IntelliJ IDEA plugin,
- a continuous integration (CI) tool,
- a command-line interface (CLI).

These three application types have low-level _**core**_ settings. The plugin also has per-project _**plugin-specific**_ settings.

## Core settings

Core settings are persisted in the **_settings file_**: `{userHome}/.utbot/settings.properties`. This file is designed for reading only.

The defaults for the core settings are provided in source code (namely in `UtSettings.kt`) so the file itself may be absent or exist with a few of the customized properties only. For example, a file with just one line like `utBotGenerationTimeoutInMillis=15000` is valid and useful.

##  Plugin-specific settings

IDE persists the plugin-specific settings automatically (per project!) in the **plugin configuration file**: `{projectDir}/.idea/utbot-settings.xml`. Nobody is expected to edit this file manually.

At the moment, the core and plugin-specific settings have very small intersection (i.e. the keys of different levels control the same behavior aspects). 
If they still intersect, the core settings should provide the defaults for the plugin-specific settings. 
As for now, this concept is partially implemented.

## Property categories

A developer usually represents the new feature settings as a subset of properties and has to choose the proper "level" for them. In practice, we have these property categories:

- **Hardcoded directly as constants in source code**   
_One can build the plugin with their own hardcoded preset._
- **Experimental or temporary**  
_These properties can disappear from the settings file or jump back to the hardcoded constants. We do not expect the end user to change these properties, but it is still possible to specify them in the settings file._
- **Engine-level tuning with reasonable defaults**  
_Designed for low-level tuning during contests, etc._
- **Rarely used, good to be changed once per PC** 
- **Project-level setup in IDE**  
_The end user can change them via **File** > **Settings** > **Tools** > **UnitTestBot**_.
- **Small set of per-generation options**

Thereby, some properties can be considered as public API while the rest are pretty "internal".

The end user has three places to change UnitTestBot behavior:
1. Settings file, which is PC-wide — read by all the UnitTestBot instances across PC. 
For example, CLI and two different projects in IDE will re-use it.
2. Plugin settings UI (**File** > **Settings** > **Tools** > **UnitTestBot**).
3. Controls in the **Generate Tests with UnitTestBot window** dialog.

Properties from the plugin settings UI and the dialog are plugin-specific, and they are automatically persisted in `{projectDir}/.idea/utbot-settings.xml`. _Note:_ only non-default values are stored here.

## Configuring UnitTestBot with auto-generated `settings.properties`

Common users usually change UnitTestBot settings via UI:
* in the **Generate Tests with UnitTestBot** dialog,
* via **File** > **Settings** > **Tools** > **UnitTestBot**.

Advanced users and contributors require advanced settings.

### How to configure advanced settings: motivation to improve

Advanced settings were not visible in UnitTestBot UI and were configurable only via `settings.properties`.
UnitTestBot did not provide this file by default, so you had to create it manually in your `{home}/.utbot` directory.
You could configure advanced settings here if you knew available options — they are listed in UnitTestBot source code,
namely, `UtSettings.kt`. As UnitTestBot is a developing product, it often gets new features and new settings
that UnitTestBot users sometimes are not aware of.

### Implemented `settings.properties` improvements

Currently, UnitTestBot generates a template `settings.properties` file with the up-to-date list of available setting
options, corresponding default values, and explicit descriptions for each option.

This template file is auto-generated on the basis of `UtSettings.kt` doc comments. The template file consists of
the commented lines, so you can uncomment the line to enable the setting or easily get back to defaults.

_Idea to be implemented: we can annotate properties in UtSettings.kt as @api to provide the template file with a narrow subset of properties._

Generating `settings.properties` is a part of a Gradle task in IntelliJ IDEA. The `settings.properties` file is
bundled with the published UnitTestBot plugin as a top-level entry inside the `utbot-intellij-{version}.jar` file.

Upon IntelliJ IDEA start, the UnitTestBot plugin loads its settings and checks whether the template setting file exists
in the local file system as `{home}/.utbot/settings.properties`:
* If there is no such file, it is created (along with the hidden `{home}/.utbot` directory if needed).
* An existing file is updated with new settings and corresponding info if necessary.
* UnitTestBot does not re-write `settings.properties` if the file exists and has already been customized.
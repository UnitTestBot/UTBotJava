# UnitTestBot Settings

First, let's define the term "**settings**" as set of "**properties**" of the form '_key=value_' that 
may be represented as source code and as plain text stored in a file.
Altogether this set affects key aspects of behavior of UnitTestBot.

UnitTestBot works as following applications:
- IntelliJ plugin
- Continuous Integration tool (CI)
- Command Line Interface (CLI)

For all three types there are low-level _**core**_ settings but for plugin
we also have per-project _**plugin-specific**_ settings. 

## Core settings
Core settings are being persisted in file located in **_settings file_** `{userHome}/.utbot/settings.properties`  
This file is designed for reading only. 
All defaults for core settings are provided in source code (namely in `UtSettings.kt`) so the file itself may absent or exist with a few of customized properties only, 
for example file with just one line like `utBotGenerationTimeoutInMillis=15000` is valid and useful.

##  Plugin-specific settings
Plugin-specific settings are being persisted automatically by IDE (per-project!) 
in **plugin configuration file** `{projectDir}/.idea/utbot-settings.xml` and nobody is expected to edit this file manually.

At the moment, these two kinds of settings (core and plugin-specific) 
have very small intersection. 
In case of properties intersection Core settings should act as provider of defaults for plugin-specific settings. 
As for now, this concept is partially implemented.

## Categories of properties
Every newly added feature tends to be represented in settings as subset of properties,
and the question is the choice of proper "level". There are several categories of properties we have in practice: 
- **Hardcoded directly as constants in sourcecode**   
_It means one can build own build of plugin with different hardcoded preset._
- **Experimental or temporary**  
_Can disappear from the settings file or jump back to hardcoded constants. We do not expect these properties to be tweaked by end-user but still it's possible to specify them in settings file._
- **Engine-level tuning with reasonable defaults**  
_Designed for low-level tuning during contests etc._
- **Rarely used, good to be tweaked once per PC** 
- **Project-level setup in IDE**  
_End-user can tweak them in **File** > **Settings** > **Tools** > **UnitTestBot**_  
- **Small set of per-generation options**  
Thereby, some properties can be considered as public API while the rest is pretty "internal".

For user there are three places to tweak UnitTestBot behavior:
1. Settings file is PC-wide level to be read by all UnitTestBot instances across PC. 
For example, CLI and two different projects in IDE will re-use it.
2. Plugin settings UI (**File** > **Settings** > **Tools** > **UnitTestBot**)
3. Controls in "Generate" dialog
   Settings in 2 and 3 are plugin-specific and they are automatically persisted in {projectDir}/.idea/utbot-settings.xml
   (Note, only non-default values are stored here)

## Configuring UnitTestBot with auto-generated `settings.properties`

Common users usually change UnitTestBot settings via UI:
* in the **Generate Tests with UnitTestBot** dialog,
* through **File** > **Settings** > **Tools** > **UnitTestBot**.

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

_Idea to be implemented: we can annotate properties in UtSettings.kt as @api to provide the template file with narrow subset of properties._

Generating `settings.properties` is a part of a Gradle task in IntelliJ IDEA. The `settings.properties` file is
bundled with the published UnitTestBot plugin as a top-level entry inside the `utbot-intellij-{version}.jar` file.

Upon IntelliJ IDEA start, the UnitTestBot plugin loads its settings and checks whether the template setting file exists
in the local file system as `{home}/.utbot/settings.properties`:
* If there is no such file, it is created (along with the hidden `{home}/.utbot` directory if needed).
* An existing file is updated with new settings and corresponding info if necessary.
* UnitTestBot does not re-write `settings.properties` if the file exists and has already been customized.
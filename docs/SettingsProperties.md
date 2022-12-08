# Configuring UnitTestBot with auto-generated `settings.properties`

Common users usually change UnitTestBot settings via UI:
* in the **Generate Tests with UnitTestBot** dialog,
* through **File** > **Settings** > **Tools** > **UnitTestBot**.

Advanced users and contributors require advanced settings.

## How to configure advanced settings: motivation to improve

Advanced settings were not visible in UnitTestBot UI and were configurable only via `settings.properties`. 
UnitTestBot did not provide this file by default, so you had to create it manually in your `{home}/.utbot` directory. 
You could configure advanced settings here if you knew available options — they are listed in UnitTestBot source code, 
namely, `UtSettings.kt`. As UnitTestBot is a developing product, it often gets new features and new settings 
that UnitTestBot users sometimes are not aware of. 

## Implemented `settings.properties` improvements 

Currently, UnitTestBot generates a template `settings.properties` file with the up-to-date list of available setting 
options, corresponding default values, and explicit descriptions for each option.

This template file is auto-generated on the basis of `UtSettings.kt` doc comments. It consists of 
the commented lines, so you can uncomment the line to enable the setting or easily get back to defaults.

Generating `settings.properties` is a part of a Gradle task in IntelliJ IDEA. The `settings.properties` file is 
bundled with the published UnitTestBot plugin as a top-level entry inside the `utbot-intellij-{version}.jar` file.

Upon IntelliJ IDEA start, the UnitTestBot plugin loads its settings and checks whether the template setting file exists 
in the local file system as `{home}/.utbot/settings.properties`:
* If there is no such file, it is created (along with the hidden `{home}/.utbot` directory if needed).
* An existing file is updated with new settings and corresponding info if necessary.
* UnitTestBot does not re-write `settings.properties` if the file exists and has already been customized.
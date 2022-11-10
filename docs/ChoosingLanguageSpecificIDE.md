# Choosing language specific IDE

Some language-specific modules depends on specific IntelliJ IDE:
* Python can work with IntelliJ Community, IntelliJ Ultimate, PyCharm Community, PyCharm Professional
* JavaScript can work with IntelliJ Ultimate, PyCharm Professional and WebStorm
* Java and Kotlin - IntelliJ Community and IntelliJ Ultimate

You should select correct IDE in `gradle.properties` file:
```
ideType=<IU>
ideVersion=<222.4167.29>
```

### IDE marking

| Mark | Full name            | Supported plugin                       |
|------|----------------------|----------------------------------------|
| IC   | IntelliJ Community   | JVM, Python, AndroidStudio             |
| IU   | IntelliJ Ultimate    | JVM, Python, JavaScript, AndroidStudio |
| PC   | PyCharm Community    | Python                                 |
| PY   | PyCharm Professional | Python, JavaScript                     |

[IntelliJ Platform Plugin SDK documentation](https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#tasks-runpluginverifier)
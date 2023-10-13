# UTBot Python packages managements

Current UTBot Python packages:

- `utbot_mypy_runner`: https://pypi.org/project/utbot-mypy-runner/
- `utbot_executor`: https://pypi.org/project/utbot-executor/

To be able to publish new releases on pypi, ask @tochilinak ot @tamarinvs19 to give you permissions.

## Gradle tasks

To use Gradle tasks for Python packages, add the following properties in `gradle.properties` in your `GRADLE_USER_HOME` directory (about: https://docs.gradle.org/current/userguide/directory_layout.html#dir:gradle_user_home):

- `pythonInterpreter` (for example, `python3`)
- `pypiToken`(about: https://pypi.org/help/#apitoken)

## utbot_mypy_runner

### Version

Write version in file `utbot-python-types/src/main/resources/utbot_mypy_runner_version`.

Gradle task `utbot-python-types:setVersion` will update `pyproject.toml`.

### Usage of local version

Add the following files locally (they are listed in `.gitignore`):

- `utbot-python/src/main/resources/local_pip_setup/local_utbot_mypy_path`

    Add here absolute path to `utbot_mypy_runner/dist` directory.


- `utbot-python/src/main/resources/local_pip_setup/use_local_python_packages`

    Write here `true` if you want to build `utbot-python` that uses local versions of UTBot Python packages.
 
## utbot_executor

### Version

Write version in file `utbot-python-executor/src/main/resources/utbot_executor_version`.

Gradle task `utbot-python-executor:setVersion` will update `pyproject.toml`.

### Usage of local version

Add the following files locally (they are listed in `.gitignore`):

- `utbot-python/src/main/resources/local_pip_setup/local_utbot_executor_path`

  Add here absolute path to `utbot_executor/dist` directory.


- `utbot-python/src/main/resources/local_pip_setup/use_local_python_packages`

  Write here `true` if you want to build `utbot-python` that uses local versions of UTBot Python packages.

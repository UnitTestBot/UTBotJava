# How to setup environment for experiments on Linux

* Clone repository, go to root
* Maybe you will need `chmod +x ./scripts/*` and `chmod +x gradlew`.
* Set `Java 8` as default and set `JAVA_HOME` to this `Java`.
  For example
  * Go through [this](https://sdkman.io/install) until `Windows installation`
  * `sdk list java`
  * Find any `Java 8`
  * `sdk install <this java>`
  * `sdk use <this java>`
  * Check `java -version`
* `mkdir -p eval/features`
* `mkdir models`
* Set environment for `Python`.
  For example
  * `python3 -m venv /path/to/new/virtual/environment`
  * `source /path/to/venv/bin/activate`
  * Check `which python3`, it should be somewhere in `path/to/env` folder.
  * `pip install -r scripts/requirements.txt`
* `./scripts/prepare.sh`
* Change `scripts/prog_list` to run on smaller project or delete some classes from `contest_input/classes/<project>/list`.
* Also you can reduce number of models in `models` variable in `scripts/train_iteratively.sh`
* `scripts/train_iteratively.sh 30 2 models <your python3 command>`
* In `models/` you should get models.
* `mkdir eval/jacoco`
* `./scripts/run_with_coverage.sh <any project (guava-26.0, for example)> 30 "NN_REWARD_GUIDED_SELECTOR path/to/model" some_alias`. `path/to/model` should be something like `models/nn32/0`.
* You should get jacoco report in `eval/jacoco/guava-26.0/some_alias/`
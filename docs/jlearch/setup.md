# How to setup environment for experiments on Linux

* Clone repository, go to root
* `chmod +x ./scripts/ml/*` and `chmod +x gradlew`.
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
  * `pip install -r scripts/ml/requirements.txt`
* `./scripts/ml/prepare.sh`
* Change `scripts/ml/prog_list` to run on smaller project or delete some classes from `contest_input/classes/<project>/list`.

# Default settings and how to change it
* You can reduce number of models in `models` variable in `scripts/ml/train_iteratively.sh`
* You can change amount of required RAM in `run_contest_estimator.sh`: `16 gb`  by default
* You can change `batch_size` or `device` n `train.py`: `4096` and `gpu` by default
* If you are completing setup on server, then you will need to uncomment tmp directory option in `run_contest_estimator.sh`

# Continue setup
* `scripts/ml/train_iteratively.sh 30 2 models <your python3 command>`
* In `models/` you should get models.
* `mkdir eval/jacoco`
* `./scripts/ml/run_with_coverage.sh <any project (guava-26.0, for example)> 30 "NN_REWARD_GUIDED_SELECTOR path/to/model" some_alias`. `path/to/model` should be something like `models/nn32/0`, where `nn32` is a type of model and `0` is the iteration number
* You should get jacoco report in `eval/jacoco/guava-26.0/some_alias/`
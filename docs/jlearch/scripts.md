# How to use scripts
For each scenario: go to root of `UTBotJava` repository - it is `WORKDIR`.

`PATH_SELECTOR` as argument is `"PATH_SELECTOR_TYPE [PATH_SELECTOR_PATH for NN] [IS_COMBINED (false by default)] [ITERATIONS]"`.

Before start of work run:
```bash
./scripts/ml/prepare.sh
```

It will copy contest resources in `contest_input` folder and build the project, because we use jars, so if you want to change something in code and re-run scripts, then you should run:
```bash
./gradlew clean build -x test
```

## To Train a few iterations of your models:
By default features directory is `eval/features` - it should be created, to change it you should manually do it in source code of scripts.

List projects and selectors on what you want to train in `scripts/ml/prog_list` and `scripts/selector_list`. You will be trained on all methods of all classes from `contest_input/classes/<project name>/list`.

Then just run:
```bash
./scripts/ml/train_iteratively.sh <time_limit> <iterations> <output_dir> <python_command>
```
Python command is your command for python3, in the end of execution you will get iterations models in `<output_dir>` folder and features for each selector and project in `<features_dir>/<selector>/<project>` for `selector` from `selectors_list` and in `<features_dir>/jlearch/<selector>/<prog>` for models.

## To Run Contest Estimator with coverage:
Check that `srcTestDir` with your project exist in `build.gradle` of `utbot-junit-contest`. If it is not then add `build/output/test/<project>`.

Then just run: 
```bash
./scripts/ml/run_with_coverage.sh <project> <time_limit> <path_selector> <selector_alias>
``` 

In the end of execution you will get jacoco report in `eval/jacoco/<project>/<selector_alias>/` folder.

## To estimate quality
Just run:
```bash
./scripts/ml/quality_analysis.sh <project> <selector_aliases, separated by comma>
```
It will take coverage reports from relative report folders (at `eval/jacoco/project/alias`) and generate charts in `$outputDir/<project>/<timestamp>.html`.
`outputDir` can be changed in `QualityAnalysisConfig`. Result file will contain information about 3 metrics:
* $\frac{\sum_{c \in classSet} instCoverage(c)}{|classSet|}$
* $\frac{\sum_{c \in classSet} coveredInstructions(c)}{\sum_{c \in classSet} allInstructions(c)}$
* $\frac{\sum_{c \in classSet} branchCoverage(c)}{|classSet|}$

For each metric for each selector you will have:
* value of metric
* some chart with median, $q_1$, $q_3$ and so on


## To scrap solution classes from codeforces
Note: You can't scrap many classes, because codeforces api has a request limit.

It can be useful, if you want to train Jlearch on classes usually without virtual functions, but with many algorithms, so cycles and conditions.

Just run:
```bash
python3 path/to/codeforces_scrapper.py --problem_count <val> --submission_count <val> --min_rating <val> --max_rating <val> --output_dir <val>
```

All arguments are optional. Default values: `100`, `10`, `0`, `1500`, `.`.

At the end you should get `submission_count` classes for each of `problem_count` problems with rating between `min_rating` and `max_rating` at `output_dir`. Each class have package `p<contest_id>.p<submission_id>`.
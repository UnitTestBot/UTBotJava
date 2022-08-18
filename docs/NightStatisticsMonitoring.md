# Night Statistics Monitoring

## What is the problem?

As UnitTestBot contributors, we'd like to constantly improve our product. There are many of us introducing code changes simultaneously — unfortunately, some changes or combinations of them may lead to reduced plugin efficiency. To avoid such an unlucky result we need to monitor statistics on test generation performance.

## Why monitor nightly?

It would be great to collect statistics as soon as the contributor changes the code. In case you have a huge project it takes too long to run the monitoring system after each push into master.
Thus, we decided to do it every night when (hopefully!) no one makes changes.

## How do we collect statistics?

To find the algorithm you can refer to `StatisticsMonitoring.kt`. Shortly speaking, it is based on `ContestEstimator.kt`, which runs test generation on the sample projects and then compile the resulting tests. We repeat the whole process several times to reduce measurement error.

## Statistics monitoring usage

### Collecting statistics

To run statistics monitoring you have to specify the name of the JSON output file.

Input arguments: `<output json>`.

Output format: you get the JSON file, which contains an array of objects with statistics on each run.

More about each statistic: `Statistics.kt`.

More about monitoring settings: `MonitoringSettings.kt`.

Input example:

```
stats.json
```

Output example (the result of three runs during one night):

```json
[
  {
    "classes_for_generation": 20,
    "testcases_generated": 1651,
    "classes_without_problems": 12,
    "classes_canceled_by_timeout": 2,
    "total_methods_for_generation": 519,
    "methods_with_at_least_one_testcase_generated": 365,
    "methods_with_exceptions": 46,
    "suspicious_methods": 85,
    "test_classes_failed_to_compile": 0,
    "covered_instructions_count": 5753,
    "covered_instructions_count_by_fuzzing": 4375,
    "covered_instructions_count_by_concolic": 4069,
    "total_instructions_count": 10182,
    "avg_coverage": 62.885408034613
  },
  {
    "classes_for_generation": 20,
    "testcases_generated": 1872,
    "classes_without_problems": 12,
    "classes_canceled_by_timeout": 2,
    "total_methods_for_generation": 519,
    "methods_with_at_least_one_testcase_generated": 413,
    "methods_with_exceptions": 46,
    "suspicious_methods": 38,
    "test_classes_failed_to_compile": 0,
    "covered_instructions_count": 6291,
    "covered_instructions_count_by_fuzzing": 4470,
    "covered_instructions_count_by_concolic": 5232,
    "total_instructions_count": 11011,
    "avg_coverage": 62.966064315865275
  },
  {
    "classes_for_generation": 20,
    "testcases_generated": 1770,
    "classes_without_problems": 13,
    "classes_canceled_by_timeout": 2,
    "total_methods_for_generation": 519,
    "methods_with_at_least_one_testcase_generated": 405,
    "methods_with_exceptions": 44,
    "suspicious_methods": 43,
    "test_classes_failed_to_compile": 0,
    "covered_instructions_count": 6266,
    "covered_instructions_count_by_fuzzing": 4543,
    "covered_instructions_count_by_concolic": 5041,
    "total_instructions_count": 11011,
    "avg_coverage": 61.59069193429194
  }
]
```

### Metadata and summarising

To get rid of measurement errors and get a general understanding of UnitTestBot efficiency we average the statistics over the runs during one night.

Our main goal is to find code changes or run conditions related to the reduced UnitTestBot performance. Thus, we collect metadata about each run: the commit hash, the UnitTestBot build number, and also information about the environment (including JDK and build system versions, and other parameters).

The `insert_metadata.py` script is responsible for doing this. To run it you have to specify the following arguments.

Input arguments: `<stats file> <output file> <commit hash> <build number>`.

Please notice, that the `<output file>` must look like:

`*-<timestamp>-<commit hash>.json`


Output format: you get the JSON file, containing summarised statistics and metadata.

Input example:
```
stats.json data/data-main-2022-08-17-1660740407-66a1aeb6.json 66a1aeb6 2022.8
```

Output example (an average for each statistic over the three runs followed by metadata):
```json
{
  "classes_for_generation": 20.0,
  "testcases_generated": 1764.3333333333333,
  "classes_without_problems": 12.333333333333334,
  "classes_canceled_by_timeout": 2.0,
  "total_methods_for_generation": 519.0,
  "methods_with_at_least_one_testcase_generated": 394.3333333333333,
  "methods_with_exceptions": 45.333333333333336,
  "suspicious_methods": 55.333333333333336,
  "test_classes_failed_to_compile": 0.0,
  "avg_coverage": 62.480721428256736,
  "total_coverage": 56.84739152087949,
  "total_coverage_by_fuzzing": 41.60749728061026,
  "total_coverage_by_concolic": 44.420096905766805,
  "metadata": {
    "commit_hash": "66a1aeb6",
    "build_number": "2022.8",
    "environment": {
      "host": "host",
      "OS": "Windows version 10.0.19043",
      "java_version": "openjdk version \"1.8.0_322\"\r\nOpenJDK Runtime Environment Corretto-8.322.06.1 (build 1.8.0_322-b06)\r\nOpenJDK 64-Bit Server VM Corretto-8.322.06.1 (build 25.322-b06, mixed mode)\r\n",
      "gradle_version": "Gradle 7.4",
      "JAVA_HOME": "D:\\Java\\jdk",
      "KOTLIN_HOME": "D:\\Kotlin\\kotlinc",
      "PATH": "D:\\gradle-7.4\\bin;D:\\Java\\jre\\bin;"
    }
  }
}
```

### Aggregating

The `build_aggregated_data.py` script gathers the results for several nights. The summarised results for each of the nights are put together into one array. You can specify the period for aggregating. It is useful for visualising or finding statistical characteristics of UnitTestBot performance, e.g. the median or max/min values.

To run aggregating you should provide the input.

Input arguments: `<input data dir> <output file> <timestamp from> <timestamp to>`.

Please notice that the `<input data dir>` must contain the files which look like `*-<timestamp>-<commit hash>.json`. You (probably) have already named them properly during summarisation.

Output format: you get the JSON file, which contains an array of summarised results for each of the nights during the specified period.

Input example:

```
./data aggregated_data.json 0 1660740407
```

Output example:

(You'll get an array of several summarised outputs without metadata. The following example is just one element of such an array.)

```json
[
    {
        "classes_for_generation": 20.0,
        "testcases_generated": 1764.3333333333333,
        "classes_without_problems": 12.333333333333334,
        "classes_canceled_by_timeout": 2.0,
        "total_methods_for_generation": 519.0,
        "methods_with_at_least_one_testcase_generated": 394.3333333333333,
        "methods_with_exceptions": 45.333333333333336,
        "suspicious_methods": 55.333333333333336,
        "test_classes_failed_to_compile": 0.0,
        "avg_coverage": 62.480721428256736,
        "total_coverage": 56.84739152087949,
        "total_coverage_by_fuzzing": 41.60749728061026,
        "total_coverage_by_concolic": 44.420096905766805,
        "timestamp": 1660740407
    }
]
```

### Datastorage structure

We store the collected statistics in our repository. You can find two special branches: `monitoring-data` and `monitoring-aggregated-data`.

The `monitoring-data` branch is a storage for raw statistics data as well as metadata.

The filename format: `data-<branch>-<yyyy>-<mm>-<dd>-<timestamp>-<short commit hash>.json`

The `monitoring-aggregated-data` branch is a storage for aggregated statistics. The aggregating period is set to one month by default.

The filename format: `aggregated-data-<yyyy>-<mm>-<dd>.json`

### Grafana (in process)

We can use [Grafana](https://monitoring.utbot.org) for more dynamic and detailed statistics visualisation. Grafana pulls data from our repository automatically by means of GitHub API.

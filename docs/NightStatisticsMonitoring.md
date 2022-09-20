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

Output format: you get the JSON file, which contains an array of objects with statistics and input parameters on each run.

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
    "parameters": {
      "target": "guava",
      "class_timeout_sec": 20,
      "run_timeout_min": 20
    },
    "metrics": {
      "duration_ms": 604225,
      "classes_for_generation": 20,
      "testcases_generated": 1651,
      "classes_without_problems": 12,
      "classes_canceled_by_timeout": 2,
      "total_methods_for_generation": 519,
      "methods_with_at_least_one_testcase_generated": 365,
      "methods_with_exceptions": 46,
      "suspicious_methods": 85,
      "test_classes_failed_to_compile": 0,
      "covered_instructions": 5753,
      "covered_instructions_by_fuzzing": 4375,
      "covered_instructions_by_concolic": 4069,
      "total_instructions": 10182,
      "avg_coverage": 62.885408034613
    }
  },
  {
    "parameters": {
      "target": "guava",
      "class_timeout_sec": 20,
      "run_timeout_min": 20
    },
    "metrics": {
      "duration_ms": 633713,
      "classes_for_generation": 20,
      "testcases_generated": 1872,
      "classes_without_problems": 12,
      "classes_canceled_by_timeout": 2,
      "total_methods_for_generation": 519,
      "methods_with_at_least_one_testcase_generated": 413,
      "methods_with_exceptions": 46,
      "suspicious_methods": 38,
      "test_classes_failed_to_compile": 0,
      "covered_instructions": 6291,
      "covered_instructions_by_fuzzing": 4470,
      "covered_instructions_by_concolic": 5232,
      "total_instructions": 11011,
      "avg_coverage": 62.966064315865275
    }
  },
  {
    "parameters": {
      "target": "guava",
      "class_timeout_sec": 20,
      "run_timeout_min": 20
    },
    "metrics": {
      "duration_ms": 660421,
      "classes_for_generation": 20,
      "testcases_generated": 1770,
      "classes_without_problems": 13,
      "classes_canceled_by_timeout": 2,
      "total_methods_for_generation": 519,
      "methods_with_at_least_one_testcase_generated": 405,
      "methods_with_exceptions": 44,
      "suspicious_methods": 43,
      "test_classes_failed_to_compile": 0,
      "covered_instructions": 6266,
      "covered_instructions_by_fuzzing": 4543,
      "covered_instructions_by_concolic": 5041,
      "total_instructions": 11011,
      "avg_coverage": 61.59069193429194
    }
  }
]
```

### Metadata

Our main goal is to find code changes or run conditions related to the reduced UnitTestBot performance. Thus, we collect metadata about each run: the commit hash, the UnitTestBot build number, and also information about the environment (including JDK and build system versions, and other parameters).

The `insert_metadata.py` script is responsible for doing this. To run it you have to specify the following arguments.

To get more information about input arguments call script with option `--help`.

Output format: you get the JSON file, containing statistics and parameters grouped by target project and metadata.

Input example:
```
--stats_file stats.json --output_file data/meta-stats.json
--commit 66a1aeb6 --branch main
--build 2022.8 --timestamp 1661330445 
--source_type github-action --source_id 2917672580
```

Output example (statistics followed by metadata):
```json
{
  "version": 1,
  "targets": [
    {
      "id": "guava",
      "version": "0",
      "parameters": [
        {
          "class_timeout_sec": 20,
          "run_timeout_min": 20
        },
        {
          "class_timeout_sec": 20,
          "run_timeout_min": 20
        },
        {
          "class_timeout_sec": 20,
          "run_timeout_min": 20
        }
      ],
      "metrics": [
        {
          "duration_ms": 604225,
          "classes_for_generation": 20,
          "testcases_generated": 1651,
          "classes_without_problems": 12,
          "classes_canceled_by_timeout": 2,
          "total_methods_for_generation": 519,
          "methods_with_at_least_one_testcase_generated": 365,
          "methods_with_exceptions": 46,
          "suspicious_methods": 85,
          "test_classes_failed_to_compile": 0,
          "covered_instructions": 5753,
          "covered_instructions_by_fuzzing": 4375,
          "covered_instructions_by_concolic": 4069,
          "total_instructions": 10182,
          "avg_coverage": 62.885408034613
        },
        {
          "duration_ms": 633713,
          "classes_for_generation": 20,
          "testcases_generated": 1872,
          "classes_without_problems": 12,
          "classes_canceled_by_timeout": 2,
          "total_methods_for_generation": 519,
          "methods_with_at_least_one_testcase_generated": 413,
          "methods_with_exceptions": 46,
          "suspicious_methods": 38,
          "test_classes_failed_to_compile": 0,
          "covered_instructions": 6291,
          "covered_instructions_by_fuzzing": 4470,
          "covered_instructions_by_concolic": 5232,
          "total_instructions": 11011,
          "avg_coverage": 62.966064315865275
        },
        {
          "duration_ms": 660421,
          "classes_for_generation": 20,
          "testcases_generated": 1770,
          "classes_without_problems": 13,
          "classes_canceled_by_timeout": 2,
          "total_methods_for_generation": 519,
          "methods_with_at_least_one_testcase_generated": 405,
          "methods_with_exceptions": 44,
          "suspicious_methods": 43,
          "test_classes_failed_to_compile": 0,
          "covered_instructions": 6266,
          "covered_instructions_by_fuzzing": 4543,
          "covered_instructions_by_concolic": 5041,
          "total_instructions": 11011,
          "avg_coverage": 61.59069193429194
        }
      ]
    }
  ],
  "metadata": {
    "source": {
      "type": "github-action",
      "id": "2917672580"
    },
    "commit_hash": "66a1aeb6",
    "branch": "main",
    "build_number": "2022.8",
    "timestamp": 1661330445,
    "date": "2022-08-24T08:40:45",
    "environment": {
      "host": "fv-az183-700",
      "OS": "Linux version #20~20.04.1-Ubuntu SMP Fri Aug 5 12:16:53 UTC 2022",
      "java_version": "openjdk version \"11.0.16\" 2022-07-19 LTS\nOpenJDK Runtime Environment Zulu11.58+15-CA (build 11.0.16+8-LTS)\nOpenJDK 64-Bit Server VM Zulu11.58+15-CA (build 11.0.16+8-LTS, mixed mode)\n",
      "gradle_version": "Gradle 7.4.2",
      "JAVA_HOME": "/opt/hostedtoolcache/Java_Zulu_jdk+fx/11.0.16-8/x64",
      "KOTLIN_HOME": "/usr",
      "PATH": "/opt/hostedtoolcache/Python/3.9.13/x64/bin:/opt/hostedtoolcache/Python/3.9.13/x64:/home/runner/gradle-installations/installs/gradle-7.4.2/bin:/opt/hostedtoolcache/Java_Zulu_jdk+fx/11.0.16-8/x64/bin:/home/linuxbrew/.linuxbrew/bin:/home/linuxbrew/.linuxbrew/sbin:/home/runner/.local/bin:/opt/pipx_bin:/home/runner/.cargo/bin:/home/runner/.config/composer/vendor/bin:/usr/local/.ghcup/bin:/home/runner/.dotnet/tools:/snap/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/snap/bin"
    }
  }
}
```

### Aggregating

The `build_aggregated_data.py` script gathers the results for several nights. The collected results for each of the nights are put together into one array. You can specify the period for aggregating. It is useful for visualising or finding statistical characteristics of UnitTestBot performance, e.g. the median or max/min values.

To run aggregating you should provide the input.

To get more information about input arguments call script with option `--help`.

Output format: you get the JSON file, which contains arrays of grouped by target results for each of the nights during the specified period.

Input example:

```
--input_data_dir ./data --output_file aggregated_data.json 
--timestamp_from 0 --timestamp_to 1661330445
```

Output example (You'll get an array of several outputs without metadata):
```json
[
  {
    "id": "guava",
    "version": "0",
    "parameters": [
      {
        "class_timeout_sec": 20,
        "run_timeout_min": 20,
        "timestamp": 1661330445
      },
      {
        "class_timeout_sec": 20,
        "run_timeout_min": 20,
        "timestamp": 1661330445
      },
      {
        "class_timeout_sec": 20,
        "run_timeout_min": 20,
        "timestamp": 1661330445
      }
    ],
    "metrics": [
      {
        "duration_ms": 604225,
        "classes_for_generation": 20,
        "testcases_generated": 1651,
        "classes_without_problems": 12,
        "classes_canceled_by_timeout": 2,
        "total_methods_for_generation": 519,
        "methods_with_at_least_one_testcase_generated": 365,
        "methods_with_exceptions": 46,
        "suspicious_methods": 85,
        "test_classes_failed_to_compile": 0,
        "avg_coverage": 62.885408034613,
        "total_coverage": 56.50166961304262,
        "total_coverage_by_fuzzing": 42.967982714594385,
        "total_coverage_by_concolic": 39.96267923787075
      },
      {
        "duration_ms": 633713,
        "classes_for_generation": 20,
        "testcases_generated": 1872,
        "classes_without_problems": 12,
        "classes_canceled_by_timeout": 2,
        "total_methods_for_generation": 519,
        "methods_with_at_least_one_testcase_generated": 413,
        "methods_with_exceptions": 46,
        "suspicious_methods": 38,
        "test_classes_failed_to_compile": 0,
        "avg_coverage": 62.966064315865275,
        "total_coverage": 57.133775315593496,
        "total_coverage_by_fuzzing": 40.595767868495145,
        "total_coverage_by_concolic": 47.51612024339297
      },
      {
        "duration_ms": 660421,
        "classes_for_generation": 20,
        "testcases_generated": 1770,
        "classes_without_problems": 13,
        "classes_canceled_by_timeout": 2,
        "total_methods_for_generation": 519,
        "methods_with_at_least_one_testcase_generated": 405,
        "methods_with_exceptions": 44,
        "suspicious_methods": 43,
        "test_classes_failed_to_compile": 0,
        "avg_coverage": 61.59069193429194,
        "total_coverage": 56.90672963400236,
        "total_coverage_by_fuzzing": 41.25874125874126,
        "total_coverage_by_concolic": 45.78149123603669
      }
    ]
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

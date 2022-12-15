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

```json5
[
  {
    "parameters": {
      "fuzzing_ratio": 0.1, // how long does fuzzing takes
      "class_timeout_sec": 20, // test generation timeout for one class
      "run_timeout_min": 20 // total timeout for this run
    },
    "targets": [ // projects that have been processed
      {
        "target": "guava", // name of project
        "summarised_metrics": { // summarised metrics for processed project 
          "total_classes": 20, // classes count
          "testcases_generated": 1042, // generated unit-tests count
          "classes_failed_to_compile": 0, // classes that's tests are not compilable
          "classes_canceled_by_timeout": 4, // classes that's generation was canceled because of timeout
          "total_methods": 526, // methods count
          "methods_with_at_least_one_testcase_generated": 345, // methods with at least one successfully generated test
          "methods_with_at_least_one_exception": 32, // methods that's generation contains exceptions
          "methods_without_any_tests_and_exceptions": 59, // suspicious methods without any tests and exceptions
          "covered_bytecode_instructions": 4240, // amount of bytecode instructions that were covered by generated tests
          "covered_bytecode_instructions_by_fuzzing": 2946, // amount of bytecode instructions that were covered by fuzzing's generated tests
          "covered_bytecode_instructions_by_concolic": 3464, // amount of bytecode instructions that were covered by concolic's generated tests
          "total_bytecode_instructions": 9531, // total amount of bytecode instructions in methods with at least one testcase generated 
          "averaged_bytecode_instruction_coverage_by_classes": 0.5315060991492891 // mean bytecode coverage by class
        },
        "metrics_by_class": [ // metrics for all classes in this project
          {
            "class_name": "com.google.common.math.LongMath", // name of processed class
            "metrics": { // metrics for specified class
              "testcases_generated": 91, // amount of generated unit-tests 
              "failed_to_compile": false, // compilation generated tests are failure 
              "canceled_by_timeout": true, // generation is interrupted because of timeout
              "total_methods_in_class": 31, // methods count in this class
              "methods_with_at_least_one_testcase_generated": 26, // methods with at least one successfully generated test
              "methods_with_at_least_one_exception": 0, // methods that's generation contains exceptions
              "methods_without_any_tests_and_exceptions": 5, // suspicious methods without any tests and exceptions
              "covered_bytecode_instructions_in_class": 585, // amount of bytecode instructions that were covered by generated tests
              "covered_bytecode_instructions_in_class_by_fuzzing": 489, // amount of bytecode instructions that were covered by fuzzing's generated tests
              "covered_bytecode_instructions_in_class_by_concolic": 376, // amount of bytecode instructions that were covered by concolic's generated tests
              "total_bytecode_instructions_in_class": 1442 // total amount of bytecode instructions in methods with at least one testcase generated 
            }
          },
          // ...
        ]
      }
    ]
  }
]
```

### Metadata

Our main goal is to find code changes or run conditions related to the reduced UnitTestBot performance. Thus, we collect metadata about each run: the commit hash, the UnitTestBot build number, and also information about the environment (including JDK and build system versions, and other parameters).

The `insert_metadata.py` script is responsible for doing this. To run it you have to specify the following arguments.

To get more information about input arguments call script with option `--help`.

Output format: you get the JSON file, containing metadata, statistics and parameters grouped by target project and classes.

Input example:
```
--stats_file stats.json --output_file data/meta-stats.json
--commit 66a1aeb6 --branch main
--build 2022.8 --timestamp 1661330445 
--source_type github-action --source_id 2917672580
```

Output example (statistics followed by metadata):
```json5
{
  "version": 2, // version of json format
  "targets": [ // projects and methods that have been processed
    {
      "target": "guava", // name of project
      "summarised": [ // list of summarised metrics with parameters on each run
        {
          "parameters": {
            "fuzzing_ratio": 0.1, // how long does fuzzing takes
            "class_timeout_sec": 20, // test generation timeout for one class
            "run_timeout_min": 20 // total timeout for this run
          },
          "metrics": {
            "total_classes": 20, // classes count
            "testcases_generated": 1042, // generated unit-tests count
            "classes_failed_to_compile": 0, // classes that's tests are not compilable
            "classes_canceled_by_timeout": 4, // classes that's generation was canceled because of timeout
            "total_methods": 526, // methods count
            "methods_with_at_least_one_testcase_generated": 345, // methods with at least one successfully generated test
            "methods_with_at_least_one_exception": 32, // methods that's generation contains exceptions
            "methods_without_any_tests_and_exceptions": 59, // suspicious methods without any tests and exceptions
            "total_bytecode_instruction_coverage": 0.44486412758, // total bytecode coverage of generated tests
            "total_bytecode_instruction_coverage_by_fuzzing": 0.30909663204, // total bytecode coverage of fuzzing's generated tests
            "total_bytecode_instruction_coverage_by_concolic": 0.36344559857, // total bytecode coverage of concolic's generated tests
            "averaged_bytecode_instruction_coverage_by_classes": 0.5315060991492891 // mean bytecode coverage by class
          }
        },
        // ...
      ],
      "by_class": [ // list of metrics and parameters for all classes in project and on each run
        {
          "class_name": "com.google.common.math.LongMath", // name of processed class
          "data": [ // metrics and parameters on each run
            {
              "parameters": { // parameters on this run
                "fuzzing_ratio": 0.1, // how long does fuzzing takes
                "class_timeout_sec": 20, // test generation timeout for one class
                "run_timeout_min": 20 // total timeout for this run
              },
              "metrics": { // metrics for specified class on this run
                "testcases_generated": 91, // amount of generated unit-tests 
                "failed_to_compile": false, // compilation generated tests are failure 
                "canceled_by_timeout": true, // generation is interrupted because of timeout
                "total_methods_in_class": 31, // methods count in this class
                "methods_with_at_least_one_testcase_generated": 26, // methods with at least one successfully generated test
                "methods_with_at_least_one_exception": 0, // methods that's generation contains exceptions
                "methods_without_any_tests_and_exceptions": 5, // suspicious methods without any tests and exceptions
                "total_bytecode_instruction_coverage_in_class": 0.40568654646, // bytecode coverage of generated tests
                "total_bytecode_instruction_coverage_in_class_by_fuzzing": 0.33911234396, // bytecode coverage of fuzzing's generated tests
                "total_bytecode_instruction_coverage_in_class_by_concolic": 0.26074895977 // bytecode coverage of concolic's generated tests
              }
            },
            // ...
          ]
        },
        // ...
      ]
    },
    // ...
  ],
  "metadata": { // device's properties
    "source": { // information about runner
      "type": "github-action",
      "id": "2917672580"
    },
    "commit_hash": "66a1aeb6", // commit hash of used utbot build 
    "branch": "main", // branch of used utbot build
    "build_number": "2022.8", // build number of used utbot build
    "timestamp": 1661330445, // run timestamp
    "date": "2022-08-24T08:40:45", // human-readable run timestamp
    "environment": { // device's environment
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

### Datastorage structure

We store the collected statistics in our repository. You can find two special branches: `monitoring-data` and `monitoring-aggregated-data`.

The `monitoring-data` branch is a storage for raw statistics data as well as metadata.

The filename format: `<yyyy>-<mm>-<dd>-<hh>-<MM>-<ss>-<branch>-<short commit hash>-<project name>-<runner number>.json`

### Grafana

#### Usage

We can use [Grafana](https://monitoring.utbot.org) for more dynamic and detailed statistics visualisation. Grafana pulls data from our repository automatically.

#### Metrics format

Our goal after collecting statistics is uploading results into grafana. For this we should prepare data and send it to our server.

The `prepare_metrics.py` script is responsible for doing this. To run it you have to specify the following arguments.

To get more information about input arguments call script with option `--help`.

Output format: you get the JSON file, containing array of metrics with some labels.

Output example:
```json5
[
  // summarised
  {
    "metric": "testcases_generated",
    "labels": {
      "project": "guava",
      "fuzzing_ratio": 0.1
    },
    "value": 1024
  },
  // ...
  // by classes
  {
    "metric": "testcases_generated",
    "labels": {
      "project": "guava",
      "class": "com.google.common.math.LongMath",
      "fuzzing_ratio": 0.1
    },
    "value": 91
  },
  // ...
]
```

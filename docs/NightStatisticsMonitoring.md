# Night Statistics Monitoring

## The problem
We want to develop and improve our product and, of course, 
there are some changes and its combinations 
which, according to some statistics, can make UTBot worse.

## Monitoring
The main idea is collecting statistics after made changes.
But it takes too long to collect statistics on a huge project
to do it after each push into master. 
Thus, we will do it every night when no one makes changes.

### Collecting statistics
Collecting statistics StatisticsMonitoring.kt based on ContestEstimator.kt 
that runs testcase generation on projects, then compile generated tests.
We run it several times. Input arguments: `<output json>`.

More about statistic: Statistics.kt.

More about monitoring settings: MonitoringSettings.kt

Example input:
```
stats.json
```
Example output:
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

### Transforming, aggregating and rendering statistics
Transforming adds total coverage statistics and timestamp.
After that all collected statistics are aggregated by average function.
Then history updates by aggregated statistics and rendered into 2 pictures:
- coverage graph - graph with coverage statistics.
- quantitative graph - graph with other quantitative statistics.


Script: draw_stats_graphs.py.
Input arguments: `<history file> <new stats file> <output coverage graph> <output quantitative graph>`.

Example input:
```
monitoring/history.json stats.json monitoring/coverage_graph.png monitoring/quantitative_graph.png
```
Example output:
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
        "timestamp": 1660202621883
    }
]
```

### Grafana
Also, we can use [Grafana](https://monitoring.utbot.org) for more dynamic and detailed statistics visualization.
Grafana pulls data from our repository automatically by GitHub API.

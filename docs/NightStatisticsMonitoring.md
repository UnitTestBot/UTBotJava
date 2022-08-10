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
We run it several times. Input arguments: `<output json> <run tries> <run timeout min>`.
More about statistic: Statistics.kt.

Example input:
```
stats.json 2 20
```
Example output:
```json
[
  {
    "classes_for_generation": 20,
    "testcases_generated": 958,
    "classes_without_problems": 12,
    "classes_canceled_by_timeout": 3,
    "total_methods_for_generation": 519,
    "methods_with_at_least_one_testcase_generated": 314,
    "methods_with_exceptions": 47,
    "suspicious_methods": 63,
    "test_classes_failed_to_compile": 1,
    "covered_instructions_count": 4388,
    "covered_instructions_count_by_fuzzing": 3651,
    "covered_instructions_count_by_concolic": 2178,
    "total_instructions_count": 9531,
    "avg_coverage": 60.10571074242921
  },
  {
    "classes_for_generation": 9,
    "testcases_generated": 557,
    "classes_without_problems": 5,
    "classes_canceled_by_timeout": 2,
    "total_methods_for_generation": 114,
    "methods_with_at_least_one_testcase_generated": 109,
    "methods_with_exceptions": 11,
    "suspicious_methods": 1,
    "test_classes_failed_to_compile": 0,
    "covered_instructions_count": 1675,
    "covered_instructions_count_by_fuzzing": 1276,
    "covered_instructions_count_by_concolic": 1332,
    "total_instructions_count": 2407,
    "avg_coverage": 70.9287503305422
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
    "classes_for_generation": 14.5,
    "testcases_generated": 757.5,
    "classes_without_problems": 8.5,
    "classes_canceled_by_timeout": 2.5,
    "total_methods_for_generation": 316.5,
    "methods_with_at_least_one_testcase_generated": 211.5,
    "methods_with_exceptions": 29.0,
    "suspicious_methods": 32.0,
    "test_classes_failed_to_compile": 0.5,
    "avg_coverage": 65.5172305364857,
    "total_coverage": 57.813969999804286,
    "total_coverage_by_fuzzing": 45.65931336298925,
    "total_coverage_by_concolic": 39.095171346713414,
    "timestamp": 1660132400587
  }
]
```

### Grafana
Also, we can use [Grafana](https://monitoring.utbot.org) for more dynamic and detailed statistics visualization.
Grafana pulls data from our repository automatically by GitHub API.

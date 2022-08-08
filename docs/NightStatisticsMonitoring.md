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
stats.json 3 20
```
Example output:
```json
[
	{
		"classes_for_generation": 20,
		"tc_generated": 1204,
		"classes_without_problems": 12,
		"classes_canceled_by_timeout": 3,
		"total_methods_for_generation": 519,
		"methods_with_at_least_one_testcase_generated": 332,
		"methods_with_exceptions": 42,
		"suspicious_methods": 107,
		"test_classes_failed_to_compile": 0,
		"covered_instructions_count": 5282,
		"total_instructions_count": 10932,
		"avg_coverage": 57.43687433585721
	},
	{
		"classes_for_generation": 20,
		"tc_generated": 1692,
		"classes_without_problems": 12,
		"classes_canceled_by_timeout": 2,
		"total_methods_for_generation": 519,
		"methods_with_at_least_one_testcase_generated": 426,
		"methods_with_exceptions": 49,
		"suspicious_methods": 29,
		"test_classes_failed_to_compile": 1,
		"covered_instructions_count": 6499,
		"total_instructions_count": 11023,
		"avg_coverage": 66.33821560285908
	},
	{
		"classes_for_generation": 20,
		"tc_generated": 1406,
		"classes_without_problems": 12,
		"classes_canceled_by_timeout": 3,
		"total_methods_for_generation": 519,
		"methods_with_at_least_one_testcase_generated": 394,
		"methods_with_exceptions": 43,
		"suspicious_methods": 61,
		"test_classes_failed_to_compile": 0,
		"covered_instructions_count": 5851,
		"total_instructions_count": 11011,
		"avg_coverage": 60.71679400185094
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
        "tc_generated": 1434.0,
        "classes_without_problems": 12.0,
        "classes_canceled_by_timeout": 2.6666666666666665,
        "total_methods_for_generation": 519.0,
        "methods_with_at_least_one_testcase_generated": 384.0,
        "methods_with_exceptions": 44.666666666666664,
        "suspicious_methods": 65.66666666666667,
        "test_classes_failed_to_compile": 0.33333333333333337,
        "avg_coverage": 61.49729464685574,
        "total_coverage": 53.47106015409328,
        "timestamp": 1659715928.5049753
    }
]
```
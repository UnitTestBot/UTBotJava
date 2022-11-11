import sys
import json

with open(sys.argv[1]) as metrics_raw:
    metrics_json = json.load(metrics_raw)

# metrics is a json list e.g.:
#     [
#         {
#             "metric": "total_classes",
#             "labels": {
#                 "project": "guava",
#                 "fuzzing_ratio": 0.1
#             },
#             "value": 20
#         },
#         {
#             "metric": "testcases_generated",
#             "labels": {
#                 "project": "guava",
#                 "fuzzing_ratio": 0.1
#             },
#             "value": 1042
#         }
#     ]
#
# the loop below iterates over each list item and constructs metrics set
metrics_set_str = ""
for metric in metrics_json:
    labels_set_str = ""
    comma = ""
    for label, value in metric['labels'].items():
        labels_set_str = f'{labels_set_str}{comma}{label}=\"{value}\"'
        comma = ","
    metrics_set_str += f'{metric["metric"]}{{{labels_set_str}}} {metric["value"]}\n'
print(metrics_set_str)

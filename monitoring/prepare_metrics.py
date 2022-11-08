import argparse
import json
from typing import List

from utils import load


def build_metric_struct(name: str, value: any, labels: dict) -> dict:
    return {
        "metric": name,
        "labels": labels,
        "value": value
    }


def build_metrics_from_data(data: dict, labels: dict) -> List[dict]:
    result = []
    fuzzing_ratio = data["parameters"]["fuzzing_ratio"]
    new_labels = {
        **labels,
        "fuzzing_ratio": fuzzing_ratio
    }
    metrics = data["metrics"]
    for metric in metrics:
        result.append(build_metric_struct(metric, metrics[metric], new_labels))
    return result


def build_metrics_from_data_array(metrics: List[dict], labels: dict) -> List[dict]:
    result = []
    for metric in metrics:
        result.extend(build_metrics_from_data(metric, labels))
    return result


def build_metrics_from_target(target: dict) -> List[dict]:
    result = []
    project = target["target"]

    result.extend(build_metrics_from_data_array(
        target["summarised"],
        {
            "project": project
        }
    ))

    for class_item in target["by_class"]:
        class_name = class_item["class_name"]
        result.extend(build_metrics_from_data_array(
            class_item["data"],
            {
                "project": project,
                "class": class_name
            }
        ))

    return result


def build_metrics_from_targets(targets: List[dict]) -> List[dict]:
    metrics = []
    for target in targets:
        metrics.extend(build_metrics_from_target(target))
    return metrics


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--stats_file', required=True,
        help='files with statistics after insertion metadata', type=str
    )
    parser.add_argument(
        '--output_file', required=True,
        help='output file', type=str
    )

    args = parser.parse_args()
    return args


def main():
    args = get_args()
    stats = load(args.stats_file)
    metrics = build_metrics_from_targets(stats["targets"])
    with open(args.output_file, "w") as f:
        json.dump(metrics, f, indent=4)


if __name__ == "__main__":
    main()

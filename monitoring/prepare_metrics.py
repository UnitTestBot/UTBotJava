import argparse
import json
from typing import List

from utils import load


def remove_in_class(name: str) -> str:
    in_class = "_in_class"
    idx = name.find(in_class)
    if idx == -1:
        return name
    return name[:idx] + name[idx:].removeprefix(in_class)


def update_from_counter_name(key_word: str, name: str, labels: dict) -> str:
    if name == f"total_{key_word}":
        labels["type"] = "total"
        return key_word
    if name.startswith(key_word):
        labels["type"] = name.removeprefix(f"{key_word}_")
        return key_word
    return name


def update_from_coverage(name: str, labels: dict) -> str:
    coverage_key = "bytecode_instruction_coverage"
    idx = name.find(coverage_key)
    if idx == -1:
        return name
    labels["type"] = name[:idx - 1]
    source = name[idx:].removeprefix(f"{coverage_key}")
    if len(source) > 0:
        source = source.removeprefix("_by_")
        if source == "classes":
            labels["type"] = "averaged_by_classes"
        else:
            labels["source"] = source
    return coverage_key


def build_metric_struct(name: str, value: any, labels: dict) -> dict:
    name = remove_in_class(name)
    name = update_from_counter_name("classes", name, labels)
    name = update_from_counter_name("methods", name, labels)
    name = update_from_coverage(name, labels)

    name = f"utbot_{name}"
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
        result.append(build_metric_struct(metric, metrics[metric], new_labels.copy()))
    return result


def build_metrics_from_data_array(metrics: List[dict], labels: dict) -> List[dict]:
    result = []
    for metric in metrics:
        result.extend(build_metrics_from_data(metric, labels))
    return result


def build_metrics_from_target(target: dict, runner: str) -> List[dict]:
    result = []
    project = target["target"]

    result.extend(build_metrics_from_data_array(
        target["summarised"],
        {
            "runner": runner,
            "project": project
        }
    ))

    for class_item in target["by_class"]:
        class_name = class_item["class_name"]
        result.extend(build_metrics_from_data_array(
            class_item["data"],
            {
                "runner": runner,
                "project": project,
                "class": class_name
            }
        ))

    return result


def build_metrics_from_targets(targets: List[dict], runner: str) -> List[dict]:
    metrics = []
    for target in targets:
        metrics.extend(build_metrics_from_target(target, runner))
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
    runner = stats["metadata"]["environment"]["host"]
    metrics = build_metrics_from_targets(stats["targets"], runner)
    metrics.sort(key=lambda x: x["metric"])
    with open(args.output_file, "w") as f:
        json.dump(metrics, f, indent=4)


if __name__ == "__main__":
    main()

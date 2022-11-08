import argparse
import json
import re
import subprocess
from collections import OrderedDict
from datetime import datetime
from os import environ
from platform import uname
from time import time
from typing import Optional, List

from monitoring_settings import JSON_VERSION
from utils import load


def try_get_output(args: str) -> Optional[str]:
    """
    Try to run subprocess with specified arguments
    :param args: arguments for execution
    :return: result output of execution or None
    """
    try:
        return subprocess.check_output(args, stderr=subprocess.STDOUT, shell=True).decode()
    except Exception as e:
        print(f'Error in command "{args}":\n\t{e}')
        return None


def parse_gradle_version(s: str) -> Optional[str]:
    """
    Parse gradle version from given string
    :param s: execution result of gradle --version
    :return: parsed gradle version or None
    """
    if s is None:
        return None
    regex = re.compile(r'^\s*(Gradle [.\d]+)\s*$', re.MULTILINE)
    result = regex.search(s)
    if result is None:
        return None
    return result.group(1)


def build_environment_data() -> dict:
    """
    Collect environment data from host
    :return: dictionary with environment data
    """
    uname_result = uname()
    environment = {
        'host': uname_result.node,
        'OS': f'{uname_result.system} version {uname_result.version}',
        'java_version': try_get_output('java -version'),
        'gradle_version': parse_gradle_version(try_get_output('gradle --version')),
        'JAVA_HOME': environ.get('JAVA_HOME'),
        'KOTLIN_HOME': environ.get('KOTLIN_HOME'),
        'PATH': environ.get('PATH'),
    }
    return environment


def build_metadata(args: argparse.Namespace) -> dict:
    """
    Collect metadata into dictionary
    :param args: parsed program arguments
    :return: dictionary with metadata
    """
    metadata = {
        'source': {
            'type': args.source_type,
            'id': args.source_id
        },
        'commit_hash': args.commit,
        'branch': args.branch,
        'build_number': args.build,
        'timestamp': args.timestamp,
        'date': datetime.fromtimestamp(args.timestamp).strftime('%Y-%m-%dT%H:%M:%S'),
        'environment': build_environment_data()
    }
    return metadata


def build_target(target_name: str) -> dict:
    return {
        "target": target_name,
        "summarised": [],
        "by_class": OrderedDict()
    }


def transform_metrics(metrics: dict) -> dict:
    """
    Transform given metrics with calculation coverage
    :param metrics: given metrics
    :return: transformed metrics
    """
    result = OrderedDict()

    instr_count_prefix = "covered_bytecode_instructions"
    total_instr_count_prefix = "total_bytecode_instructions"

    coverage_prefix = "total_bytecode_instruction_coverage"

    total_count = 0
    for metric in metrics:
        if metric.startswith(total_instr_count_prefix):
            total_count = metrics[metric]
            break

    for metric in metrics:
        if metric.startswith(total_instr_count_prefix):
            continue
        if metric.startswith(instr_count_prefix):
            coverage = metrics[metric] / total_count if total_count > 0 else 0.0
            result[coverage_prefix + metric.removeprefix(instr_count_prefix)] = coverage
        else:
            result[metric] = metrics[metric]

    return result


def build_data(parameters: dict, metrics: dict) -> dict:
    return {
        "parameters": {
            **parameters
        },
        "metrics": {
            **transform_metrics(metrics)
        }
    }


def build_by_class(class_name: str) -> dict:
    return {
        "class_name": class_name,
        "data": []
    }


def update_from_class(by_class: dict, class_item: dict, parameters: dict):
    """
    Update class object using given class_item
    :param by_class: dictionary with classname keys
    :param class_item: class metrics of current run
    :param parameters: parameters of current run
    """
    class_name = class_item["class_name"]
    if class_name not in by_class:
        by_class[class_name] = build_by_class(class_name)

    metrics = class_item["metrics"]
    by_class[class_name]["data"].append(
        build_data(parameters, metrics)
    )


def update_from_target(targets: dict, target_item: dict, parameters: dict):
    """
    Update targets using given target_item
    :param targets: dictionary with target keys
    :param target_item: metrics of current run
    :param parameters: parameters of current run
    """
    target_name = target_item["target"]
    if target_name not in targets:
        targets[target_name] = build_target(target_name)

    summarised_metrics = target_item["summarised_metrics"]
    targets[target_name]["summarised"].append(
        build_data(parameters, summarised_metrics)
    )

    for class_item in target_item["metrics_by_class"]:
        update_from_class(targets[target_name]["by_class"], class_item, parameters)


def update_from_stats(targets: dict, stats: dict):
    """
    Updates targets using given statistics
    :param targets: dictionary with target keys
    :param stats: target object
    """
    parameters = stats["parameters"]
    for target_item in stats["targets"]:
        update_from_target(targets, target_item, parameters)


def postprocess_by_class(by_class: dict) -> List[dict]:
    """
    Transform dictionary with classname keys into array with class objects
    :param by_class: dictionary with classname keys
    :return: array of class objects
    """
    return list(by_class.values())


def postprocess_targets(targets: dict) -> List[dict]:
    """
    Transform dictionary with target keys into array with target objects
    :param targets: dictionary with target keys
    :return: array of targets
    """
    result = []
    for target in targets.values():
        target["by_class"] = postprocess_by_class(target["by_class"])
        result.append(target)
    return result


def build_targets(stats_array: List[dict]) -> List[dict]:
    """
    Collect and group statistics by target
    :param stats_array: list of dictionaries with parameters and metrics
    :return: list of metrics and parameters grouped by target
    """
    result = OrderedDict()
    for stats in stats_array:
        update_from_stats(result, stats)

    return postprocess_targets(result)


def insert_metadata(args: argparse.Namespace) -> dict:
    """
    Collect metadata and statistics from specified files and merge them into result
    :param args: parsed program arguments
    :return: dictionary with statistics and metadata
    """
    stats_array = [item for f in args.stats_file for item in load(f)]
    result = {
        'version': JSON_VERSION,
        'targets': build_targets(stats_array),
        'metadata': build_metadata(args)
    }
    return result


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--stats_file', required=True, nargs='+',
        help='files (one or more) with statistics', type=str
    )
    parser.add_argument(
        '--commit', help='commit hash', type=str
    )
    parser.add_argument(
        '--build', help='build number', type=str
    )
    parser.add_argument(
        '--output_file', required=True,
        help='output file', type=str
    )
    parser.add_argument(
        '--timestamp', help='statistics timestamp',
        type=int, default=int(time())
    )
    parser.add_argument(
        '--source_type', help='source type of metadata',
        type=str, default="Manual"
    )
    parser.add_argument(
        '--source_id', help='source id of metadata', type=str
    )
    parser.add_argument(
        '--branch', help='branch name', type=str
    )

    args = parser.parse_args()
    return args


def main():
    args = get_args()
    stats = insert_metadata(args)
    with open(args.output_file, "w") as f:
        json.dump(stats, f, indent=4)


if __name__ == "__main__":
    main()

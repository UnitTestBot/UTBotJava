import argparse
import json
from os import listdir
from os.path import isfile, join
from time import time
from typing import Iterator

from monitoring_settings import JSON_VERSION
from utils import *


def get_file_seq(input_data_dir: str) -> Iterator[str]:
    """
    Get all files from specified directory
    :param input_data_dir: path to directory with files
    :return: sequence of filepaths
    """
    for filename in listdir(input_data_dir):
        path = join(input_data_dir, filename)
        if isfile(path):
            yield path


def check_stats(stats: dict, args: argparse.Namespace) -> bool:
    """
    Checks timestamp and version of given statistics
    :param stats: dictionary with statistics and metadata
    :param args: parsed program arguments
    :return: is timestamp and version match
    """
    try:
        timestamp = stats["metadata"]["timestamp"]
        timestamp_match = args.timestamp_from <= timestamp <= args.timestamp_to
        json_version_match = stats["version"] == JSON_VERSION
        return timestamp_match and json_version_match
    except:
        return False


def get_stats_seq(args: argparse.Namespace) -> Iterator[dict]:
    """
    Get statistics with metadata matched specified period
    :param args: parsed program arguments
    :return: sequence of statistics with metadata filtered by version and timestamp
    """
    for file in get_file_seq(args.input_data_dir):
        with open(file, "r") as f:
            stats = json.load(f)
        if check_stats(stats, args):
            yield stats


def transform_target_stats(stats: dict) -> dict:
    """
    Transform metrics by computing total coverage
    :param stats: metrics
    :return: transformed metrics
    """
    common_prefix = "covered_instructions"
    denum = stats["total_instructions"]

    nums_keys = [(key, key.removeprefix(common_prefix)) for key in stats.keys() if key.startswith(common_prefix)]

    for (key, by) in nums_keys:
        num = stats[key]
        stats["total_coverage" + by] = 100 * num / denum if denum != 0 else 0
        del stats[key]

    del stats["total_instructions"]

    return stats


def aggregate_stats(stats_seq: Iterator[dict]) -> List[dict]:
    """
    Aggregate list of metrics and parameters into list of transformed metrics and parameters grouped by targets
    :param stats_seq: sequence of metrics and parameters
    :return: list of metrics and parameters grouped by targets
    """
    result = get_default_metrics_dict()

    for stats in stats_seq:
        targets = stats["targets"]
        timestamp = stats["metadata"]["timestamp"]
        for target in targets:
            full_name = f'{target["id"]}-{target["version"]}'
            new_data = result[full_name]
            for target_stats in target["metrics"]:
                new_data["metrics"].append(transform_target_stats(target_stats))
            for target_params in target["parameters"]:
                target_params["timestamp"] = timestamp
                new_data["parameters"].append(target_params)

    return postprocess_targets(result)


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--input_data_dir', required=True,
        help='input directory with data', type=str
    )
    parser.add_argument(
        '--output_file', required=True,
        help='output file', type=str
    )
    parser.add_argument(
        '--timestamp_from', help='timestamp started collection from',
        type=int, default=0
    )
    parser.add_argument(
        '--timestamp_to', help='timestamp finished collection to',
        type=int, default=int(time())
    )

    args = parser.parse_args()
    return args


def main():
    args = get_args()
    stats_seq = get_stats_seq(args)
    result = aggregate_stats(stats_seq)
    with open(args.output_file, "w") as f:
        json.dump(result, f, indent=4)


if __name__ == '__main__':
    main()

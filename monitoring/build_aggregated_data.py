import argparse
import json
from collections import defaultdict
from os import listdir
from os.path import isfile, join
from time import time

from monitoring_settings import JSON_VERSION


def get_file_seq(input_data_dir):
    for filename in listdir(input_data_dir):
        path = join(input_data_dir, filename)
        if isfile(path):
            yield path


def check_stats(stats, args):
    timestamp = stats["metadata"]["timestamp"]
    timestamp_match = args.timestamp_from <= timestamp <= args.timestamp_to
    json_version_match = stats["json_version"] == JSON_VERSION
    return timestamp_match and json_version_match


def get_stats_seq(args):
    for file in get_file_seq(args.input_data_dir):
        with open(file, "r") as f:
            stats = json.load(f)
        if check_stats(stats, args):
            yield stats


def transform_and_combine_target(stats_list, timestamp):
    new_stats = defaultdict(lambda: 0.0)

    # calculate average by all keys
    for n, stats in enumerate(stats_list, start=1):
        transformed = transform_target(stats)
        for key in transformed:
            new_stats[key] = new_stats[key] + (transformed[key] - new_stats[key]) / n

    new_stats['timestamp'] = timestamp

    return new_stats


def transform_target(stats):
    common_prefix = "covered_instructions_count"
    denum = stats["total_instructions_count"]

    nums_keys = [(key, key.removeprefix(common_prefix)) for key in stats.keys() if key.startswith(common_prefix)]

    for (key, by) in nums_keys:
        num = stats[key]
        stats["total_coverage" + by] = 100 * num / denum if denum != 0 else 0
        del stats[key]

    del stats["total_instructions_count"]

    return stats


def aggregate_stats(stats_seq):
    result = defaultdict(lambda: [])

    for stats in stats_seq:
        targets = stats["targets"]
        timestamp = stats["metadata"]["timestamp"]
        for target in targets:
            result[target].append(
                transform_and_combine_target(targets[target], timestamp)
            )

    return result


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

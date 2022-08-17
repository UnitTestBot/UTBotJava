import json
import re
from os import listdir
from os.path import isfile, join
from sys import argv

"""
Input files data requires names format *-<timestamp>-<commit>.json
"""
FILE_FORMAT_REGEXP = re.compile(r'.+-(\d+)-[^-]+\.json')


def get_file_timestamp(filename):
    result = FILE_FORMAT_REGEXP.fullmatch(filename)
    if result is None:
        return None
    return int(result.group(1))


def check_timestamp(timestamp, timestamp_from, timestamp_to):
    return timestamp is not None and timestamp_from <= timestamp <= timestamp_to


def get_file_seq(input_data_dir, timestamp_from, timestamp_to):
    for filename in listdir(input_data_dir):
        path = join(input_data_dir, filename)
        if isfile(path):
            timestamp = get_file_timestamp(filename)
            if check_timestamp(timestamp, timestamp_from, timestamp_to):
                yield path, timestamp


def get_stats_seq(input_data_dir, timestamp_from, timestamp_to):
    for file, timestamp in get_file_seq(input_data_dir, timestamp_from, timestamp_to):
        with open(file, "r") as f:
            stats = json.load(f)
        yield stats, timestamp


def transform_stats(stats, timestamp):
    del stats['metadata']
    stats['timestamp'] = timestamp
    return stats


def aggregate_stats(stats_seq):
    result = []
    for stats, timestamp in stats_seq:
        result.append(transform_stats(stats, timestamp))
    return result


def main():
    args = argv[1:]
    if len(args) != 4:
        raise RuntimeError(
            f"Expected <input data dir> <output file> "
            f"<timestamp from> <timestamp to> "
            f"but got {' '.join(args)}"
        )
    (input_data_dir, output_file, timestamp_from, timestamp_to) = args
    timestamp_from = int(timestamp_from)
    timestamp_to = int(timestamp_to)
    stats_seq = get_stats_seq(input_data_dir, timestamp_from, timestamp_to)
    result = aggregate_stats(stats_seq)
    with open(output_file, "w") as f:
        json.dump(result, f, indent=4)


if __name__ == '__main__':
    main()

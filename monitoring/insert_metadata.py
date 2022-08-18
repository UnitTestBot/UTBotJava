import json
import re
import subprocess
from collections import defaultdict
from os import environ
from os.path import exists
from platform import uname
from sys import argv


def load(json_file):
    if exists(json_file):
        with open(json_file, "r") as f:
            return json.load(f)
    return None


def transform_stats(stats):
    common_prefix = "covered_instructions_count"
    denum = stats["total_instructions_count"]

    nums_keys = [(key, key.removeprefix(common_prefix)) for key in stats.keys() if key.startswith(common_prefix)]

    for (key, by) in nums_keys:
        num = stats[key]
        stats["total_coverage" + by] = 100 * num / denum if denum != 0 else 0
        del stats[key]

    del stats["total_instructions_count"]

    return stats


def transform_and_combine_stats(stats_list):
    new_stats = defaultdict(lambda: 0.0)

    # calculate average by all keys
    for n, stats in enumerate(stats_list, start=1):
        transformed = transform_stats(stats)
        for key in transformed:
            new_stats[key] = new_stats[key] + (transformed[key] - new_stats[key]) / n

    return new_stats


def try_get_output(args):
    try:
        return subprocess.check_output(args, stderr=subprocess.STDOUT, shell=True).decode()
    except Exception as e:
        print(f'Error in command "{args}":\n\t{e}')
        return None


def parse_gradle_version(s):
    if s is None:
        return None
    regex = re.compile(r'^\s*(Gradle [.\d]+)\s*$', re.MULTILINE)
    result = regex.search(s)
    if result is None:
        return None
    return result.group(1)


def build_environment_data():
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


def build_metadata(commit, build):
    metadata = {
        'commit_hash': commit,
        'build_number': build,
        'environment': build_environment_data()
    }
    return metadata


def transform_and_insert_metadata(stats_file, commit, build):
    stats = load(stats_file)
    if stats is None:
        raise FileNotFoundError("File with stats does not exist!")
    stats = transform_and_combine_stats(stats)
    stats['metadata'] = build_metadata(commit, build)
    return stats


def main():
    args = argv[1:]
    if len(args) != 4:
        raise RuntimeError(
            f"Expected <stats file> <output file> "
            f"<commit hash> <build number> "
            f"but got {' '.join(args)}"
        )
    (stats_file, output_file, commit, build) = args
    stats = transform_and_insert_metadata(stats_file, commit, build)
    with open(output_file, "w") as f:
        json.dump(stats, f, indent=4)


if __name__ == "__main__":
    main()

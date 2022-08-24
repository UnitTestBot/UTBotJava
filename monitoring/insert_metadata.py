import argparse
import json
import subprocess
from datetime import datetime
from os import environ
from os.path import exists
from platform import uname
from time import time

from monitoring_settings import JSON_VERSION
from utils import *


def load(json_file):
    if exists(json_file):
        with open(json_file, "r") as f:
            return json.load(f)
    return None


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


def build_metadata(args):
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


def build_targets(stats_array):
    result = get_default_metrics_dict()
    for stats in stats_array:
        target = stats['parameters']['target']
        del stats['parameters']['target']
        update_target(result[target], stats)

    return postprocess_targets(result)


def insert_metadata(args):
    stats_array = load(args.stats_file)
    if stats_array is None:
        raise FileNotFoundError("File with stats does not exist!")
    result = {
        'version': JSON_VERSION,
        'targets': build_targets(stats_array),
        'metadata': build_metadata(args)
    }
    return result


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--stats_file', required=True,
        help='file with statistics', type=str
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

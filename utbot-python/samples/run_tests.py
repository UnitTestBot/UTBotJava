"""
Example command
<python> run_tests.py generate <java> <utbot-cli-python.jar> <path to UTBotJava/utbot-python/samples> -c test_configuration.json
 -p <python> -o <path to UTBotJava/utbot-python/samples/cli_test_dir or another directory>

<python> run_tests.py run -p <python> -t <path to UTBotJava/utbot-python/samples/cli_test_dir or another directory>
 -c <path to UTBotJava/utbot-python/samples>
"""
import argparse
import json
import os
import typing
import pathlib


def parse_arguments():
    parser = argparse.ArgumentParser(
        prog='UtBot Python test',
        description='Generage tests for example files'
    )
    subparsers = parser.add_subparsers(dest="command")
    parser_generate = subparsers.add_parser('generate', help='Generate tests')
    parser_generate.add_argument('java')
    parser_generate.add_argument('jar')
    parser_generate.add_argument('path_to_test_dir')
    parser_generate.add_argument('-c', '--config_file')
    parser_generate.add_argument('-p', '--python_path')
    parser_generate.add_argument('-o', '--output_dir')
    parser_generate.add_argument('-i', '--coverage_output_dir')
    parser_run = subparsers.add_parser('run', help='Run tests')
    parser_run.add_argument('-p', '--python_path')
    parser_run.add_argument('-t', '--test_directory')
    parser_run.add_argument('-c', '--code_directory')
    parser_coverage = subparsers.add_parser('check_coverage', help='Check coverage')
    parser_coverage.add_argument('-i', '--coverage_output_dir')
    parser_coverage.add_argument('-c', '--config_file')
    return parser.parse_args()


def parse_config(config_path: str):
    with open(config_path, "r") as fin:
        return json.loads(fin.read())


def generate_tests(
        java: str,
        jar_path: str,
        sys_paths: list[str],
        python_path: str,
        file_under_test: str,
        timeout: int,
        output: str,
        coverage_output: str,
        class_names: typing.Optional[list[str]] = None,
        method_names: typing.Optional[list[str]] = None
    ):
    command = f"{java} -jar {jar_path} generate_python {file_under_test}.py -p {python_path} -o {output} -s {' '.join(sys_paths)} --timeout {timeout * 1000} --install-requirements --runtime-exception-behaviour PASS --coverage={coverage_output}"
    if class_names is not None:
        command += f" -c {','.join(class_names)}"
    if method_names is not None:
        command += f" -m {','.join(method_names)}"
    print(command)
    code = os.system(command)
    return code


def run_tests(
        python_path: str,
        tests_dir: str,
        samples_dir: str,
):
    command = f'{python_path} -m coverage run --source={samples_dir} -m unittest {tests_dir} -p "utbot_*"'
    print(command)
    code = os.system(command)
    return code


def check_coverage(
        config_file: str,
        coverage_output_dir: str,
):
    config = parse_config(config_file)
    report: typing.Dict[str, bool] = {}
    for part in config['parts'][:2]:
        for file in part['files'][:2]:
            for group in file['groups'][:2]:
                expected_coverage = group.get('coverage', 0)

                file_suffix = f"{part['path'].replace('/', '_')}_{file['name']}"
                coverage_output_file = pathlib.PurePath(coverage_output_dir, f"coverage_{file_suffix}.json")
                with open(coverage_output_file, "rt") as fin:
                    actual_coverage_json = json.loads(fin.readline())
                actual_covered = sum(lines['end'] - lines['start'] + 1 for lines in actual_coverage_json['covered'])
                actual_not_covered = sum(lines['end'] - lines['start'] + 1 for lines in actual_coverage_json['notCovered'])
                actual_coverage = round(actual_covered / (actual_not_covered + actual_covered)) * 100

                report[file_suffix] = actual_coverage >= expected_coverage
    if all(report.values()):
        return True
    print("-------------")
    print("Bad coverage:")
    print("-------------")
    for file, good_coverage in report.items():
        if not good_coverage:
            print(file)
    return False


def main_test_generation(args):
    config = parse_config(args.config_file)
    for part in config['parts'][:2]:
        for file in part['files'][:2]:
            for group in file['groups'][:2]:
                full_name = pathlib.PurePath(args.path_to_test_dir, part['path'], file['name'])
                output_file = pathlib.PurePath(args.output_dir, f"utbot_tests_{part['path'].replace('/', '_')}_{file['name']}.py")
                coverage_output_file = pathlib.PurePath(args.coverage_output_dir, f"coverage_{part['path'].replace('/', '_')}_{file['name']}.json")
                generate_tests(
                    args.java,
                    args.jar,
                    [args.path_to_test_dir],
                    args.python_path,
                    str(full_name),
                    group['timeout'],
                    str(output_file),
                    str(coverage_output_file),
                    group['classes'],
                    group['methods']
                )
                    

if __name__ == '__main__':
    arguments = parse_arguments()
    if arguments.command == 'generate':
        main_test_generation(arguments)
    elif arguments.command == 'run':
        run_tests(arguments.python_path, arguments.test_directory, arguments.code_directory)
    elif arguments.command == 'check_coverage':
        check_coverage(arguments.config_file, arguments.coverage_output_dir)

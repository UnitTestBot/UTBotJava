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
    subparsers = parser.add_subparsers()
    parser_generate = subparsers.add_parser('generate', help='Generate tests')
    parser_generate.add_argument('java')
    parser_generate.add_argument('jar')
    parser_generate.add_argument('path_to_test_dir')
    parser_generate.add_argument('-c', '--config_file')
    parser_generate.add_argument('-p', '--python_path')
    parser_generate.add_argument('-o', '--output_dir')
    parser_run = subparsers.add_parser('run', help='Run tests')
    parser_run.add_argument('-p', '--python_path')
    parser_run.add_argument('-t', '--test_directory')
    parser_run.add_argument('-c', '--code_directory')
    return parser.parse_args()


def parse_config(config_path: str):
    with open(config_path, "r") as fin:
        return json.loads(fin.read())


def generate_tests(
    java: str,
    jar_path: str,
    sys_paths: list[str],
    python_path: str, 
    file: str, 
    timeout: int,
    output: str,
    class_name: typing.Optional[str] = None,
    method_names: typing.Optional[str] = None
    ):
    command = f"{java} -jar {jar_path} generate_python {file}.py -p {python_path} -o {output} -s {' '.join(sys_paths)} --timeout {timeout * 1000} --install-requirements --runtime-exception-behaviour Passing"
    if class_name is not None:
        command += f" -c {class_name}"
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


def main():
    config = parse_config(args.config_file)
    for part in config['parts']:
        for file in part['files']:
            for group in file['groups']:
                full_name = pathlib.PurePath(args.path_to_test_dir, part['path'], file['name'])
                output_file = pathlib.PurePath(args.output_dir, f"utbot_tests_{part['path'].replace('/', '_')}_{file['name']}.py")
                generate_tests(
                    args.java,
                    args.jar,
                    [args.path_to_test_dir],
                    args.python_path,
                    str(full_name),
                    group['timeout'],
                    str(output_file),
                    group['classes'],
                    group['methods']
                )
                    

if __name__ == '__main__':
    args = parse_arguments()
    print(args)

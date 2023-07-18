"""
Example command
<python> run_tests.py <java> <utbot-cli-python.jar> <path to UTBotJava/utbot-python/samples> -c test_configuration.json
 -p <python> -o  <path to UTBotJava/utbot-python/samples/cli_test_dir or another directory>
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
    parser.add_argument('java')
    parser.add_argument('jar')
    parser.add_argument('path_to_test_dir')
    parser.add_argument('-c', '--config_file')
    parser.add_argument('-p', '--python_path')
    parser.add_argument('-o', '--output_dir')
    return parser.parse_args()


def parse_config(config_path: str):
    with open(config_path, "r") as fin:
        return json.loads(fin.read())


def run_tests(
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
    command = f"{java} -jar {jar_path} generate_python {file}.py -p {python_path} -o {output} -s {' '.join(sys_paths)} --timeout {timeout * 1000} --install-requirements"
    if class_name is not None:
        command += f" -c {class_name}"
    if method_names is not None:
        command += f" -m {','.join(method_names)}"
    print(command)
    code = os.system(command)
    return code


def main():
    args = parse_arguments()
    config = parse_config(args.config_file)
    for part in config['parts']:
        for file in part['files']:
            for group in file['groups']:
                full_name = pathlib.PurePath(args.path_to_test_dir, part['path'], file['name'])
                output_file = pathlib.PurePath(args.output_dir, f"utbot_tests_{part['path'].replace('/', '_')}_{file['name']}.py")
                run_tests(
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
    main()

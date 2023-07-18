import argparse
import json
import os


def parse_arguments():
    parser = argparse.ArgumentParser(
        prog='UtBot Python test',
        description='Generage tests for example files'
    )
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
    jar_path: str,
    sys_paths: list[str],
    python_path: str, 
    file: str, 
    timeout: int,
    output: str,
    class_name: typing.Optional[str] = None,
    method_names: typing.Optional[str] = None
    ):
    command = f"java -jar {jar_path} generate_python {file} -p {python_path} -o {output} -s {' '.join(sys_path)} --timeout {timeout * 1000}"
    if class_name is not None:
        command += f" -c {class_name}"
    if method_names is not None:
        command += f" -m {','.join(method_names)}"
    os.system(command)


def main():
    args = parse_arguments()
    config = parse_config(args.config_file)
    for part in config['parts']:
        for file in part['files']:
            for group in file['groups']:
                full_name = '/'.join([args.path_to_test_dir, part['path'], file['name']])
                output_file = '/'.join([args.output])
                run_tests(
                    args.jar,
                    args.path_to_test_dir,
                    args.python_path,
                    full_name,
                    group.timeout,
                    output_file,
                    group.classes,
                    group.methods
                )
                    

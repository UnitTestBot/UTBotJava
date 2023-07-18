import json
import os


def parse_config(config_path: str):
    with open(config_path, "r") as fin:
        return json.loads(fin.read())


def run_tests(jar_path: str, sys_paths: [str], python_path: str, files: list[str], timeout: int = 20, output: str):
    command = f"java -jar {jar_path} generate_python {' '.join(files)} -p {python_path} -o {output} -s {' '.join(sys_path)} --timeout {timeout * 1000}"
    try:
        os.system(command)

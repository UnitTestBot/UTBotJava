import json
import pathlib
import sys
import typing

def read_test_config(path: pathlib.Path, coverage_dir: pathlib.Path) -> typing.Dict[str, int]:
    with open(path, 'r', encoding='utf-8') as fin:
        data = json.loads(fin.read())

    res = {}
    for part in data['parts']:
        for file in part['files']:
            for group in file['groups']:
                file_suffix = f"{part['path'].replace('/', '_')}_{file['name']}"
                executions_output_file = pathlib.Path(coverage_dir, f"coverage_{file_suffix}.json.executions")
                timeout = group['timeout']
                res[executions_output_file] = timeout
    return res


def read_executions(path: pathlib.Path):
    with open(path, 'r', encoding='utf-8') as fin:
        data = json.loads(fin.read())
    return data


def main(config_path: pathlib.Path, executions_path: pathlib.Path, coverage_dir: pathlib.Path):
    timeouts = read_test_config(config_path, coverage_dir)
    executions = read_executions(executions_path)

    for f, timeout in timeouts.items():
        executions[str(f)] /= timeout

    with open('speeds.json', 'w') as fout:
        print(json.dumps(executions, indent=1), file=fout)


if __name__ == '__main__':
    main(*sys.argv[1:])



import json
import pathlib
import sys
import typing


def get_all_files(folder: pathlib.Path) -> typing.List[pathlib.Path]:
    if folder.is_dir:
        return folder.glob("*.executions")
    return []


def get_excecutions_number(file: pathlib.Path) -> int:
    with open(file, 'r', encoding='utf-8') as fin:
        return int(json.loads(fin.readline().strip())['executions'])


def save_data(data: typing.Dict[pathlib.Path, int]) -> None:
    with open('data_executions.json', 'w', encoding='utf-8') as fout:
        print(json.dumps(data, indent=1), file=fout)


def main(folder: pathlib.Path) -> None:
    files = get_all_files(folder)
    data = {
            str(file): get_excecutions_number(file)
            for file in files
            }
    save_data(data)


if __name__ == '__main__':
    main(pathlib.Path(sys.argv[1]))


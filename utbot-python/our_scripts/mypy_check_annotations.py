import argparse
import json
import os
from itertools import product
from typing import Optional

from find_and_check_types import run_mypy, add_annotations


def check_possible_annotations(
        code: str,
        function_name: str,
        annotations: dict[str, list[str]],
        filename: Optional[str] = None,
):
    run_mypy(code, filename)
    default_res = run_mypy(code, filename)
    annotations_ = [
        [(name, ann) for ann in anns]
        for name, anns in annotations.items()
    ]

    good_types = []
    for _new_annotations in product(*annotations_):
        new_annotations = {
            name_: annotation
            for (name_, annotation) in _new_annotations
        }
        res = run_mypy(
            add_annotations(code, function_name, new_annotations),
            filename
        )
        if len(default_res) > len(res) or (len(default_res) == len(res) and default_res == res):
            good_types.append(new_annotations)

    return good_types


def main_with_args():
    parser = argparse.ArgumentParser(
        description='',
    )
    parser.add_argument('function_name', type=str)
    parser.add_argument('annotations', type=str)
    parser.add_argument('code', type=str)
    parser.add_argument('output_file', type=str)

    args = parser.parse_args()
    filename = f'{args.function_name}__mypy.py'

    results = check_possible_annotations(
        args.code,
        args.function_name,
        eval(args.annotations),
        filename,
    )
    os.remove(filename)

    with open(args.output_file, 'w') as fout:
        print(json.dumps(results), file=fout)


if __name__ == '__main__':
    """
    Example:
    python mypy_check_annotations.py 'f' '{"x": ["int", "str"], "y": ["int", "float"]}' 'def f(x, y): return x + y' 'type_checking_result__f'
    """
    main_with_args()

import json
from os.path import exists
from typing import Optional


def load(json_file: str) -> Optional[any]:
    """
    Try load object from json file
    :param json_file: path to json file
    :return: object from given json file or None
    """
    if exists(json_file):
        with open(json_file, "r") as f:
            return json.load(f)
    return None

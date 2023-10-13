import re


def check_regex(string: str) -> bool:
    pattern = r"'(''|\\\\|\\'|[^'])*'"
    if re.match(pattern, string):
        return True
    return False


def create_pattern(string: str):
    if len(string) > 10:
        return re.compile(rf"{string}")
    else:
        return re.compile(string)

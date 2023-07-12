import re
import time


def check_regex(string: str) -> bool:
    pattern = r"'(''|\\\\|\\'|[^'])*'"
    if re.match(pattern, string):
        return True
    return False

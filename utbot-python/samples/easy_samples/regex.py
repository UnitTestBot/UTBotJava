import re
import time


def check_regex(string: str) -> bool:
    pattern = r"'(''|\\\\|\\'|[^'])*'"
    if re.match(pattern, string):
        return True
    return False


def timetest():
    t = time.time()
    print(check_regex("\'\J/\\\\\\'\\N''''P'''\'x'L''\'';'\'N\$'\\\'\'\`''D\\''�='''m\\\\\'\'\\H\\No'F'(''U]\'V"))
    print((time.time() - t))

if __name__ == '__main__':
    timetest()

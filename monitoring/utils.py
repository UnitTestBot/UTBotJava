import re

from monitoring_settings import DEFAULT_PROJECT_VERSION


def parse_name_and_version(full_name):
    regex = re.compile(r'([^.]+)(-([\d.]*))?')
    result = regex.fullmatch(full_name)
    if result is None:
        return full_name, DEFAULT_PROJECT_VERSION
    name = result.group(1)
    version = result.group(3) or DEFAULT_PROJECT_VERSION
    return name, version


def postprocess_targets(targets):
    result = []
    for target in targets:
        (name, version) = parse_name_and_version(target)
        result.append({
            'id': name,
            'version': version,
            'metrics': targets[target]
        })
    return result

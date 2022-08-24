import re
from collections import defaultdict

from monitoring_settings import DEFAULT_PROJECT_VERSION


def parse_name_and_version(full_name):
    regex = re.compile(r'([^.]+)-([\d.]+)')
    result = regex.fullmatch(full_name)
    if result is None:
        return full_name, DEFAULT_PROJECT_VERSION
    name = result.group(1)
    version = result.group(2)
    return name, version


def postprocess_targets(targets):
    result = []
    for target in targets:
        (name, version) = parse_name_and_version(target)
        result.append({
            'id': name,
            'version': version,
            **targets[target]
        })
    return result


def get_default_metrics_dict():
    return defaultdict(lambda: {
        'parameters': [],
        'metrics': []
    })


def update_target(target, stats):
    target['parameters'].append(stats['parameters'])
    target['metrics'].append(stats['metrics'])
    return target

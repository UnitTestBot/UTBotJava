import re
from collections import defaultdict
from typing import List

from monitoring_settings import DEFAULT_PROJECT_VERSION


def parse_name_and_version(full_name: str) -> tuple[str, str]:
    """
    Parse from string name and version of project
    :param full_name: string with format <name>-<version>
    :return: pair of name and version strings
    """
    regex = re.compile(r'([^.]+)-([\d.]+)')
    result = regex.fullmatch(full_name)
    if result is None:
        return full_name, DEFAULT_PROJECT_VERSION
    name = result.group(1)
    version = result.group(2)
    return name, version


def postprocess_targets(targets: dict) -> List[dict]:
    """
    Transform dictionary with fullname target keys into array with target objects
    :param targets: dictionary with fullname target keys
    :return: array of targets
    """
    result = []
    for target in targets:
        (name, version) = parse_name_and_version(target)
        result.append({
            'id': name,
            'version': version,
            **targets[target]
        })
    return result


def get_default_metrics_dict() -> dict:
    return defaultdict(lambda: {
        'parameters': [],
        'metrics': []
    })


def update_target(target: dict, stats: dict) -> dict:
    """
    Update dictionary with target by new stats
    :param target: dictionary with target metrics and parameters
    :param stats: new metrics and parameters
    :return: updated target dictionary
    """
    target['parameters'].append(stats['parameters'])
    target['metrics'].append(stats['metrics'])
    return target

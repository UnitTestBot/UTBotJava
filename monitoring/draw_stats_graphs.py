import json
from os.path import exists
from sys import argv
from time import time
import matplotlib.pyplot as plt


def load(json_file):
    if exists(json_file):
        with open(json_file, "r") as f:
            return json.load(f)
    return None


def transform_stats(stats):
    num = stats["covered_instructions_count"]
    denum = stats["total_instructions_count"]
    stats["total_coverage"] = num / denum if denum != 0 else 0
    del stats["covered_instructions_count"]
    del stats["total_instructions_count"]

    stats["timestamp"] = time()

    return stats


def update_stats_history(history_file, new_stats_file):
    history = load(history_file) or []
    new_stats = load(new_stats_file)
    if new_stats is None:
        raise FileNotFoundError("File with new stats not exists!")
    history.append(transform_stats(new_stats))
    with open(history_file, "w") as f:
        json.dump(history, f, indent=4)
    return history


def render_history(history, graph_file):
    pass


def main():
    args = argv[1:]
    if len(args) != 3:
        raise RuntimeError(f"Expected <history file> <new stats file> <output graph>, but got {' '.join(args)}")
    history = update_stats_history(args[0], args[1])
    render_history(history, args[2])


if __name__ == "__main__":
    main()

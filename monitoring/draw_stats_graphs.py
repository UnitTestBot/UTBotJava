import json
from datetime import datetime
from os.path import exists
from sys import argv
from time import time

import matplotlib.pyplot as plt
from matplotlib.dates import DayLocator, ConciseDateFormatter

DPI = 128
WIDTH = 20
HEIGHT = 10


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


def get_history_x(history):
    return list(map(lambda x: datetime.fromtimestamp(x["timestamp"]), history))


def get_history_y_seq(history):
    for key in history[0]:
        if key != "timestamp":
            yield key, list(map(lambda x: x[key], history))


def format_key_label(key):
    return key.title().replace('_', ' ')


def get_subplot(title):
    fig, ax = plt.subplots()

    locator = DayLocator(bymonthday=range(1, 32))
    formatter = ConciseDateFormatter(locator)
    ax.xaxis.set_major_locator(locator)
    ax.xaxis.set_major_formatter(formatter)

    ax.set_title(title)

    fig.set_size_inches(WIDTH, HEIGHT)
    fig.set_dpi(DPI)

    return fig, ax


def postprocess_plot(ax):
    ax.legend(loc="upper left")


def render_history(history, coverage_graph_file, quantitative_graph_file):
    coverage_fig, coverage_ax = get_subplot("Monitoring Coverage Statistics")
    quantitative_fig, quantitative_ax = get_subplot("Monitoring Quantitative Statistics")

    x = get_history_x(history)
    for key, y in get_history_y_seq(history):
        if "coverage" in key:
            coverage_ax.plot(x, y, label=format_key_label(key))
        else:
            quantitative_ax.plot(x, y, label=format_key_label(key))

    postprocess_plot(coverage_ax)
    postprocess_plot(quantitative_ax)

    coverage_fig.savefig(coverage_graph_file)
    quantitative_fig.savefig(quantitative_graph_file)


def main():
    args = argv[1:]
    if len(args) != 4:
        raise RuntimeError(
            f"Expected <history file> <new stats file> "
            f"<output coverage graph> <output quantitative graph>, "
            f"but got {' '.join(args)}"
        )
    history = update_stats_history(args[0], args[1])
    render_history(history, args[2], args[3])


if __name__ == "__main__":
    main()

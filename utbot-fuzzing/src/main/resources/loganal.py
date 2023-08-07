import os
import sys
import re
from datetime import datetime
import argparse
import pandas as pd
from tqdm import tqdm
from matplotlib import pyplot as plt


plt.rcParams["figure.figsize"] = (16, 9)


def parse_java_log(file_path: str, operation):
    with open(file_path, 'r') as log:
        items = log.read().replace('\n', '').split(' | ')[:-1]
        
        c = 0
        for item in items:
            match c:
                case 0: time = datetime.strptime(item, '[%H:%M:%S:%f]')
                case 1: value = item
                case 2:
                    try:
                        class_name = item.split(': ')[1]
                    except:
                        print(item)
                case 3: method = item.split(': ')[1]
                case 4: line_number = item.split(': ')[1]
                case 5: trace_hash = item.split(': ')[1]
                case 6: trace_count = item.split(': ')[1]
                case 7:
                    event = item
                    operation(time, value, class_name, method, line_number, trace_hash, trace_count, event)
                    c = -1
            c += 1
    pass

def build_dataframe(logpath: str):
    file_path = os.path.expanduser(logpath)
    pattern = r'(?<!"")\b\w+\b(?![^"]*["])'
    mutation_labels = set()

    def get_mutations_labels(time, value, class_name, method, line_number, trace_hash, trace_count, event):
        mutations = re.findall(pattern, value)
        if value == 'FuzzedValue: %var% = null':
            return
        mutations = list(filter(lambda label: label != 'null' and label != 'var', mutations))

        for mutation in mutations:
            mutation_labels.add(mutation)

    parse_java_log(file_path, get_mutations_labels)
 
    data = []
    def put_data(time, value, class_name, method, line_number, trace_hash, trace_count, event):
        mutations = re.findall(pattern, value)
        mutations_mapping = [ 1 if mutation in mutations else 0 for mutation in mutation_labels ]
        data.append(
                [
                    class_name,
                    method,
                    value
                ] + mutations_mapping + [
                    line_number,
                    trace_hash,
                    trace_count,
                    event,
                    time
                ]
            )

    parse_java_log(file_path, put_data)

    df = pd.DataFrame(data, columns =
                      ['class_name', 'method', 'value'] + list(mutation_labels) + 
                      ['line_number', 'trace_hash', 'trace_count', 'event', 'time'])
    df = df.assign(new_feedbacks_count = (df.event == 'NEW_FEEDBACK').cumsum())

    return (df, list(mutation_labels))


def plot_specific_effectiveness(df, ax):
    ax.set_title('Specific effectiveness')
    [ ax.plot( df.index, (df[label]*df.new_feedbacks_count.diff()).cumsum() / df[label].cumsum()) for label in mutation_labels ]
    ax.legend(mutation_labels)
    ax.set_xlabel('Iteration')
    ax.set_ylabel('Specific effectiveness')
    ax.set_yscale('log')
    ax.grid()


def plot_total_specific_effectiveness(df, ax):
    ax.set_title('Total specific effectiveness')
    ax.bar(mutation_labels, [ (df[label] * df.new_feedbacks_count.diff()).sum() / df[label].sum() for label in mutation_labels] )
    ax.tick_params(axis='x', labelrotation=-15)
    ax.grid(axis='y')


def plot_discovered_traces(df, ax):
    ax.set_title('Number of discovered traces')
    [ ax.plot(df.index, (df.new_feedbacks_count.diff() * df[label]).cumsum()) for label in mutation_labels ]
    ax.plot(df.index, df.new_feedbacks_count)
    ax.legend(labels = mutation_labels + ['Overall'])
    ax.set_xlabel('Iteration')
    ax.set_ylabel('Number of traces')
    ax.grid()


def plot_empirical_probabilities(df, ax):
    ax.set_title('Empirical probability')
    [ ax.plot(df.index[50:], (df[label].cumsum() / df.index)[50:]) for label in mutation_labels ]
    ax.legend(labels=mutation_labels)
    ax.set_xlabel('Iteration')
    ax.set_ylabel('Number of mutations')
    ax.grid()


def draw_report(df: pd.DataFrame, mutations_labels: list, title: str, pic_path = None):
    plt.figure(0)
    ax1 = plt.subplot2grid((2,4), (0,0), colspan=3)
    ax2 = plt.subplot2grid((2,4), (0,3))
    ax3 = plt.subplot2grid((2,4), (1,0), colspan=2)
    ax4 = plt.subplot2grid((2,4), (1,2), colspan=2)

    plot_discovered_traces(df, ax1)
    plot_total_specific_effectiveness(df, ax2)
    plot_empirical_probabilities(df, ax3)
    plot_specific_effectiveness(df, ax4)

    if title:
        plt.suptitle(title)

    if pic_path:
        plt.tight_layout()
        plt.subplots_adjust(wspace=0.2, hspace=0.2)
        plt.savefig(pic_path)
    else:
        plt.show()


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('path', help='Log file path', type=str)
    parser.add_argument('-t', '--title', help='Report title', type=str)
    parser.add_argument('-dp', '--dataframe-path', help='Path to save a dataframe', type=str)
    parser.add_argument('-pp', '--picture-path', help='Path to save a picture', type=str)
    args = parser.parse_args()
    
    (df, mutation_labels) = build_dataframe(args.path)

    if args.dataframe_path:
        df.to_pickle(args.dataframe_path)

    draw_report(df, mutation_labels, args.title, args.picture_path)


import os
import sys
import re
import pandas as pd
from tqdm import tqdm
from matplotlib import pyplot as plt

def build_dataframe(logpath: str):
    pattern = r'(?<=: )\w+(?=,|)'
    mutation_labels = set()
    
    file_path = os.path.expanduser(logpath)
    with open(file_path, 'r') as log:
        lines_counter = 0
        for line in log.readlines():
            try:
                if 'class name' in line.split("|")[1]:
                    continue

                mutations = re.findall(pattern, line.split("|")[1])

                for mutation in mutations:
                    mutation_labels.add(mutation)
                lines_counter+= 1
            except:
                pass

        log.seek(0)
        
        data = []
        for line in tqdm(log, total = lines_counter):
            try:
                (time, value, class_name, method, line_number, trace_hash, trace_count, event, _) = line.split(' | ')
                mutations = re.findall(pattern, value)
                mutations_mapping = [ 1 if mutation in mutations else 0 for mutation in mutation_labels ]

                data.append(
                    [
                        class_name.split(': ')[1],
                        method.split(': ', 1),
                        value
                    ] + mutations_mapping + [
                        line_number.split(': ')[1],
                        trace_hash.split(': ')[1],
                        trace_count.split(': ')[1],
                        event,
                        time[1:-1]
                    ]
                )
            except:
                pass

        return (pd.DataFrame(data,
            columns = ['class_name', 'method', 'value'] + list(mutation_labels) + ['line_number', 'trace_hash', 'trace_count', 'event', 'time']
        ),
        list(mutation_labels))


if __name__ == "__main__":
    (df, mutation_labels) = build_dataframe(sys.argv[1])
    df = df.assign(new_feedbacks_count = (df.event == 'NEW_FEEDBACK').cumsum())

    plt.figure(0)
    ax1 = plt.subplot2grid((2,3), (0,0), colspan=2)
    ax2 = plt.subplot2grid((2,3), (0,2))
    ax3 = plt.subplot2grid((2,3), (1,0), colspan=3)

    ax1.set_title('Specific effectiveness')
    [ ax1.plot( df.index, (df[label]*df.new_feedbacks_count.diff()).cumsum() / df[label].cumsum()) for label in mutation_labels ]
    ax1.legend(mutation_labels)
    ax1.set_xlabel('Iteration')
    ax1.set_ylabel('Specific effectiveness')
    ax1.set_yscale('log')
    ax1.grid()

    ax2.set_title('Total specific effectiveness')
    ax2.bar(mutation_labels, [ (df[label] * df.new_feedbacks_count.diff()).sum() / df[label].sum() for label in mutation_labels] )
    # ax2.grid(axis='y')

    ax3.set_title('Number of discovered traces')
    [ ax3.plot(df.index, (df.new_feedbacks_count.diff() * df[label]).cumsum()) for label in mutation_labels ]
    plt.plot(df.index, df.new_feedbacks_count)
    ax3.legend(labels = mutation_labels + ['Overall'])
    ax3.set_xlabel('Iteration')
    ax3.set_ylabel('Number of traces')
    ax3.grid()

    plt.suptitle('Regular traces')
    plt.show()

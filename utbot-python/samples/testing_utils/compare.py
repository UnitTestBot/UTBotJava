import json
import matplotlib.pyplot as plt
import pathlib


def read_stats(path: pathlib.Path):
    with open(path, 'r') as fin:
        return json.loads(fin.read())


data1 = read_stats('speeds_basic.json')
data2 = read_stats('speeds_forks.json')
data3 = read_stats('speeds_forks2.json')
data = [list(d.values()) for d in [data1, data2, data3]]

fig = plt.figure()
ax = fig.add_subplot(1, 1, 1)
ax.boxplot(
        data,
        labels=['basic', 'forks', 'forks2'],
        vert=True,
        patch_artist=True
        )
ax.grid(True)
plt.show()

import glob
import pandas as pd
from matplotlib import pyplot as plt 


def plot_on_and_off(inv_period, seed, color='blue', ax=plt):
    on_df = pd.read_pickle(f'regular_traces/log_inv{inv_period}_seed{seed}_on.pkl')
    off_df = pd.read_pickle(f'regular_traces/log_inv{inv_period}_seed{seed}_off.pkl')
    
    ax.plot(on_df.index, on_df.new_feedbacks_count, ls='--', c=color)
    ax.plot(off_df.index, off_df.new_feedbacks_count, ls='-', c=color)

    ax.set_xlim(-10, min(on_df.shape[0], off_df.shape[0]))
    ax.grid()


plt.figure(0)
ax1 = plt.subplot2grid((1,3), (0,0))
ax2 = plt.subplot2grid((1,3), (0,1))
ax3 = plt.subplot2grid((1,3), (0,2))

plot_on_and_off(1000, 0, ax=ax1)
plot_on_and_off(1000, 1, ax=ax2)
plot_on_and_off(1000, 2, ax=ax3)

plt.show()

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

plot_color = '#4e79a7'
plot_styles = ['dotted', 'dashed', 'dashdot', 'solid']

df = pd.read_table('benchmark.tsv')
for (m, g) in df.groupby('m'):
    g['log'] = np.log10(g['msMedian'] / 1e3)
    g['avg'] = g['log'].rolling(3, center=True, win_type='exponential').mean()
    rowFirst = g.index[ 0]
    rowLast  = g.index[-1]
    g.at[rowFirst, 'avg'] = g.at[rowFirst, 'log']
    g.at[rowLast,  'avg'] = g.at[rowLast,  'log']
    g['sec'] = 10**g['avg']

    plt.plot(
        g['n'], g['sec'], label=f'm={m}',
        color=plot_color, linestyle=plot_styles.pop()
    )

plt.xlim(10, 50)
plt.ylim(2.5e-6, 1e2)
plt.yscale('log')

plt.grid()
plt.legend(handlelength=3)
plt.title('Performance of SNP')
plt.xlabel('n')
plt.ylabel('median runtime (s)')

plt.savefig('benchmark.pdf', bbox_inches='tight')
plt.show()

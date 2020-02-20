import os
import numpy as np
import matplotlib.pyplot as plt
from argparse import ArgumentParser
from collections import defaultdict
from typing import List, Dict, Any, Optional

from plot_utils import read_as_json


BAR_WIDTH = 0.12


def plot_execution_times(results: List[List[Dict[str, Any]]], cockroach_profiling: List[Dict[str, Any]], output_file: Optional[str]):
    with plt.style.context('ggplot'):
        
        fix, ax = plt.subplots()
        colors = plt.rcParams['axes.prop_cycle'].by_key()['color']

        stats = defaultdict(list)
        for query_type, profile_results in enumerate(cockroach_profiling):

            stats['Cockroach'].append(dict(avg=np.average(profile_results['latency']), std=np.std(profile_results['latency'])))

            optimizer_times = defaultdict(list)
            for results_list in results:
                for opt_result in results_list:
                    opt_name = opt_result['optimizer_name']
                    opt_times = [r['elapsed_time'] for r in opt_result['stats'] if r['query_type'] == query_type]
                    
                    optimizer_times[opt_name].extend(opt_times)

            for opt_name, times in optimizer_times.items():
                stats[opt_name].append(dict(avg=np.average(times), std=np.std(times)))

        num_series = len(stats)
        offset = -float(num_series) / 2.0 + 0.5
        for i, (label, results) in enumerate(stats.items()):
            xs = [i + offset * BAR_WIDTH for i in range(len(results))]
            
            averages = [r['avg'] for r in results]
            errors = [r['std'] for r in results]

            ax.bar(x=xs, height=averages, yerr=errors, width=BAR_WIDTH, capsize=2, color=colors[i], label=label)

            for x, y in zip(xs, averages):
                ax.annotate(f'{y:.2f}', xy=(x, y), xytext=(x, y), xycoords='data', textcoords='offset points')

            offset += 1

        ax.set_xticks(list(range(len(cockroach_profiling))))

        ax.set_xlabel('Query Type')
        ax.set_ylabel('Execution Time (ms)')
        ax.set_title('Average Query Execution Times')

        ax.legend(fontsize='x-small')

        if output_file is not None:
            plt.savefig(output_file)
        else:
            plt.show()


if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument('--result-files', type=str, nargs='+')
    parser.add_argument('--cockroach-folder', type=str, required=True)
    parser.add_argument('--output-file', type=str)
    args = parser.parse_args()

    results: List[List[Dict[str, Any]]] = []
    for path in args.result_files:
        results.append(read_as_json(path))

    cockroach_profiling: List[Dict[str, Any]] = []
    for file_name in sorted(os.listdir(args.cockroach_folder)):
        path = os.path.join(args.cockroach_folder, file_name)
        cockroach_profiling.append(read_as_json(path)[0])

    plot_execution_times(results, cockroach_profiling, args.output_file)

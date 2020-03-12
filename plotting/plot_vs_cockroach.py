import os
import numpy as np
import matplotlib.pyplot as plt
import scipy.stats
from argparse import ArgumentParser
from collections import defaultdict
from typing import List, Dict, Any, Optional

from plot_utils import read_as_json


BAR_WIDTH = 0.12


def run_ttests(results: List[List[Dict[str, Any]]], cockroach_profiling: List[Dict[str, Any]], query_types: int):
    
    for query_type, profile_results in zip(query_types, cockroach_profiling):
        cockroach_results = profile_results['latency']

        cockroach_avg = np.average(cockroach_results)
        cockroach_std = np.std(cockroach_results)

        print(f'Query Type: {query_type}')

        for results_list in results:
            for opt_result in results_list:
                opt_name = opt_result['optimizer_name']
                opt_times = [r['elapsed_time'] for r in opt_result['stats'] if r['query_type'] == query_type]

                t_stat, p_value = scipy.stats.ttest_ind(cockroach_results, opt_times, equal_var=False)

                opt_avg = np.average(opt_times)
                opt_std = np.std(opt_times)
                perc_diff = (opt_avg - cockroach_avg) / cockroach_avg

                print(f'{opt_name} {opt_avg:.3f} ({opt_std:.3f}), Cockroach {cockroach_avg:.3f} ({cockroach_std:.3f}) -> T-stat: {t_stat:.3f}, P-value: {p_value:.3f}, %-Diff: {perc_diff:.3f}')

        print('==========')


def plot_execution_times(results: List[List[Dict[str, Any]]], cockroach_profiling: List[Dict[str, Any]], query_types: int, output_file: Optional[str]):
    with plt.style.context('ggplot'):
        
        fix, ax = plt.subplots()
        colors = plt.rcParams['axes.prop_cycle'].by_key()['color']

        stats = defaultdict(list)
        for query_type, profile_results in zip(query_types, cockroach_profiling):

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
        for i, (label, results) in enumerate(sorted(stats.items(), key=lambda t: t[0])):
            xs = [i + offset * BAR_WIDTH + 1 for i in range(len(results))]
            
            averages = [r['avg'] for r in results]
            errors = [r['std'] for r in results]

            ax.bar(x=xs, height=averages, yerr=errors, width=BAR_WIDTH, capsize=2, color=colors[i], label=label)

            for x, y in zip(xs, averages):
                ax.annotate(f'{y:.2f}', xy=(x, y), xytext=(x, y), xycoords='data', textcoords='offset points')

            offset += 1

        ax.set_xticks(list(range(1, len(cockroach_profiling) + 1)))

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
    parser.add_argument('--profiling-folder', type=str, required=True)
    parser.add_argument('--cockroach-folder', type=str, required=True)
    parser.add_argument('--output-file', type=str)
    args = parser.parse_args()

    results: List[List[Dict[str, Any]]] = []
    for path in args.result_files:
        results.append(read_as_json(path))

    input_query_files = list(sorted(os.listdir(args.profiling_folder)))
    query_types: List[int] = []

    cockroach_profiling: List[Dict[str, Any]] = []
    cockroach_files = list(sorted(os.listdir(args.cockroach_folder)))
    
    for file_name in cockroach_files:
        path = os.path.join(args.cockroach_folder, file_name)

        sql_file = file_name.replace('.json', '.sql')
        if sql_file in input_query_files:
            query_types.append(input_query_files.index(sql_file))
            cockroach_profiling.append(read_as_json(path)[0])

    plot_execution_times(results, cockroach_profiling, query_types, args.output_file)

    run_ttests(results, cockroach_profiling, query_types)

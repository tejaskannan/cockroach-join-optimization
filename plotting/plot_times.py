import matplotlib.pyplot as plt
import os
import numpy as np
from argparse import ArgumentParser
from collections import defaultdict
from typing import Dict, Any, List, Optional

from plot_utils import read_as_json



def plot_regret(results: List[List[Dict[str, Any]]], output_file: Optional[str], concatenate: bool):
    with plt.style.context('ggplot'):
        fig, ax = plt.subplots()

        optimizer_results: Dict[str, List[List[float]]] = defaultdict(list)

        if concatenate:
            concat_results: Dict[str, List[float]] = defaultdict(list)
            for result in results:
                for op_result in result:
                    times = [r['elapsed_time'] for r in op_result['stats']]
                    concat_results[op_result['optimizer_name']].extend(times)

            for opt_name, times in concat_results.items():
                cumulative_times = np.cumsum(times)
                avg_time = [c / float(t+1) for t, c in enumerate(cumulative_times)]
                optimizer_results[opt_name].append(avg_time)
        else:
            for result in results:
                for op_result in result:
                    times = [r['elapsed_time'] for r in op_result['stats']] 
                    cumulative_times = np.cumsum(times)
                    avg_times = [t / (i+1) for i, t in enumerate(cumulative_times)]

                    optimizer_results[op_result['optimizer_name']].append(avg_times)
        
        for optimizer_name, regret_lists in optimizer_results.items():
            regret_matrix = np.vstack(regret_lists)
            avg_regret = np.average(regret_matrix, axis=0)

            error = np.std(regret_matrix, axis=0)

            times = list(range(len(avg_regret)))
            ax.plot(times, avg_regret, label=optimizer_name)

            ax.fill_between(times, avg_regret - error, avg_regret + error, alpha=0.4)

        ax.set_xlabel('Time Step')
        ax.set_ylabel('Average Elapsed Time (ms)')
        ax.set_title('Average Query Latency for Each Optimizer')

        ax.legend()

        if output_file is not None:
            plt.savefig(output_file)
        else:
            plt.show()


if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument('--result-files', type=str, nargs='+')
    parser.add_argument('--output-file', type=str)
    parser.add_argument('--concatenate', action='store_true')
    args = parser.parse_args()

    results = []
    for path in args.result_files:
        results.append(read_as_json(path))

    plot_regret(results, args.output_file, args.concatenate)

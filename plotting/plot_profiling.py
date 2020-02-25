import os
import numpy as np
import matplotlib.pyplot as plt
from argparse import ArgumentParser
from typing import Dict, List, Any, Optional

from plot_utils import read_as_json, read_queries

BAR_WIDTH = 0.2


def plot_profiling(profile_results: List[List[Dict[str, Any]]], queries: List[List[str]], output_file: Optional[str]):
    with plt.style.context('ggplot'):

        fig, ax = plt.subplots()
        colors = plt.rcParams['axes.prop_cycle'].by_key()['color']

        for query_type, (order_results, query_orders) in enumerate(zip(profile_results, queries)):

            offset = -float(len(query_orders)) / 2.0 + 0.5
            for order_index, query in enumerate(query_orders):

                order_times = None
                for order_values in order_results: 
                    if order_values['query'].strip() == query:
                        order_times = order_values['latency']
                        break

                avg_time = np.average(order_times)
                std_time = np.std(order_times)

                ax.bar(x=query_type + offset * BAR_WIDTH + 1, height=avg_time, yerr=std_time, width=BAR_WIDTH, capsize=2, color=colors[order_index])

                offset += 1

        ax.set_xticks(list(range(1, len(queries) + 1)))

        ax.set_xlabel('Query Type')
        ax.set_ylabel('Average Execution Latency (ms)')
        ax.set_title('Average Query Latency')

        if output_file is not None:
            plt.savefig(output_file)
        else:
            plt.show()


if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument('--profiling-folder', type=str, required=True)
    parser.add_argument('--query-folder', type=str, required=True)
    parser.add_argument('--output-file', type=str)
    args = parser.parse_args()

    profile_results: List[List[Dict[str, Any]]] = []
    for file_name in sorted(os.listdir(args.profiling_folder)):
        path = os.path.join(args.profiling_folder, file_name)
        profile_results.append(read_as_json(path))

    queries: List[List[str]] = []
    for file_name in sorted(os.listdir(args.query_folder)):
        path = os.path.join(args.query_folder, file_name)
        queries.append(read_queries(path))

    plot_profiling(profile_results, queries, args.output_file)

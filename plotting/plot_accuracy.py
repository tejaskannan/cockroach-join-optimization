import matplotlib.pyplot as plt
import os
import numpy as np
from argparse import ArgumentParser
from collections import defaultdict
from typing import Dict, Any, List, Optional

from plot_utils import read_as_json


def plot_acc(results: List[List[Dict[str, Any]]], start_time: int, output_file: Optional[str], concatenate: bool):
    with plt.style.context('ggplot'):
        fig, ax = plt.subplots()

        optimizer_results: Dict[str, List[List[float]]] = defaultdict(list)

        if concatenate:
            concat_results: Dict[str, List[float]] = defaultdict(list)
            for result in results:
                for op_result in result:
                    correct = [float(r['arm'] == r['bestArm']) for r in op_result['stats']]
                    concat_results[op_result['optimizer_name']].extend(correct)

            for opt_name, correct in concat_results.items():
                correct_sums = np.cumsum(correct)
                avg_accuracy = [c / float(t+1) for t, c in enumerate(correct_sums)]
                optimizer_results[opt_name].append(avg_accuracy)
        else:
            for result in results:
                for op_result in result:
                    correct = [float(r['arm'] == r['bestArm']) for r in op_result['stats']] 
                    correct_sums = np.cumsum(correct)
                    avg_accuracy = [c / float(t+1) for t, c in enumerate(correct_sums)]

                    optimizer_results[op_result['optimizer_name']].append(avg_accuracy)

        for optimizer_name, accuracy_lists in optimizer_results.items():
            acc_matrix = np.array(accuracy_lists)
            avg_accuracy = np.average(acc_matrix, axis=0)[start_time:]

            error = np.std(acc_matrix, axis=0)[start_time:]

            times = list(range(len(avg_accuracy)))
            ax.plot(times, avg_accuracy, label=optimizer_name)

            ax.fill_between(times, avg_accuracy - error, avg_accuracy + error, alpha=0.4)

        ax.set_xlabel('Time Step')
        ax.set_ylabel('Average Accuracy')
        ax.set_title('Average Arm Accuracy for Each Optimizer')

        ax.legend()

        if output_file is not None:
            plt.savefig(output_file)
        else:
            plt.show()


if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument('--result-files', type=str, nargs='+')
    parser.add_argument('--start-time', type=int, default=0)
    parser.add_argument('--output-file', type=str)
    parser.add_argument('--concatenate', action='store_true')
    args = parser.parse_args()

    results = []
    for path in args.result_files:
        results.append(read_as_json(path))

    plot_acc(results, args.start_time, args.output_file, args.concatenate)

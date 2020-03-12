import matplotlib.pyplot as plt
import os
import numpy as np
from argparse import ArgumentParser
from math import ceil
from collections import defaultdict
from typing import Dict, Any, List, Optional

from plot_utils import read_as_json, moving_average


def plot_acc(results: List[List[Dict[str, Any]]], start_time: int, output_file: Optional[str], concatenate: bool, mode: str, window: int):
    with plt.style.context('ggplot'):
        fig, ax = plt.subplots()

        optimizer_results: Dict[str, List[List[float]]] = defaultdict(list)
        concat_points: List[int] = []

        if concatenate:
            concat_results: Dict[str, List[float]] = defaultdict(list)
            for result in results:
                for i, op_result in enumerate(result):
                    correct = [float(r['arm'] == r['best_arm']) for r in op_result['stats']]
                    concat_results[op_result['optimizer_name']].extend(correct)
                    
                    if i == 0:
                        concat_points.append(len(correct))
            
            concat_points = np.cumsum(concat_points[:-1])

            for opt_name, correct in concat_results.items():
                if mode == 'cumulative':
                    correct_sums = np.cumsum(correct)
                    avg_accuracy = [c / float(t+1) for t, c in enumerate(correct_sums)]
                else:
                    avg_accuracy = moving_average(correct, window)

                optimizer_results[opt_name].append(avg_accuracy)
        else:
            for result in results:
                for op_result in result:
                    correct = [float(r['arm'] == r['best_arm']) for r in op_result['stats']] 
                    
                    if mode == 'cumulative':
                        correct_sums = np.cumsum(correct)
                        avg_accuracy = [c / float(t+1) for t, c in enumerate(correct_sums)]
                    else:
                        avg_accuracy = moving_average(correct, window)

                    optimizer_results[op_result['optimizer_name']].append(avg_accuracy)

        averages: List[float] = []
        for optimizer_name, accuracy_lists in sorted(optimizer_results.items()):
            acc_matrix = np.array(accuracy_lists)
            avg_accuracy = np.average(acc_matrix, axis=0)[start_time:]

            error = np.std(acc_matrix, axis=0)[start_time:]

            times = list(range(len(avg_accuracy)))
            ax.plot(times, avg_accuracy, label=optimizer_name)

            ax.fill_between(times, avg_accuracy - error, avg_accuracy + error, alpha=0.4)

            averages.append(avg_accuracy[-1])

        last_y_pos = -1
        for i, avg_acc in enumerate(sorted(averages)):
            y_pos = max(last_y_pos + 0.05, avg_acc)
            ax.annotate(f'{avg_acc:.3f}', (times[-1], avg_acc), xycoords='data', xytext=(1.05 * times[-1], y_pos), color='black')
            last_y_pos = y_pos

        # Draw points to show where new queries are introduced
        ymin, ymax = ax.get_ylim()
        y_points = [0, 1.01]
        for concat_point in concat_points:
            x_values = [concat_point] * len(y_points)
            ax.plot(x_values, y_points, linestyle='--', linewidth=2, color='gray')
            ax.annotate('New Queries Added', (concat_point, y_points[0]), xycoords='data', xytext=(0.35 * concat_point, 0))

        ax.set_xlabel('Step')
        ax.set_ylabel('Average Accuracy')
        ax.set_title('Average Arm Accuracy for Each Optimizer')

        ax.legend(loc='lower right', fontsize='x-small')

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
    parser.add_argument('--mode', choices=['cumulative', 'moving'])
    parser.add_argument('--window', type=int, default=25)
    args = parser.parse_args()

    results = []
    for path in args.result_files:
        results.append(read_as_json(path))

    output_file = None
    if args.output_file is not None:
        folder, _name = os.path.split(args.result_files[0])
        output_file = os.path.join(folder, args.output_file)

    plot_acc(results, args.start_time, output_file, args.concatenate, args.mode, args.window)

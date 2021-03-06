import matplotlib.pyplot as plt
import os
import numpy as np
from math import ceil
from argparse import ArgumentParser
from collections import defaultdict
from typing import Dict, Any, List, Optional

from plot_utils import read_as_json, moving_average, write_as_json


SHIFT_UP = 30
SHIFT_DOWN = 20


def get_percentage_error(results: List[List[Dict[str, Any]]], error_file: Optional[str], concatenate: bool):
    optimizer_results: Dict[str, List[List[float]]] = defaultdict(list)
    oracle: List[List[float]] = []

    if concatenate:
        concat_results: Dict[str, List[float]] = defaultdict(list)
        concat_oracle: List[float] = []

        for result in results:
            for op_result in result:
                times = [r['elapsed_time'] for r in op_result['stats']]
                concat_results[op_result['optimizer_name']].extend(times)

            # Collect oracle results
            op_result = next(iter(result))
            concat_oracle.extend((r['best_time'] for r in op_result['stats']))

        for opt_name, times in concat_results.items():
            optimizer_results[opt_name].append(times)

        oracle.append(concat_oracle)
    else:
        for result in results:
            for op_result in result:
                times = [r['elapsed_time'] for r in op_result['stats']] 
                optimizer_results[op_result['optimizer_name']].append(times)
    
            # Collect oracle results
            op_result = next(iter(result))
            oracle.append([r['best_time'] for r in op_result['stats']])

    errors: Dict[str, float] = dict()
    oracle_matrix = np.vstack(oracle)
    for opt_name, times in optimizer_results.items():
        times_matrix = np.vstack(times)
        error = (times_matrix - oracle_matrix) / (oracle_matrix)
        avg_error = np.average(error)

        errors[opt_name] = avg_error

    if error_file is not None:
        write_as_json(errors, error_file)
    else:
        print(errors)


def plot_regret(results: List[List[Dict[str, Any]]], output_file: Optional[str], concatenate: bool, mode: str, window: int, start: int):
    with plt.style.context('ggplot'):
        fig, ax = plt.subplots()

        optimizer_results: Dict[str, List[List[float]]] = defaultdict(list)
        oracle: List[List[float]] = []
        concat_points: List[int] = []
    
        if concatenate:
            concat_results: Dict[str, List[float]] = defaultdict(list)
            concat_oracle: List[float] = []

            for result in results:
                for i, op_result in enumerate(result):
                    times = [r['elapsed_time'] for r in op_result['stats']]
                    concat_results[op_result['optimizer_name']].extend(times)

                    if i == 0:
                        concat_points.append(len(times))

                # Collect oracle results
                op_result = next(iter(result))
                concat_oracle.extend((r['best_time'] for r in op_result['stats']))

            concat_points = np.cumsum(concat_points)[:-1]

            for opt_name, times in concat_results.items():
                if mode == 'cumulative':
                    cumulative_times = np.cumsum(times)
                    avg_time = [c / float(t+1) for t, c in enumerate(cumulative_times)]
                else:
                    avg_time = moving_average(times, window)

                optimizer_results[opt_name].append(avg_time)

            if mode == 'cumulative':
                cumulative_times = np.cumsum(concat_oracle)
                avg_time = [c / float(t+1) for t, c in enumerate(cumulative_times)]
            else:
                avg_time = moving_average(concat_oracle, window)

            oracle.append(avg_time)
        else:
            for result in results:
                for op_result in result:
                    times = [r['elapsed_time'] for r in op_result['stats']] 
                    
                    if mode == 'cumulative':
                        cumulative_times = np.cumsum(times)
                        avg_times = [t / (i+1) for i, t in enumerate(cumulative_times)]
                    else:
                        avg_time = moving_average(times, window)

                    optimizer_results[op_result['optimizer_name']].append(avg_times)
        
                # Collect oracle results
                op_result = next(iter(result))
                oracle_result = [r['best_time'] for r in op_result['stats']]

                if mode == 'cumulative':
                    cumulative_times = np.cumsum(oracle_result)
                    avg_time = [c / float(t+1) for t, c in enumerate(cumulative_times)]
                else:
                    avg_time = moving_average(oracle_result, window)

                oracle.append(avg_time)
 
        averages: List[float] = []
        for optimizer_name, regret_lists in sorted(optimizer_results.items()):
            regret_matrix = np.vstack(regret_lists)
            avg_regret = np.average(regret_matrix, axis=0)

            error = np.std(regret_matrix, axis=0)

            avg_regret = avg_regret[start:]
            error = error[start:]

            times = list(range(len(avg_regret)))
            ax.plot(times, avg_regret, label=optimizer_name)

            ax.fill_between(times, avg_regret - error, avg_regret + error, alpha=0.4)
            averages.append(avg_regret[-1])

        oracle_matrix = np.vstack(oracle)
        oracle_avg = np.average(oracle_matrix, axis=0)[start:]
        times = list(range(len(oracle_avg)))
        ax.plot(times, oracle_avg, label='Oracle')
        averages.append(oracle_avg[-1])

        last_y_pos = -1
        for i, avg_time in enumerate(sorted(averages)):
            y_pos = max(last_y_pos + SHIFT_UP, avg_time - SHIFT_DOWN)
            ax.annotate(f'{avg_time:.2f}', (times[-1], avg_time), xycoords='data', xytext=(1.05 * times[-1], y_pos), color='black')
            last_y_pos = y_pos

        ymin, ymax = ax.get_ylim()
        y_points = list(range(0, int(ceil(ymax) + 1)))
        for concat_point in concat_points:
            x_values = [concat_point] * len(y_points)
            ax.plot(x_values, y_points, linestyle='--', linewidth=2, color='gray')
            ax.annotate('New Queries Added', (concat_point, y_points[0]), xycoords='data', xytext=(0.35 * concat_point, 0), color='black')

        ax.set_xlabel('Step')
        ax.set_ylabel('Average Elapsed Time (ms)')
        ax.set_title('Average Query Latency for Each Optimizer')

        ax.legend(fontsize='x-small')

        if output_file is not None:
            plt.savefig(output_file)
        else:
            plt.show()


if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument('--result-files', type=str, nargs='+')
    parser.add_argument('--error-file', type=str)
    parser.add_argument('--plot-file', type=str)
    parser.add_argument('--concatenate', action='store_true')
    parser.add_argument('--window', type=int, default=25)
    parser.add_argument('--mode', choices=['cumulative', 'moving'])
    parser.add_argument('--start', type=int, default=0)
    args = parser.parse_args()

    results = []
    for path in args.result_files:
        results.append(read_as_json(path))

    output_folder, _name = os.path.split(args.result_files[0])

    plot_file = None
    if args.plot_file is not None:
        plot_file = os.path.join(output_folder, args.plot_file)
    
    error_file = None
    if args.error_file is not None:
        error_file = os.path.join(output_folder, args.error_file)

    plot_regret(results, plot_file, args.concatenate, args.mode, args.window, args.start)
    get_percentage_error(results, error_file, args.concatenate)

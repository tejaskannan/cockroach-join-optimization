import matplotlib.pyplot as plt
import os
import numpy as np
from argparse import ArgumentParser
from collections import defaultdict
from typing import Dict, Any, List, Optional

from plot_utils import read_as_json, moving_average, write_as_json


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


def plot_regret(results: List[List[Dict[str, Any]]], output_file: Optional[str], field: str, concatenate: bool, mode: str, window: int, start: int):
    with plt.style.context('ggplot'):
        fig, ax = plt.subplots()

        optimizer_results: Dict[str, List[List[float]]] = defaultdict(list)
        oracle: List[List[float]] = []
    
        if concatenate:
            concat_results: Dict[str, List[float]] = defaultdict(list)
            concat_oracle: List[float] = []

            for result in results:
                for op_result in result:
                    times = [r[field] for r in op_result['stats']]
                    concat_results[op_result['optimizer_name']].extend(times)

                # Collect oracle results
                op_result = next(iter(result))
                concat_oracle.extend((r['best_time'] for r in op_result['stats']))

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
                    times = [r[field] for r in op_result['stats']] 
                    
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
 
        for optimizer_name, regret_lists in optimizer_results.items():
            regret_matrix = np.vstack(regret_lists)
            avg_regret = np.average(regret_matrix, axis=0)

            error = np.std(regret_matrix, axis=0)

            avg_regret = avg_regret[start:]
            error = error[start:]

            times = list(range(len(avg_regret)))
            ax.plot(times, avg_regret, label=optimizer_name)

            ax.fill_between(times, avg_regret - error, avg_regret + error, alpha=0.4)

        oracle_matrix = np.vstack(oracle)
        oracle_avg = np.average(oracle_matrix, axis=0)[start:]
        times = list(range(len(oracle_avg)))
        ax.plot(times, oracle_avg, label='Oracle')

        label_tokens = [t.capitalize() for t in field.split('_')]
        label = ' '.join(label_tokens)

        ax.set_xlabel('Step')
        ax.set_ylabel('Average {0}'.format(label))
        ax.set_title('Average {0} for Each Optimizer'.format(label))

        ax.legend()

        if output_file is not None:
            plt.savefig(output_file)
        else:
            plt.show()


if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument('--result-files', type=str, nargs='+')
    parser.add_argument('--output-folder', type=str)
    parser.add_argument('--field', type=str, required=True)
    parser.add_argument('--concatenate', action='store_true')
    parser.add_argument('--window', type=int, default=25)
    parser.add_argument('--mode', choices=['cumulative', 'moving'])
    parser.add_argument('--start', type=int, default=0)
    args = parser.parse_args()

    results = []
    for path in args.result_files:
        results.append(read_as_json(path))

    plot_file = os.path.join(args.output_folder, 'times.pdf') if args.output_folder is not None else None
    error_file = os.path.join(args.output_folder, 'percentage_error.json') if args.output_folder is not None else None

    plot_regret(results, plot_file, args.field, args.concatenate, args.mode, args.window, args.start)
    get_percentage_error(results, error_file, args.concatenate)

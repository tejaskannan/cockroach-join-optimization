import scipy.stats
from argparse import ArgumentParser
from typing import Dict, Any, List

from plot_utils import read_as_json, write_as_json


def run_ttests(results: Dict[str, Any], mode: str, output_file: str):
    seen = set()


    test_results: List[Dict[str, Any]] = []

    for series in results:
        for other_series in results:

            name = series['optimizer_name']
            other_name = other_series['optimizer_name']

            pair = (name, other_name) if name < other_name else (other_name, name)
            if pair in seen or name == other_name:
                continue

            if mode == 'time':
                values = [r['elapsed_time'] for r in series['stats']]
                other_values = [r['elapsed_time'] for r in other_series['stats']]
            elif mode == 'accuracy':
                value = [float(r['arm'] == r['best_arm']) for r in series['stats']]
                other_values = [float(r['arm'] == r['best_arm']) for r in other_series['stats']]

            t_stat, p_value = scipy.stats.ttest_ind(a=values, b=other_values, equal_var=False)

            test_results.append(dict(opt1=name, opt2=other_name, t_stat=t_stat, p_value=p_value))
            seen.add(pair)

    print(test_results)
    write_as_json(test_results, output_file)


if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument('--result-file', type=str, required=True)
    parser.add_argument('--mode', type=str, choices=['time', 'accuracy'])
    parser.add_argument('--output-file', type=str, required=True)
    args = parser.parse_args()

    results = read_as_json(args.result_file)

    run_ttests(results, args.mode, args.output_file)

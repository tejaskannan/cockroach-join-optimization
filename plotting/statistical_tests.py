import os
import scipy.stats
from argparse import ArgumentParser
from typing import Dict, Any, List, Set, Tuple, Optional

from plot_utils import read_as_json, write_as_json


def get_query_types(results: List[Dict[str, Any]]) -> List[int]:
    series_results = results[0]
    types = set((r['query_type'] for r in series_results['stats']))
    return list(sorted(types))


def run_ttests(results: List[Dict[str, Any]], mode: str, query_type: Optional[int]) -> List[Dict[str, Any]]:
    seen: Set[Tuple[str, str]] = set()

    test_results: List[Dict[str, Any]] = []

    for series in results:
        for other_series in results:

            name = series['optimizer_name']
            other_name = other_series['optimizer_name']

            pair = (name, other_name) if name < other_name else (other_name, name)
            if pair in seen or name == other_name:
                continue

            if mode == 'time':
                values = [r['elapsed_time'] for r in series['stats'] if query_type is None or r['query_type'] == query_type]
                other_values = [r['elapsed_time'] for r in other_series['stats'] if query_type is None or r['query_type'] == query_type]
            elif mode == 'accuracy':
                value = [float(r['arm'] == r['best_arm']) for r in series['stats'] if query_type is None or r['query_type'] == query_type]
                other_values = [float(r['arm'] == r['best_arm']) for r in other_series['stats'] if query_type is None or r['query_type'] == query_type]

            t_stat, p_value = scipy.stats.ttest_ind(a=values, b=other_values, equal_var=False)

            test_results.append(dict(opt1=name, opt2=other_name, t_stat=t_stat, p_value=p_value))
            seen.add(pair)

    if mode == 'time':
        for series in results:
            values = [r['elapsed_time'] for r in series['stats'] if query_type is None or r['query_type'] == query_type]
            oracle = [r['best_time'] for r in series['stats'] if query_type is None or r['query_type'] == query_type]

            t_stat, p_value = scipy.stats.ttest_ind(a=values, b=oracle, equal_var=False)

            test_results.append(dict(opt1=series['optimizer_name'], opt2='oracle', t_stat=t_stat, p_value=p_value))

    return test_results


if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument('--result-file', type=str, required=True)
    parser.add_argument('--mode', type=str, choices=['time', 'accuracy'])
    parser.add_argument('--output-file', type=str, required=True)
    args = parser.parse_args()

    results = read_as_json(args.result_file)

    query_types = get_query_types(results)

    # Save all test results
    test_results: Dict[str, List[Dict[str, Any]]] = dict()
    test_results['all'] = run_ttests(results, args.mode, None)
    for query_type in query_types:
        test_results[f'type-{query_type + 1}'] = run_ttests(results, args.mode, query_type)

    output_folder, _name = os.path.split(args.result_file)
    output_file = os.path.join(output_folder, args.output_file)

    write_as_json(test_results, output_file)

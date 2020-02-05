import matplotlib.pyplot as plt
import numpy as np
from argparse import ArgumentParser
from typing import Dict, List


def read_results(path: str) -> Dict[int, float]:
    result: Dict[int, float] = dict()
    with open(path, 'r') as f:
        for line in f:
            tokens = line.split(',')
            index = int(tokens[0])
            val = float(tokens[1])
            result[index] = val
    return result


def plot_results(results: List[Dict[str, float]], labels: List[str], output_file: str):
    with plt.style.context('ggplot'):
        fig, ax = plt.subplots()

        for series, label in zip(results, labels):
            indexes = [t for t, _ in series.items()]
            values = [v for _, v in series.items()]
            partial_sums = np.cumsum(values)
            avgs = [p / (i+1) for i, p in enumerate(partial_sums)]
            ax.plot(indexes, avgs, label=label)

        ax.set_title('Normalized Query Runtimes')
        ax.set_xlabel('Step')
        ax.set_ylabel('Normalized Runtime')
        ax.legend()

        plt.savefig(output_file)


if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument('--data-files', type=str, nargs='+')
    parser.add_argument('--labels', type=str, nargs='+')
    parser.add_argument('--output-file', type=str, required=True)
    args = parser.parse_args()

    results = list(map(read_results, args.data_files))
    plot_results(results, args.labels, args.output_file)


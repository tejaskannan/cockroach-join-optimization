import matplotlib.pyplot as plt
import os
import numpy as np
from argparse import ArgumentParser
from typing import Dict, Any, List, Optional

from plot_utils import read_as_json


def plot_acc(results: List[Dict[str, Any]], output_file: Optional[str]):
    with plt.style.context('ggplot'):
        fig, ax = plt.subplots()

        for op_result in results:
            correct = [int(r['arm'] == r['bestArm']) for r in op_result['stats']] 
            correct_sums = np.cumsum(correct)
            avg_accuracy = [c / float(t+1) for t, c in enumerate(correct_sums)]

            times = list(range(len(avg_accuracy)))
            ax.plot(times, avg_accuracy, label=op_result['optimizer_name'])

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
    parser.add_argument('--results-file', type=str, required=True)
    parser.add_argument('--output-file', type=str)
    args = parser.parse_args()

    results = read_as_json(args.results_file)

    plot_acc(results, args.output_file)

import matplotlib.pyplot as plt
import os
import numpy as np
from argparse import ArgumentParser
from typing import Dict, Any, List, Optional

from plot_utils import read_as_json



def plot_regret(results: List[Dict[str, Any]], output_file: Optional[str]):
    with plt.style.context('ggplot'):
        fig, ax = plt.subplots()

        for op_result in results:
            regret = [r['regret'] for r in op_result['stats']] 
            cumulative_regret = np.cumsum(regret)

            times = list(range(len(cumulative_regret)))
            ax.plot(times, cumulative_regret, label=op_result['optimizer_name'])

        ax.set_xlabel('Time Step')
        ax.set_ylabel('Cumulative Regret')
        ax.set_title('Normalized Regret for Each Optimizer')

        ax.legend()

        plt.show()


if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument('--results-file', type=str, required=True)
    parser.add_argument('--output-file', type=str)
    args = parser.parse_args()

    results = read_as_json(args.results_file)

    plot_regret(results, args.output_file)

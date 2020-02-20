import json
import numpy as np
from typing import Dict, Any, List


def read_as_json(path: str) -> Dict[str, Any]:
    with open(path, 'r') as f:
        return json.load(f)


def write_as_json(data: Dict[str, Any], path: str):
    with open(path, 'w') as f:
        json.dump(data, f)


def read_queries(path: str) -> List[str]:
    results: List[str] = []

    with open(path, 'r') as f:
        query = []
        for line in f:
            text = line.strip()
            if len(text) == 0:
                continue

            query.append(text)

            if text[-1] == ';':
                results.append(' '.join(query).strip())
                query = []

    return results


def moving_average(arr: List[float], window: int):
    sums = np.cumsum(arr)
    sums[window:] = sums[window:] - sums[:-window]
    return sums[window - 1:] / window

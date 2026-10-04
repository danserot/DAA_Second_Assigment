from pathlib import Path
import csv
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[1]
with (ROOT / "results/results.csv").open(encoding="utf-8", newline="") as source:
    rows = list(csv.DictReader(source))
if len(rows) != 36:
    raise ValueError("Expected 36 benchmark cases")
keys = {(r["workload"], r["variant"], r["structure"], int(r["n"])) for r in rows}
expected = set()
for n in (100, 1000, 10000, 100000):
    for structure in ("DynamicArray", "MyLinkedList"):
        for workload, variant in (("W1", "-"), ("W2", "-"), ("W3", "head"), ("W3", "middle")):
            expected.add((workload, variant, structure, n))
    expected.add(("W4", "-", "MinHeap", n))
if keys != expected:
    raise ValueError("Missing or unexpected CSV cases")
output = ROOT / "results/plots"
output.mkdir(parents=True, exist_ok=True)
for workload in ("W1", "W2", "W3", "W4"):
    selected = [r for r in rows if r["workload"] == workload]
    groups = sorted({(r["structure"], r["variant"]) for r in selected})
    fig, axes = plt.subplots(2, 2, figsize=(12, 7), constrained_layout=True)
    for ax, metric in zip(axes.flat, ("time_ms", "steps", "moves", "comparisons")):
        for structure, variant in groups:
            series = sorted((r for r in selected if r["structure"] == structure and r["variant"] == variant), key=lambda r: int(r["n"]))
            label = structure + (" / " + variant if variant != "-" else "")
            ax.plot([int(r["n"]) for r in series], [float(r[metric]) for r in series], marker="o", label=label)
        ax.set_xscale("log")
        ax.set_xlabel("Initial number of elements, n")
        ax.set_ylabel("Time (ms)" if metric == "time_ms" else metric.capitalize() + " (operations)")
        ax.set_title(metric)
        ax.grid(True, alpha=0.3)
        ax.legend(fontsize=8)
    fig.suptitle(workload + " - median of 5 runs after 2 warm-ups")
    fig.savefig(output / (workload + ".png"), dpi=160)
    plt.close(fig)
print("Created four figures in", output)

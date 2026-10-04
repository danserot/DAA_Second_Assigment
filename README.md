# DAA Assignment 2 - In-Memory Workload Engine

Artem Khloptsev, SE-2525.

## Requirements

- JDK 17 or newer; compilation targets Java 17.
- Maven 3.9 or newer (IntelliJ bundled Maven is also suitable).
- Python 3.12 or newer for the pinned plotting dependency.
- Internet access for the first Maven/Python dependency download.

## Run from the project root

```sh
mvn clean test
mvn compile exec:java
python -m pip install -r scripts/requirements.txt
python scripts/plot_results.py
```

IntelliJ: reload the Maven project, select the project JDK, run Maven `test`, then run `engine.Main`. Set the working directory to the repository root. A separately installed Maven is unnecessary when using the Maven tool window.

The benchmark writes `results/results.csv` (36 rows plus header). An alternative output path can be passed to `engine.Main`. Plotting reads the standard CSV path and creates W1.png through W4.png in `results/plots/`.

## Layout and contracts

`src/main/java/engine/structures`: primitive-int DynamicArray, singly linked MyLinkedList with head/tail, and array-based MinHeap. The first two share IntSequence. `metrics` contains per-instance long counters; `benchmark` contains workload generation, verification, warm-up, measurement and CSV export. Tests are in `src/test/java/engine`.

get/remove accept [0,size); indexed add accepts [0,size]. Invalid indices throw IndexOutOfBoundsException. Empty heap operations throw IllegalStateException. Array and heap double capacity when full. remove returns the removed value. Standard collections are used only in tests.

## Measurement protocol

For each n = 100, 1000, 10000, 100000, use Random(42), identical values and requests for both sequences. W1: 10000 random get calls. W2: 1000 contains queries, exactly half present; negative absent queries cannot occur in the nonnegative input. W3: 1000 insertions followed by 1000 removals, at zero or the fixed original n/2. W4: n heap insertions and n extractions. Each case uses a new structure, two discarded warm-ups and five measured runs; CSV records the median-time run and its counters.

W1-W3 exclude initial filling and verification. W4 includes insertion, extraction and output-array writes, but excludes output-array allocation and validation. All counters are inside structure methods and included in elapsed time. See REPORT.md for definitions and limitations.

## Git history

main contains the final working project. Existing commits were preserved without rebase: feature/array ends after array tests; feature/list after list tests; feature/heap after heap tests; feature/metrics contains the benchmark and completion commits. Their histories share earlier stages because the original commits were sequential. A backup branch and external Git bundle preserve the pre-completion state. Tag v1.0 identifies the final release.

## Submission

Use the ZIP named DAA_Assignment2_Artem_Khloptsev_SE-2525.zip and include the repository link: https://github.com/danserot/DAA_Second_Assigment . Explain every submitted line during the defense; the assignment limits AI use to debugging and concept explanations. Bonus JOL measurements and Floyd buildHeap are not included.

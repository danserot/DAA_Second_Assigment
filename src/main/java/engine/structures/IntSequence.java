package engine.structures;

import engine.metrics.OperationMetrics;

public interface IntSequence {
    int size();

    void add(int value);

    void add(int index, int value);

    int remove(int index);

    int get(int index);

    boolean contains(int value);

    OperationMetrics metrics();
}
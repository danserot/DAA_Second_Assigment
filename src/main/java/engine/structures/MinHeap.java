package engine.structures;

import engine.metrics.OperationMetrics;

public final class MinHeap {
    private static final int INITIAL_CAPACITY = 8;

    private int[] data = new int[INITIAL_CAPACITY];
    private int size;

    private final OperationMetrics metrics = new OperationMetrics();

    public int size() {
        return size;
    }

    public OperationMetrics metrics() {
        return metrics;
    }

    public void insert(int value) {
        ensureCapacity();

        int index = size;
        data[index] = value;
        size++;

        bubbleUp(index);
    }

    public int peekMin() {
        checkNotEmpty();
        return read(0);
    }

    public int extractMin() {
        checkNotEmpty();

        int minimum = read(0);
        size--;

        if (size > 0) {
            data[0] = read(size);
            metrics.recordMove();

            bubbleDown(0);
        }

        return minimum;
    }

    private void bubbleUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;

            if (!isLess(index, parent)) {
                return;
            }

            swap(index, parent);
            index = parent;
        }
    }

    private void bubbleDown(int index) {
        // Узлы с индексами >= size / 2 являются листьями.
        while (index < size / 2) {
            int left = 2 * index + 1;
            int right = left + 1;
            int smallestChild = left;

            if (right < size && isLess(right, left)) {
                smallestChild = right;
            }

            if (!isLess(smallestChild, index)) {
                return;
            }

            swap(index, smallestChild);
            index = smallestChild;
        }
    }

    private boolean isLess(int firstIndex, int secondIndex) {
        int first = read(firstIndex);
        int second = read(secondIndex);

        metrics.recordComparison();
        return first < second;
    }

    private void swap(int firstIndex, int secondIndex) {
        int first = read(firstIndex);
        int second = read(secondIndex);

        data[firstIndex] = second;
        metrics.recordMove();

        data[secondIndex] = first;
        metrics.recordMove();
    }

    private int read(int index) {
        metrics.recordStep();
        return data[index];
    }

    private void ensureCapacity() {
        if (size < data.length) {
            return;
        }

        int newCapacity = Math.multiplyExact(data.length, 2);
        int[] expanded = new int[newCapacity];

        for (int i = 0; i < size; i++) {
            expanded[i] = read(i);
            metrics.recordMove();
        }

        data = expanded;
    }

    private void checkNotEmpty() {
        if (size == 0) {
            throw new IllegalStateException("Heap is empty");
        }
    }

    /**
     * Проверка свойства кучи для тестов.
     * Не изменяет счётчики измеряемых операций.
     */
    public boolean isValidHeap() {
        for (int child = 1; child < size; child++) {
            int parent = (child - 1) / 2;

            if (data[parent] > data[child]) {
                return false;
            }
        }

        return true;
    }
}
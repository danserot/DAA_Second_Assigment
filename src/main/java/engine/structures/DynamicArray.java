package engine.structures;

import engine.metrics.OperationMetrics;

public final class DynamicArray implements IntSequence {
    private static final int INITIAL_CAPACITY = 8;

    private int[] data = new int[INITIAL_CAPACITY];
    private int size;

    private final OperationMetrics metrics = new OperationMetrics();

    @Override
    public int size() {
        return size;
    }

    @Override
    public OperationMetrics metrics() {
        return metrics;
    }

    @Override
    public void add(int value) {
        add(size, value);
    }

    @Override
    public void add(int index, int value) {
        checkInsertionIndex(index);
        ensureCapacity();

        // Сдвигаем справа налево, чтобы не затереть элементы.
        for (int i = size; i > index; i--) {
            data[i] = read(i - 1);
            metrics.recordMove();
        }

        data[index] = value;
        size++;
    }

    @Override
    public int remove(int index) {
        checkElementIndex(index);

        int removed = read(index);

        // Заполняем освободившееся место сдвигом влево.
        for (int i = index; i < size - 1; i++) {
            data[i] = read(i + 1);
            metrics.recordMove();
        }

        size--;
        return removed;
    }

    @Override
    public int get(int index) {
        checkElementIndex(index);
        return read(index);
    }

    @Override
    public boolean contains(int value) {
        for (int i = 0; i < size; i++) {
            int current = read(i);
            metrics.recordComparison();

            if (current == value) {
                return true;
            }
        }

        return false;
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

    private void checkElementIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                    "index=" + index + ", size=" + size
            );
        }
    }

    private void checkInsertionIndex(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(
                    "index=" + index + ", size=" + size
            );
        }
    }
}
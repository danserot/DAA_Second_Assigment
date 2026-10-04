package engine.structures;

import engine.metrics.OperationMetrics;

public final class MyLinkedList implements IntSequence {
    private static final class Node {
        private final int value;
        private Node next;

        private Node(int value) {
            this.value = value;
        }
    }

    private Node head;
    private Node tail;
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
        Node node = new Node(value);

        if (size == 0) {
            head = node;
            metrics.recordMove();
        } else {
            tail.next = node;
            metrics.recordMove();
        }

        tail = node;
        metrics.recordMove();
        size++;
    }

    @Override
    public void add(int index, int value) {
        checkInsertionIndex(index);

        if (index == size) {
            add(value);
            return;
        }

        Node node = new Node(value);

        if (index == 0) {
            node.next = head;
            metrics.recordMove();

            head = node;
            metrics.recordMove();
        } else {
            Node previous = nodeAt(index - 1);

            node.next = nextNode(previous);
            metrics.recordMove();

            previous.next = node;
            metrics.recordMove();
        }

        size++;
    }

    @Override
    public int remove(int index) {
        checkElementIndex(index);

        Node removed;

        if (index == 0) {
            removed = head;

            head = nextNode(head);
            metrics.recordMove();

            if (size == 1) {
                tail = null;
                metrics.recordMove();
            }
        } else {
            Node previous = nodeAt(index - 1);
            removed = nextNode(previous);

            previous.next = nextNode(removed);
            metrics.recordMove();

            if (removed == tail) {
                tail = previous;
                metrics.recordMove();
            }
        }

        size--;
        return removed.value;
    }

    @Override
    public int get(int index) {
        checkElementIndex(index);
        return nodeAt(index).value;
    }

    @Override
    public boolean contains(int value) {
        Node current = head;

        while (current != null) {
            metrics.recordComparison();

            if (current.value == value) {
                return true;
            }

            current = nextNode(current);
        }

        return false;
    }

    private Node nodeAt(int index) {
        Node current = head;

        for (int i = 0; i < index; i++) {
            current = nextNode(current);
        }

        return current;
    }

    private Node nextNode(Node node) {
        metrics.recordStep();
        return node.next;
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
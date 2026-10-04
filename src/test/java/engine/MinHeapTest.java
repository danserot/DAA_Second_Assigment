package engine;

import engine.structures.MinHeap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MinHeapTest {

    @Test
    void emptyHeapThrowsExceptions() {
        MinHeap heap = new MinHeap();

        assertEquals(0, heap.size());
        assertTrue(heap.isValidHeap());

        assertThrows(IllegalStateException.class, heap::peekMin);
        assertThrows(IllegalStateException.class, heap::extractMin);

        assertEquals(0, heap.size());
    }

    @Test
    void supportsOneElementAndReuseAfterExtraction() {
        MinHeap heap = new MinHeap();

        heap.insert(42);

        assertEquals(1, heap.size());
        assertEquals(42, heap.peekMin());
        assertTrue(heap.isValidHeap());

        assertEquals(42, heap.extractMin());
        assertEquals(0, heap.size());
        assertTrue(heap.isValidHeap());

        heap.insert(7);

        assertEquals(7, heap.extractMin());
        assertEquals(0, heap.size());
    }

    @Test
    void peekDoesNotRemoveMinimum() {
        MinHeap heap = new MinHeap();

        heap.insert(30);
        heap.insert(10);
        heap.insert(20);

        assertEquals(10, heap.peekMin());
        assertEquals(10, heap.peekMin());
        assertEquals(3, heap.size());
        assertTrue(heap.isValidHeap());
    }

    @Test
    void supportsDuplicatesAndExtremeValues() {
        MinHeap heap = new MinHeap();

        int[] input = {
                7,
                Integer.MAX_VALUE,
                7,
                0,
                Integer.MIN_VALUE,
                -10
        };

        for (int value : input) {
            heap.insert(value);
            assertTrue(heap.isValidHeap());
        }

        int[] expected = input.clone();
        Arrays.sort(expected);

        for (int value : expected) {
            assertEquals(value, heap.extractMin());
            assertTrue(heap.isValidHeap());
        }

        assertEquals(0, heap.size());
    }

    @Test
    void handlesNodeWithOnlyLeftChild() {
        MinHeap heap = new MinHeap();

        heap.insert(1);
        heap.insert(2);
        heap.insert(3);

        assertEquals(1, heap.extractMin());
        assertTrue(heap.isValidHeap());

        assertEquals(2, heap.extractMin());
        assertTrue(heap.isValidHeap());

        assertEquals(3, heap.extractMin());
        assertTrue(heap.isValidHeap());
    }

    @Test
    void bubbleDownChoosesSmallerRightChild() {
        MinHeap heap = new MinHeap();

        heap.insert(1);
        heap.insert(5);
        heap.insert(2);
        heap.insert(9);

        assertEquals(1, heap.extractMin());
        assertTrue(heap.isValidHeap());
        assertEquals(2, heap.peekMin());

        assertEquals(2, heap.extractMin());
        assertEquals(5, heap.extractMin());
        assertEquals(9, heap.extractMin());
        assertEquals(0, heap.size());
    }

    @Test
    void growsAndExtractsValuesInSortedOrder() {
        MinHeap heap = new MinHeap();
        Random random = new Random(42);
        int[] expected = new int[2_000];

        for (int i = 0; i < expected.length; i++) {
            expected[i] = random.nextInt();
            heap.insert(expected[i]);

            assertEquals(i + 1, heap.size());
            assertTrue(heap.isValidHeap(), "Invalid heap after insert " + i);
        }

        Arrays.sort(expected);

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], heap.extractMin());
            assertEquals(expected.length - i - 1, heap.size());
            assertTrue(heap.isValidHeap(), "Invalid heap after extract " + i);
        }
    }

    @Test
    void randomOperationsMatchPriorityQueue() {
        MinHeap actual = new MinHeap();
        PriorityQueue<Integer> expected = new PriorityQueue<>();
        Random random = new Random(42);

        for (int operation = 0; operation < 5_000; operation++) {
            if (expected.isEmpty() || random.nextBoolean()) {
                int value = random.nextInt(201) - 100;

                actual.insert(value);
                expected.add(value);
            } else {
                int minimum = expected.remove();

                assertEquals(minimum, actual.extractMin());
            }

            assertEquals(expected.size(), actual.size());
            assertTrue(
                    actual.isValidHeap(),
                    "Invalid heap after operation " + operation
            );

            if (!expected.isEmpty()) {
                assertEquals(expected.peek().intValue(), actual.peekMin());
            }
        }

        while (!expected.isEmpty()) {
            assertEquals(expected.remove().intValue(), actual.extractMin());
            assertEquals(expected.size(), actual.size());
            assertTrue(actual.isValidHeap());
        }

        assertEquals(0, actual.size());
        assertThrows(IllegalStateException.class, actual::extractMin);
    }

    @Test
    void countsReadsComparisonsAndMoves() {
        MinHeap heap = new MinHeap();

        heap.insert(20);
        assertMetrics(heap, 0, 0, 0);

        heap.metrics().reset();
        heap.insert(10);
        assertMetrics(heap, 4, 2, 1);

        heap.metrics().reset();
        assertEquals(10, heap.peekMin());
        assertMetrics(heap, 1, 0, 0);

        heap.metrics().reset();
        assertEquals(10, heap.extractMin());
        assertMetrics(heap, 2, 1, 0);

        heap.metrics().reset();
        assertEquals(20, heap.extractMin());
        assertMetrics(heap, 1, 0, 0);

        heap.metrics().reset();
        assertMetrics(heap, 0, 0, 0);
    }

    @Test
    void countsCopiesDuringGrowth() {
        MinHeap heap = new MinHeap();

        for (int i = 0; i < 8; i++) {
            heap.insert(i);
        }

        heap.metrics().reset();
        heap.insert(8);

        // 8 чтений при расширении + 2 чтения при сравнении с родителем.
        assertMetrics(heap, 10, 8, 1);
        assertEquals(9, heap.size());
        assertTrue(heap.isValidHeap());
    }

    @Test
    void heapValidationDoesNotChangeCounters() {
        MinHeap heap = new MinHeap();

        heap.insert(20);
        heap.insert(10);
        heap.insert(30);

        heap.metrics().reset();

        assertTrue(heap.isValidHeap());
        assertMetrics(heap, 0, 0, 0);
    }

    private static void assertMetrics(
            MinHeap heap,
            long steps,
            long moves,
            long comparisons
    ) {
        assertEquals(steps, heap.metrics().getSteps(), "steps");
        assertEquals(moves, heap.metrics().getMoves(), "moves");
        assertEquals(
                comparisons,
                heap.metrics().getComparisons(),
                "comparisons"
        );
    }
}
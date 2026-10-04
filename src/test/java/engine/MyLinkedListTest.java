package engine;

import engine.structures.MyLinkedList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MyLinkedListTest {

    @Test
    void emptyListHasZeroSize() {
        MyLinkedList list = new MyLinkedList();

        assertEquals(0, list.size());
        assertFalse(list.contains(10));

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, 10));
    }

    @Test
    void supportsOneElementAndReuseAfterRemoval() {
        MyLinkedList list = new MyLinkedList();

        list.add(0, 42);

        assertEquals(1, list.size());
        assertEquals(42, list.get(0));
        assertTrue(list.contains(42));

        assertEquals(42, list.remove(0));
        assertEquals(0, list.size());
        assertFalse(list.contains(42));

        list.add(7);
        list.add(8);

        assertContents(list, 7, 8);
    }

    @Test
    void appendsElementsInOrder() {
        MyLinkedList list = new MyLinkedList();

        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

        assertEquals(100, list.size());

        for (int i = 0; i < 100; i++) {
            assertEquals(i, list.get(i));
        }
    }

    @Test
    void insertsAtBeginningMiddleAndEnd() {
        MyLinkedList list = new MyLinkedList();

        list.add(20);
        list.add(40);

        list.add(0, 10);
        list.add(2, 30);
        list.add(list.size(), 50);

        assertContents(list, 10, 20, 30, 40, 50);
    }

    @Test
    void removesAtBeginningMiddleAndEnd() {
        MyLinkedList list = new MyLinkedList();

        for (int value = 10; value <= 50; value += 10) {
            list.add(value);
        }

        assertEquals(10, list.remove(0));
        assertEquals(30, list.remove(1));
        assertEquals(50, list.remove(list.size() - 1));

        assertContents(list, 20, 40);
    }

    @Test
    void updatesTailAfterRemovingLastElement() {
        MyLinkedList list = new MyLinkedList();

        list.add(10);
        list.add(20);
        list.add(30);

        assertEquals(30, list.remove(2));

        list.add(40);

        assertContents(list, 10, 20, 40);
    }

    @Test
    void supportsDuplicatesAndExtremeValues() {
        MyLinkedList list = new MyLinkedList();

        list.add(Integer.MIN_VALUE);
        list.add(7);
        list.add(7);
        list.add(Integer.MAX_VALUE);

        assertTrue(list.contains(Integer.MIN_VALUE));
        assertTrue(list.contains(Integer.MAX_VALUE));
        assertFalse(list.contains(0));

        assertEquals(7, list.remove(1));
        assertTrue(list.contains(7));

        assertEquals(7, list.remove(1));
        assertFalse(list.contains(7));

        assertContents(list, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    void invalidOperationsDoNotChangeContents() {
        MyLinkedList list = new MyLinkedList();
        list.add(10);
        list.add(20);

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));

        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(2));

        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 30));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(3, 30));

        assertContents(list, 10, 20);
    }

    @Test
    void randomOperationsMatchArrayList() {
        MyLinkedList actual = new MyLinkedList();
        ArrayList<Integer> expected = new ArrayList<>();
        Random random = new Random(42);

        for (int operation = 0; operation < 2_000; operation++) {
            int value = random.nextInt(101) - 50;

            switch (random.nextInt(5)) {
                case 0 -> {
                    actual.add(value);
                    expected.add(value);
                }
                case 1 -> {
                    int index = random.nextInt(expected.size() + 1);
                    actual.add(index, value);
                    expected.add(index, value);
                }
                case 2 -> {
                    if (!expected.isEmpty()) {
                        int index = random.nextInt(expected.size());
                        int removed = expected.remove(index);

                        assertEquals(removed, actual.remove(index));
                    }
                }
                case 3 -> {
                    if (!expected.isEmpty()) {
                        int index = random.nextInt(expected.size());

                        assertEquals(
                                expected.get(index).intValue(),
                                actual.get(index)
                        );
                    }
                }
                case 4 -> assertEquals(
                        expected.contains(value),
                        actual.contains(value)
                );
            }

            assertEquals(expected.size(), actual.size());

            for (int i = 0; i < expected.size(); i++) {
                assertEquals(
                        expected.get(i).intValue(),
                        actual.get(i),
                        "Wrong value at index " + i
                );
            }
        }
    }

    @Test
    void countsTraversalAndSearchComparisons() {
        MyLinkedList list = new MyLinkedList();
        list.add(10);
        list.add(20);
        list.add(30);

        list.metrics().reset();
        assertEquals(10, list.get(0));
        assertMetrics(list, 0, 0, 0);

        list.metrics().reset();
        assertEquals(30, list.get(2));
        assertMetrics(list, 2, 0, 0);

        list.metrics().reset();
        assertTrue(list.contains(20));
        assertMetrics(list, 1, 0, 2);

        list.metrics().reset();
        assertFalse(list.contains(99));
        assertMetrics(list, 3, 0, 3);

        list.metrics().reset();
        assertMetrics(list, 0, 0, 0);
    }

    @Test
    void countsLinkUpdatesDuringInsertionAndRemoval() {
        MyLinkedList list = new MyLinkedList();

        list.add(10);
        assertMetrics(list, 0, 2, 0);

        list.metrics().reset();
        list.add(20);
        assertMetrics(list, 0, 2, 0);

        list.metrics().reset();
        list.add(0, 5);
        assertMetrics(list, 0, 2, 0);

        list.metrics().reset();
        list.add(2, 15);
        assertMetrics(list, 2, 2, 0);

        list.metrics().reset();
        assertEquals(15, list.remove(2));
        assertMetrics(list, 3, 1, 0);

        list.metrics().reset();
        assertEquals(5, list.remove(0));
        assertMetrics(list, 1, 1, 0);

        list.metrics().reset();
        assertEquals(20, list.remove(1));
        assertMetrics(list, 2, 2, 0);

        list.metrics().reset();
        assertEquals(10, list.remove(0));
        assertMetrics(list, 1, 2, 0);

        assertEquals(0, list.size());
    }

    private static void assertContents(
            MyLinkedList list,
            int... expected
    ) {
        assertEquals(expected.length, list.size());

        for (int i = 0; i < expected.length; i++) {
            assertEquals(
                    expected[i],
                    list.get(i),
                    "Wrong value at index " + i
            );
        }
    }

    private static void assertMetrics(
            MyLinkedList list,
            long steps,
            long moves,
            long comparisons
    ) {
        assertEquals(steps, list.metrics().getSteps(), "steps");
        assertEquals(moves, list.metrics().getMoves(), "moves");
        assertEquals(
                comparisons,
                list.metrics().getComparisons(),
                "comparisons"
        );
    }
}
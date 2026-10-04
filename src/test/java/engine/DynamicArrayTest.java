package engine;

import engine.structures.DynamicArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DynamicArrayTest {

    @Test
    void emptyArrayHasZeroSize() {
        DynamicArray array = new DynamicArray();

        assertEquals(0, array.size());
        assertFalse(array.contains(10));
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> array.remove(0));
    }

    @Test
    void supportsOneElementAndReuseAfterRemoval() {
        DynamicArray array = new DynamicArray();

        array.add(42);

        assertEquals(1, array.size());
        assertEquals(42, array.get(0));
        assertTrue(array.contains(42));
        assertEquals(42, array.remove(0));
        assertEquals(0, array.size());

        array.add(7);

        assertEquals(1, array.size());
        assertEquals(7, array.get(0));
    }

    @Test
    void growsWithoutLosingElements() {
        DynamicArray array = new DynamicArray();

        for (int i = 0; i < 1_000; i++) {
            array.add(i);
        }

        assertEquals(1_000, array.size());

        for (int i = 0; i < 1_000; i++) {
            assertEquals(i, array.get(i));
        }
    }

    @Test
    void insertsAtBeginningMiddleAndEnd() {
        DynamicArray array = new DynamicArray();

        array.add(20);
        array.add(40);
        array.add(0, 10);
        array.add(2, 30);
        array.add(array.size(), 50);

        assertContents(array, 10, 20, 30, 40, 50);
    }

    @Test
    void removesAtBeginningMiddleAndEnd() {
        DynamicArray array = new DynamicArray();

        for (int value = 10; value <= 50; value += 10) {
            array.add(value);
        }

        assertEquals(10, array.remove(0));
        assertEquals(30, array.remove(1));
        assertEquals(50, array.remove(array.size() - 1));

        assertContents(array, 20, 40);
    }

    @Test
    void supportsDuplicatesAndExtremeValues() {
        DynamicArray array = new DynamicArray();

        array.add(Integer.MIN_VALUE);
        array.add(7);
        array.add(7);
        array.add(Integer.MAX_VALUE);

        assertTrue(array.contains(Integer.MIN_VALUE));
        assertTrue(array.contains(Integer.MAX_VALUE));
        assertFalse(array.contains(0));

        assertEquals(7, array.remove(1));
        assertTrue(array.contains(7));

        assertEquals(7, array.remove(1));
        assertFalse(array.contains(7));

        assertContents(array, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    void invalidOperationsDoNotChangeContents() {
        DynamicArray array = new DynamicArray();
        array.add(10);
        array.add(20);

        assertThrows(IndexOutOfBoundsException.class, () -> array.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> array.remove(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> array.remove(2));
        assertThrows(IndexOutOfBoundsException.class, () -> array.add(-1, 30));
        assertThrows(IndexOutOfBoundsException.class, () -> array.add(3, 30));

        assertContents(array, 10, 20);
    }

    @Test
    void randomOperationsMatchArrayList() {
        DynamicArray actual = new DynamicArray();
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
                        assertEquals(expected.get(index).intValue(), actual.get(index));
                    }
                }
                case 4 ->
                        assertEquals(expected.contains(value), actual.contains(value));
            }

            assertEquals(expected.size(), actual.size());

            for (int i = 0; i < expected.size(); i++) {
                assertEquals(expected.get(i).intValue(), actual.get(i));
            }
        }
    }

    @Test
    void countsReadsShiftsAndValueComparisons() {
        DynamicArray array = new DynamicArray();
        array.add(10);
        array.add(20);
        array.add(30);

        array.metrics().reset();
        assertEquals(20, array.get(1));
        assertMetrics(array, 1, 0, 0);

        array.metrics().reset();
        array.add(1, 15);
        assertMetrics(array, 2, 2, 0);

        array.metrics().reset();
        assertEquals(15, array.remove(1));
        assertMetrics(array, 3, 2, 0);

        array.metrics().reset();
        assertTrue(array.contains(20));
        assertMetrics(array, 2, 0, 2);

        array.metrics().reset();
        assertFalse(array.contains(99));
        assertMetrics(array, 3, 0, 3);

        array.metrics().reset();
        assertMetrics(array, 0, 0, 0);
    }

    @Test
    void countsCopiesDuringGrowth() {
        DynamicArray array = new DynamicArray();

        for (int i = 0; i < 8; i++) {
            array.add(i);
        }

        array.metrics().reset();
        array.add(8);

        assertMetrics(array, 8, 8, 0);
        assertContents(array, 0, 1, 2, 3, 4, 5, 6, 7, 8);
    }

    private static void assertContents(DynamicArray array, int... expected) {
        assertEquals(expected.length, array.size());

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], array.get(i), "Wrong value at index " + i);
        }
    }

    private static void assertMetrics(
            DynamicArray array,
            long steps,
            long moves,
            long comparisons
    ) {
        assertEquals(steps, array.metrics().getSteps(), "steps");
        assertEquals(moves, array.metrics().getMoves(), "moves");
        assertEquals(comparisons, array.metrics().getComparisons(), "comparisons");
    }
}
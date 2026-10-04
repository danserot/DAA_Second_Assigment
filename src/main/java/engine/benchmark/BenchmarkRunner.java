package engine.benchmark;

import engine.metrics.OperationMetrics;
import engine.structures.DynamicArray;
import engine.structures.IntSequence;
import engine.structures.MinHeap;
import engine.structures.MyLinkedList;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public final class BenchmarkRunner {
    private static final int[] SIZES = {
            100, 1_000, 10_000, 100_000
    };

    private static final int ACCESS_COUNT = 10_000;
    private static final int SEARCH_COUNT = 1_000;
    private static final int CHANGE_COUNT = 1_000;

    private static final int WARMUP_RUNS = 2;
    private static final int MEASURED_RUNS = 5;

    // Делает результат вычислений наблюдаемым вне измеряемого участка.
    private static volatile long checksumSink;

    private enum Structure {
        ARRAY("DynamicArray"),
        LIST("MyLinkedList"),
        HEAP("MinHeap");

        private final String label;

        Structure(String label) {
            this.label = label;
        }
    }

    private enum Workload {
        W1, W2, W3, W4
    }

    private enum Variant {
        NONE("-"),
        HEAD("head"),
        MIDDLE("middle");

        private final String label;

        Variant(String label) {
            this.label = label;
        }
    }

    private record Input(
            int[] values,
            int[] indices,
            int[] queries
    ) {}

    private record Measurement(
            long timeNanos,
            long steps,
            long moves,
            long comparisons,
            long checksum
    ) {}

    private BenchmarkRunner() {}

    public static void main(String[] args) throws IOException {
        Path output = Path.of(
                args.length == 0 ? "results/results.csv" : args[0]
        );

        Files.createDirectories(output.toAbsolutePath().getParent());

        try (PrintWriter csv = new PrintWriter(
                Files.newBufferedWriter(output, StandardCharsets.UTF_8)
        )) {
            csv.println(
                    "workload,variant,structure,n,time_ms,steps,moves,comparisons"
            );

            for (int n : SIZES) {
                Input input = createInput(n);

                for (Structure structure :
                        new Structure[]{Structure.ARRAY, Structure.LIST}) {

                    writeCase(csv, Workload.W1, Variant.NONE, structure, input);
                    writeCase(csv, Workload.W2, Variant.NONE, structure, input);
                    writeCase(csv, Workload.W3, Variant.HEAD, structure, input);
                    writeCase(csv, Workload.W3, Variant.MIDDLE, structure, input);
                }

                writeCase(
                        csv,
                        Workload.W4,
                        Variant.NONE,
                        Structure.HEAP,
                        input
                );

                System.out.println("Completed n=" + n);
            }

            if (csv.checkError()) {
                throw new IOException("Failed to write CSV");
            }
        }

        System.out.println("Results: " + output.toAbsolutePath());
        System.out.println("Checksum: " + checksumSink);
    }

    private static Input createInput(int n) {
        Random random = new Random(42);

        int[] values = new int[n];

        for (int i = 0; i < n; i++) {
            values[i] = random.nextInt(1_000_000);
        }

        int[] indices = new int[ACCESS_COUNT];

        for (int i = 0; i < indices.length; i++) {
            indices[i] = random.nextInt(n);
        }

        int[] queries = new int[SEARCH_COUNT];

        for (int i = 0; i < queries.length; i++) {
            if (i % 2 == 0) {
                queries[i] = values[random.nextInt(n)];
            } else {
                // Данные неотрицательные, поэтому эти значения отсутствуют.
                queries[i] = -1 - random.nextInt(1_000_000);
            }
        }

        return new Input(values, indices, queries);
    }

    private static void writeCase(
            PrintWriter csv,
            Workload workload,
            Variant variant,
            Structure structure,
            Input input
    ) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            run(workload, variant, structure, input);
        }

        Measurement[] measurements = new Measurement[MEASURED_RUNS];

        for (int i = 0; i < measurements.length; i++) {
            measurements[i] = run(workload, variant, structure, input);
        }

        checkRepeatability(measurements);

        Measurement median = selectMedian(measurements);

        csv.println(
                workload.name() + ","
                        + variant.label + ","
                        + structure.label + ","
                        + input.values().length + ","
                        + Double.toString(median.timeNanos() / 1_000_000.0) + ","
                        + median.steps() + ","
                        + median.moves() + ","
                        + median.comparisons()
        );

        csv.flush();

        System.out.println(
                workload + " "
                        + variant.label + " "
                        + structure.label + " "
                        + input.values().length + ": "
                        + median.timeNanos() / 1_000_000.0 + " ms"
        );
    }

    private static Measurement run(
            Workload workload,
            Variant variant,
            Structure structure,
            Input input
    ) {
        if (workload == Workload.W4) {
            return runHeap(input.values());
        }

        IntSequence sequence = switch (structure) {
            case ARRAY -> new DynamicArray();
            case LIST -> new MyLinkedList();
            case HEAP -> throw new IllegalArgumentException(
                    "Heap does not support sequence workloads"
            );
        };

        // Начальное заполнение W1–W3 не входит в измерение.
        for (int value : input.values()) {
            sequence.add(value);
        }

        OperationMetrics metrics = sequence.metrics();
        metrics.reset();

        long checksum = 0;
        long start = System.nanoTime();

        switch (workload) {
            case W1 -> {
                for (int index : input.indices()) {
                    checksum += sequence.get(index);
                }
            }

            case W2 -> {
                for (int query : input.queries()) {
                    if (sequence.contains(query)) {
                        checksum++;
                    }
                }
            }

            case W3 -> {
                // n — исходный размер. Индекс фиксирован на весь запуск.
                int index = variant == Variant.HEAD
                        ? 0
                        : input.values().length / 2;

                for (int i = 0; i < CHANGE_COUNT; i++) {
                    sequence.add(index, -1 - i);
                }

                for (int i = 0; i < CHANGE_COUNT; i++) {
                    checksum += sequence.remove(index);
                }
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported workload: " + workload
            );
        }

        long elapsed = System.nanoTime() - start;

        // Сохраняем счётчики до проверок результата.
        Measurement measurement = new Measurement(
                elapsed,
                metrics.getSteps(),
                metrics.getMoves(),
                metrics.getComparisons(),
                checksum
        );

        verifySequence(workload, sequence, input, checksum);

        checksumSink = checksum;
        return measurement;
    }

    private static void verifySequence(
            Workload workload,
            IntSequence sequence,
            Input input,
            long checksum
    ) {
        long expectedChecksum;

        switch (workload) {
            case W1 -> {
                expectedChecksum = 0;

                for (int index : input.indices()) {
                    expectedChecksum += input.values()[index];
                }
            }

            case W2 -> expectedChecksum = SEARCH_COUNT / 2;

            case W3 -> expectedChecksum =
                    -((long) CHANGE_COUNT * (CHANGE_COUNT + 1)) / 2;

            default -> throw new IllegalArgumentException(
                    "Unsupported workload: " + workload
            );
        }

        if (checksum != expectedChecksum) {
            throw new AssertionError(
                    workload + ": unexpected checksum"
            );
        }

        if (sequence.size() != input.values().length) {
            throw new AssertionError(
                    workload + ": unexpected size"
            );
        }

        // Проверяем все оставшиеся значения после измерения.
        // Для списка удаление с головы позволяет избежать обхода по каждому индексу.
        for (int expected : input.values()) {
            if (sequence.remove(0) != expected) {
                throw new AssertionError(
                        workload + ": original data was changed"
                );
            }
        }

        if (sequence.size() != 0) {
            throw new AssertionError("Sequence was not fully drained");
        }
    }

    private static Measurement runHeap(int[] values) {
        MinHeap heap = new MinHeap();

        // Массив для проверки создаём до начала измерения.
        int[] output = new int[values.length];

        long start = System.nanoTime();

        for (int value : values) {
            heap.insert(value);
        }

        for (int i = 0; i < output.length; i++) {
            output[i] = heap.extractMin();
        }

        long elapsed = System.nanoTime() - start;

        OperationMetrics metrics = heap.metrics();

        // Проверки и контрольная сумма не входят во время W4.
        long actualSum = 0;
        long expectedSum = 0;

        for (int i = 0; i < output.length; i++) {
            if (i > 0 && output[i - 1] > output[i]) {
                throw new AssertionError("Heap output is not sorted");
            }

            actualSum += output[i];
            expectedSum += values[i];
        }

        if (actualSum != expectedSum || heap.size() != 0) {
            throw new AssertionError("Unexpected heap result");
        }

        checksumSink = actualSum;

        return new Measurement(
                elapsed,
                metrics.getSteps(),
                metrics.getMoves(),
                metrics.getComparisons(),
                actualSum
        );
    }

    private static void checkRepeatability(Measurement[] measurements) {
        Measurement first = measurements[0];

        for (Measurement current : measurements) {
            if (current.steps() != first.steps()
                    || current.moves() != first.moves()
                    || current.comparisons() != first.comparisons()
                    || current.checksum() != first.checksum()) {

                throw new AssertionError(
                        "Counters or results differ between repeated runs"
                );
            }
        }
    }

    private static Measurement selectMedian(Measurement[] measurements) {
        // Сортировка вставками для пяти результатов.
        for (int i = 1; i < measurements.length; i++) {
            Measurement current = measurements[i];
            int j = i - 1;

            while (j >= 0
                    && measurements[j].timeNanos() > current.timeNanos()) {

                measurements[j + 1] = measurements[j];
                j--;
            }

            measurements[j + 1] = current;
        }

        return measurements[measurements.length / 2];
    }
}
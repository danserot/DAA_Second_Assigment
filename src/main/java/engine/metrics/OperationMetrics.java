package engine.metrics;

public final class OperationMetrics {
    private long steps;
    private long moves;
    private long comparisons;

    public void recordStep() {
        steps++;
    }

    public void recordMove() {
        moves++;
    }

    public void recordComparison() {
        comparisons++;
    }

    public long getSteps() {
        return steps;
    }

    public long getMoves() {
        return moves;
    }

    public long getComparisons() {
        return comparisons;
    }

    public void reset() {
        steps = 0;
        moves = 0;
        comparisons = 0;
    }
}
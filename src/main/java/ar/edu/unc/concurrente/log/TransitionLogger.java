package ar.edu.unc.concurrente.log;

import java.util.Arrays;

public class TransitionLogger {
    private final int[] attemptsByTransition;
    private final int[] firedByTransition;

    public TransitionLogger(int transitionCount) {
        if (transitionCount <= 0) {
            throw new IllegalArgumentException("Debe existir al menos una transicion");
        }

        this.attemptsByTransition = new int[transitionCount];
        this.firedByTransition = new int[transitionCount];
    }

    public synchronized void record(int transition, boolean fired) {
        attemptsByTransition[transition]++;
        if (fired) {
            firedByTransition[transition]++;
        }
    }

    public synchronized int getTotalAttempts() {
        return sum(attemptsByTransition);
    }

    public synchronized int getTotalFired() {
        return sum(firedByTransition);
    }

    public synchronized int[] getFiredByTransition() {
        return Arrays.copyOf(firedByTransition, firedByTransition.length);
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }

        return total;
    }
}

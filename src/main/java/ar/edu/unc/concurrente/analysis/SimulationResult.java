package ar.edu.unc.concurrente.analysis;

import ar.edu.unc.concurrente.petri.Marking;

import java.util.Arrays;

public class SimulationResult {
    private final Marking finalMarking;
    private final int totalAttempts;
    private final int totalFired;
    private final int[] firedByTransition;
    private final boolean invariantOk;

    public SimulationResult(Marking finalMarking, int totalAttempts, int totalFired, int[] firedByTransition, boolean invariantOk) {
        this.finalMarking = finalMarking.copy();
        this.totalAttempts = totalAttempts;
        this.totalFired = totalFired;
        this.firedByTransition = Arrays.copyOf(firedByTransition, firedByTransition.length);
        this.invariantOk = invariantOk;
    }

    public Marking getFinalMarking() {
        return finalMarking.copy();
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public int getTotalFired() {
        return totalFired;
    }

    public int[] getFiredByTransition() {
        return Arrays.copyOf(firedByTransition, firedByTransition.length);
    }

    public boolean isInvariantOk() {
        return invariantOk;
    }

    @Override
    public String toString() {
        return "SimulationResult{" +
                "finalMarking=" + finalMarking +
                ", totalAttempts=" + totalAttempts +
                ", totalFired=" + totalFired +
                ", firedByTransition=" + Arrays.toString(firedByTransition) +
                ", invariantOk=" + invariantOk +
                '}';
    }
}

package ar.edu.unc.concurrente.analysis;

import java.util.Arrays;

public class SimulationState {
    private final int targetCompletedInvariants;
    private final int inputTransition;
    private final int completionTransition;
    private final int[] modeStartTransitions;
    private final int[] completedByMode;
    private int startedInvariants;
    private int completedInvariants;

    public SimulationState(
            int targetCompletedInvariants,
            int inputTransition,
            int completionTransition,
            int[] modeStartTransitions
    ) {
        if (targetCompletedInvariants <= 0) {
            throw new IllegalArgumentException("La cantidad objetivo de invariantes debe ser positiva");
        }

        this.targetCompletedInvariants = targetCompletedInvariants;
        this.inputTransition = inputTransition;
        this.completionTransition = completionTransition;
        this.modeStartTransitions = Arrays.copyOf(modeStartTransitions, modeStartTransitions.length);
        this.completedByMode = new int[modeStartTransitions.length];
    }

    public synchronized void recordFiredTransition(int transition) {
        if (transition == inputTransition && startedInvariants < targetCompletedInvariants) {
            startedInvariants++;
        }

        for (int i = 0; i < modeStartTransitions.length; i++) {
            if (modeStartTransitions[i] == transition) {
                completedByMode[i]++;
                break;
            }
        }

        if (transition == completionTransition && completedInvariants < targetCompletedInvariants) {
            completedInvariants++;
        }
    }

    public synchronized boolean isFinished() {
        return completedInvariants >= targetCompletedInvariants;
    }

    public synchronized boolean canFireTransition(int transition) {
        return transition != inputTransition || startedInvariants < targetCompletedInvariants;
    }

    public synchronized int getCompletedInvariants() {
        return completedInvariants;
    }

    public synchronized int getStartedInvariants() {
        return startedInvariants;
    }

    public int getTargetCompletedInvariants() {
        return targetCompletedInvariants;
    }

    public synchronized int[] getCompletedByMode() {
        return Arrays.copyOf(completedByMode, completedByMode.length);
    }
}

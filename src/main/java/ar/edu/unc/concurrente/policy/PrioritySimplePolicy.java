package ar.edu.unc.concurrente.policy;

import java.util.Set;

public class PrioritySimplePolicy implements Policy {
    private final Integer simpleModeTransition;

    public PrioritySimplePolicy() {
        this.simpleModeTransition = null;
    }

    public PrioritySimplePolicy(int simpleModeTransition) {
        this.simpleModeTransition = simpleModeTransition;
    }

    @Override
    public int chooseTransition(Set<Integer> enabledTransitions) {
        if (enabledTransitions.isEmpty()) {
            throw new IllegalArgumentException("No hay transiciones habilitadas para elegir");
        }
        if (simpleModeTransition != null && enabledTransitions.contains(simpleModeTransition)) {
            return simpleModeTransition;
        }

        return enabledTransitions.iterator().next();
    }
}

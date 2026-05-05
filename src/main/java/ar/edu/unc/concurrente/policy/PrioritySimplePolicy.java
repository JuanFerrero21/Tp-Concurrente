package ar.edu.unc.concurrente.policy;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class PrioritySimplePolicy implements Policy {
    private final Integer simpleModeTransition;
    private final Set<Integer> conflictTransitions;

    public PrioritySimplePolicy() {
        this.simpleModeTransition = null;
        this.conflictTransitions = new LinkedHashSet<>();
    }

    public PrioritySimplePolicy(int simpleModeTransition) {
        this.simpleModeTransition = simpleModeTransition;
        this.conflictTransitions = new LinkedHashSet<>();
    }

    public PrioritySimplePolicy(int simpleModeTransition, int[] conflictTransitions) {
        this.simpleModeTransition = simpleModeTransition;
        this.conflictTransitions = toSet(conflictTransitions);
    }

    @Override
    public int chooseTransition(Set<Integer> enabledTransitions) {
        if (enabledTransitions.isEmpty()) {
            throw new IllegalArgumentException("No hay transiciones habilitadas para elegir");
        }

        Set<Integer> candidates = getCandidates(enabledTransitions);
        if (simpleModeTransition != null && candidates.contains(simpleModeTransition)) {
            return simpleModeTransition;
        }

        return candidates.iterator().next();
    }

    private Set<Integer> getCandidates(Set<Integer> enabledTransitions) {
        if (conflictTransitions.isEmpty()) {
            return enabledTransitions;
        }

        Set<Integer> nonConflictCandidates = new LinkedHashSet<>();
        Set<Integer> conflictCandidates = new LinkedHashSet<>();
        for (int transition : enabledTransitions) {
            if (conflictTransitions.contains(transition)) {
                conflictCandidates.add(transition);
            } else {
                nonConflictCandidates.add(transition);
            }
        }

        return nonConflictCandidates.isEmpty() ? conflictCandidates : nonConflictCandidates;
    }

    private static Set<Integer> toSet(int[] transitions) {
        Set<Integer> result = new LinkedHashSet<>();
        Arrays.stream(transitions).forEach(result::add);
        return result;
    }
}

package ar.edu.unc.concurrente.policy;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public class RandomPolicy implements Policy {
    private final Random random;
    private final Set<Integer> conflictTransitions;

    public RandomPolicy() {
        this(new Random(), new int[0]);
    }

    public RandomPolicy(int[] conflictTransitions) {
        this(new Random(), conflictTransitions);
    }

    public RandomPolicy(Random random) {
        this(random, new int[0]);
    }

    public RandomPolicy(Random random, int[] conflictTransitions) {
        this.random = random;
        this.conflictTransitions = toSet(conflictTransitions);
    }

    @Override
    public int chooseTransition(Set<Integer> enabledTransitions) {
        if (enabledTransitions.isEmpty()) {
            throw new IllegalArgumentException("No hay transiciones habilitadas para elegir");
        }

        Set<Integer> candidates = getCandidates(enabledTransitions);
        int selectedIndex = random.nextInt(candidates.size());
        int index = 0;
        for (int transition : candidates) {
            if (index == selectedIndex) {
                return transition;
            }
            index++;
        }

        throw new IllegalStateException("No se pudo seleccionar una transicion");
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

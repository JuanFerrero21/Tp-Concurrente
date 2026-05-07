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

        /*
         * Politica priorizada:
         * si la transicion del modo simple esta disponible como candidata,
         * se la elige siempre.
         */
        if (simpleModeTransition != null && enabledTransitions.contains(simpleModeTransition)) {
            return simpleModeTransition;
        }

        /*
         * Si no esta disponible el modo simple, se priorizan transiciones
         * que no pertenezcan al conflicto, por ejemplo T0 o T11.
         */
        for (int transition : enabledTransitions) {
            if (!conflictTransitions.contains(transition)) {
                return transition;
            }
        }

        /*
         * Si solo quedan transiciones de conflicto y no esta el modo simple,
         * se toma la primera disponible.
         */
        return enabledTransitions.iterator().next();
    }

    private static Set<Integer> toSet(int[] transitions) {
        Set<Integer> result = new LinkedHashSet<>();
        Arrays.stream(transitions).forEach(result::add);
        return result;
    }
}
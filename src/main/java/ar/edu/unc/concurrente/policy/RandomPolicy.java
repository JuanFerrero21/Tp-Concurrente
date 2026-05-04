package ar.edu.unc.concurrente.policy;

import java.util.Random;
import java.util.Set;

public class RandomPolicy implements Policy {
    private final Random random;

    public RandomPolicy() {
        this(new Random());
    }

    public RandomPolicy(Random random) {
        this.random = random;
    }

    @Override
    public int chooseTransition(Set<Integer> enabledTransitions) {
        if (enabledTransitions.isEmpty()) {
            throw new IllegalArgumentException("No hay transiciones habilitadas para elegir");
        }

        int selectedIndex = random.nextInt(enabledTransitions.size());
        int index = 0;
        for (int transition : enabledTransitions) {
            if (index == selectedIndex) {
                return transition;
            }
            index++;
        }

        throw new IllegalStateException("No se pudo seleccionar una transicion");
    }
}

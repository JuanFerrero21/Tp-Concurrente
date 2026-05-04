package ar.edu.unc.concurrente.policy;

import java.util.Set;

public interface Policy {
    int chooseTransition(Set<Integer> enabledTransitions);
}

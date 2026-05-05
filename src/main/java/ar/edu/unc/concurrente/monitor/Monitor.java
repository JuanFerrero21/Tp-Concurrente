package ar.edu.unc.concurrente.monitor;

import ar.edu.unc.concurrente.petri.PetriNet;
import ar.edu.unc.concurrente.policy.Policy;
import ar.edu.unc.concurrente.policy.PrioritySimplePolicy;

import java.util.LinkedHashSet;
import java.util.Set;

public class Monitor implements MonitorInterface {
    private final PetriNet petriNet;
    private final Policy policy;
    private final int[] waitingByTransition;
    private Integer selectedTransition;

    public Monitor(PetriNet petriNet) {
        this(petriNet, new PrioritySimplePolicy());
    }

    public Monitor(PetriNet petriNet, Policy policy) {
        this.petriNet = petriNet;
        this.policy = policy;
        this.waitingByTransition = new int[petriNet.getTransitionCount()];
    }

    @Override
    public synchronized boolean fireTransition(int transition) {
        validateTransition(transition);

        waitingByTransition[transition]++;
        try {
            System.out.println("\n" + Thread.currentThread().getName() + " intenta T" + transition);

            while (true) {
                updateSelectedTransitionIfNeeded();

                boolean enabled = petriNet.isEnabled(transition);
                if (enabled && selectedTransition != null && selectedTransition == transition) {
                    break;
                }

                if (!enabled) {
                    System.out.println(Thread.currentThread().getName()
                            + " NO puede T" + transition
                            + " porque no esta sensibilizada. Habilitadas con hilos esperando: "
                            + getEnabledWaitingTransitions()
                            + ". Espera.");
                } else {
                    System.out.println(Thread.currentThread().getName()
                            + " espera para T" + transition
                            + ". La politica selecciono " + formatSelectedTransition()
                            + ". Habilitadas con hilos esperando: "
                            + getEnabledWaitingTransitions()
                            + ". Espera.");
                }

                wait();

                System.out.println(Thread.currentThread().getName()
                        + " se despierta y reintenta T" + transition);
            }

            waitingByTransition[transition]--;

            petriNet.fire(transition);

            System.out.println(Thread.currentThread().getName()
                    + " ejecuto T" + transition
                    + ". Marcado: " + petriNet.getMarking());

            selectedTransition = null;
            notifyAll();

            return true;
        } catch (InterruptedException exception) {
            waitingByTransition[transition]--;
            selectedTransition = null;
            notifyAll();

            Thread.currentThread().interrupt();

            System.out.println(Thread.currentThread().getName()
                    + " fue interrumpido mientras esperaba T" + transition);

            return false;
        }
    }

    private void validateTransition(int transition) {
        if (transition < 0 || transition >= waitingByTransition.length) {
            throw new IllegalArgumentException("Transicion fuera de rango: " + transition);
        }
    }

    private void updateSelectedTransitionIfNeeded() {
        Set<Integer> enabledWaitingTransitions = getEnabledWaitingTransitions();

        if (enabledWaitingTransitions.isEmpty()) {
            selectedTransition = null;
            return;
        }

        if (selectedTransition == null || !enabledWaitingTransitions.contains(selectedTransition)) {
            selectedTransition = policy.chooseTransition(enabledWaitingTransitions);
            notifyAll();
        }
    }

    private Set<Integer> getEnabledWaitingTransitions() {
        Set<Integer> enabledWaitingTransitions = new LinkedHashSet<>();
        Set<Integer> enabledTransitions = petriNet.getEnabledTransitions();

        for (int transition : enabledTransitions) {
            if (waitingByTransition[transition] > 0) {
                enabledWaitingTransitions.add(transition);
            }
        }

        return enabledWaitingTransitions;
    }

    private String formatSelectedTransition() {
        return selectedTransition == null ? "ninguna transicion" : "T" + selectedTransition;
    }
}
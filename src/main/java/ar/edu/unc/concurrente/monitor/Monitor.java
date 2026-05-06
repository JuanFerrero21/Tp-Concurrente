package ar.edu.unc.concurrente.monitor;

import ar.edu.unc.concurrente.analysis.SimulationState;
import ar.edu.unc.concurrente.petri.PetriNet;
import ar.edu.unc.concurrente.policy.Policy;
import ar.edu.unc.concurrente.policy.PrioritySimplePolicy;

import java.util.LinkedHashSet;
import java.util.Set;

public class Monitor implements MonitorInterface {
    private final PetriNet petriNet;
    private final Policy policy;
    private final SimulationState simulationState;
    private final Mutex mutex;
    private final TransitionQueues transitionQueues;
    private final boolean verbose;
    private Integer selectedTransition;

    public Monitor(PetriNet petriNet) {
        this(petriNet, new PrioritySimplePolicy(), null, false);
    }

    public Monitor(PetriNet petriNet, Policy policy) {
        this(petriNet, policy, null, false);
    }

    public Monitor(PetriNet petriNet, Policy policy, SimulationState simulationState) {
        this(petriNet, policy, simulationState, false);
    }

    public Monitor(PetriNet petriNet, Policy policy, SimulationState simulationState, boolean verbose) {
        this.petriNet = petriNet;
        this.policy = policy;
        this.simulationState = simulationState;
        this.mutex = new Mutex();
        this.transitionQueues = new TransitionQueues(mutex, petriNet.getTransitionCount());
        this.verbose = verbose;
    }

    @Override
    public boolean fireTransition(int transition) {
        mutex.acquire();
        try {
            validateTransition(transition);
            if (isSimulationFinished() || !canFireBySimulationState(transition)) {
                return false;
            }

            transitionQueues.startWaiting(transition);
            log("\n" + Thread.currentThread().getName() + " intenta T" + transition);

            while (true) {
                if (isSimulationFinished() || !canFireBySimulationState(transition)) {
                    transitionQueues.stopWaiting(transition);
                    selectedTransition = null;
                    transitionQueues.signalAll();
                    return false;
                }

                updateSelectedTransitionIfNeeded();
                if (selectedTransition != null && selectedTransition == transition) {
                    break;
                }

                if (!petriNet.isEnabled(transition)) {
                    log(Thread.currentThread().getName()
                            + " NO puede T" + transition
                            + " porque no esta sensibilizada. Habilitadas con hilos esperando: "
                            + getEnabledWaitingTransitions()
                            + ". Espera.");
                } else {
                    log(Thread.currentThread().getName()
                            + " espera para T" + transition
                            + ". La politica selecciono " + formatSelectedTransition(selectedTransition)
                            + ". Habilitadas con hilos esperando: "
                            + getEnabledWaitingTransitions()
                            + ". Espera.");
                }

                signalSelectedTransition();
                transitionQueues.await(transition);

                log(Thread.currentThread().getName()
                        + " se despierta y reintenta T" + transition);
            }

            transitionQueues.stopWaiting(transition);

            petriNet.fire(transition);
            if (simulationState != null) {
                simulationState.recordFiredTransition(transition);
            }

            log(Thread.currentThread().getName()
                    + " ejecuto T" + transition
                    + ". Marcado: " + petriNet.getMarking());

            selectedTransition = null;
            if (isSimulationFinished()) {
                transitionQueues.signalAll();
            } else {
                signalSelectedTransition();
            }

            return true;
        } catch (InterruptedException exception) {
            transitionQueues.stopWaiting(transition);
            selectedTransition = null;
            transitionQueues.signalAll();

            Thread.currentThread().interrupt();

            log(Thread.currentThread().getName()
                    + " fue interrumpido mientras esperaba T" + transition);

            return false;
        } finally {
            mutex.release();
        }
    }

    private void validateTransition(int transition) {
        if (transition < 0 || transition >= transitionQueues.getTransitionCount()) {
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
        }
    }

    private void signalSelectedTransition() {
        updateSelectedTransitionIfNeeded();
        if (selectedTransition != null) {
            transitionQueues.signal(selectedTransition);
        }
    }

    private Set<Integer> getEnabledWaitingTransitions() {
        Set<Integer> enabledWaitingTransitions = new LinkedHashSet<>();
        Set<Integer> enabledTransitions = petriNet.getEnabledTransitions();

        for (int transition : enabledTransitions) {
            if (transitionQueues.hasWaitingThread(transition) && canFireBySimulationState(transition)) {
                enabledWaitingTransitions.add(transition);
            }
        }

        return enabledWaitingTransitions;
    }

    private String formatSelectedTransition(Integer selectedTransition) {
        return selectedTransition == null ? "ninguna transicion" : "T" + selectedTransition;
    }

    private boolean isSimulationFinished() {
        return simulationState != null && simulationState.isFinished();
    }

    private boolean canFireBySimulationState(int transition) {
        return simulationState == null || simulationState.canFireTransition(transition);
    }

    private void log(String message) {
        if (verbose) {
            System.out.println(message);
        }
    }
}

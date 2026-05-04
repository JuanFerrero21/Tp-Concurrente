package ar.edu.unc.concurrente.monitor;

import ar.edu.unc.concurrente.petri.PetriNet;
import ar.edu.unc.concurrente.policy.Policy;
import ar.edu.unc.concurrente.policy.PrioritySimplePolicy;

public class Monitor implements MonitorInterface {
    private final PetriNet petriNet;
    private final Policy policy;

    public Monitor(PetriNet petriNet) {
        this(petriNet, new PrioritySimplePolicy());
    }

    public Monitor(PetriNet petriNet, Policy policy) {
        this.petriNet = petriNet;
        this.policy = policy;
    }

    @Override
    public synchronized boolean fireTransition(int transition) {
        try {
            while (!canFireByPolicy(transition)) {
                wait();
            }

            petriNet.fire(transition);
            notifyAll();
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean canFireByPolicy(int transition) {
        return petriNet.isEnabled(transition)
                && policy.chooseTransition(petriNet.getEnabledTransitions()) == transition;
    }
}

package ar.edu.unc.concurrente.monitor;

import ar.edu.unc.concurrente.petri.PetriNet;

public class Monitor implements MonitorInterface {
    private final PetriNet petriNet;

    public Monitor(PetriNet petriNet) {
        this.petriNet = petriNet;
    }

    @Override
    public synchronized boolean fireTransition(int transition) {
        return petriNet.fire(transition);
    }
}

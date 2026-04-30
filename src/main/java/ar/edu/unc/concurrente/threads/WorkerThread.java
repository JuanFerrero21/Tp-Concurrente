package ar.edu.unc.concurrente.threads;

import ar.edu.unc.concurrente.monitor.MonitorInterface;

public class WorkerThread extends Thread {
    private final MonitorInterface monitor;
    private final int[] assignedTransitions;
    private final int cycles;

    public WorkerThread(String name, MonitorInterface monitor, int[] assignedTransitions, int cycles) {
        super(name);
        this.monitor = monitor;
        this.assignedTransitions = assignedTransitions;
        this.cycles = cycles;
    }

    @Override
    public void run() {
        for (int cycle = 0; cycle < cycles; cycle++) {
            for (int transition : assignedTransitions) {
                boolean fired = monitor.fireTransition(transition);
                System.out.println(getName() + " intento T" + transition + " -> " + fired);
            }
        }
    }
}

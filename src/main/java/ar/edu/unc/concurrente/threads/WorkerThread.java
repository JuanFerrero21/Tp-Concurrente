package ar.edu.unc.concurrente.threads;

import ar.edu.unc.concurrente.log.TransitionLogger;
import ar.edu.unc.concurrente.monitor.MonitorInterface;

public class WorkerThread extends Thread {
    private final MonitorInterface monitor;
    private final int[] assignedTransitions;
    private final int cycles;
    private final TransitionLogger transitionLogger;

    public WorkerThread(String name, MonitorInterface monitor, int[] assignedTransitions, int cycles) {
        this(name, monitor, assignedTransitions, cycles, null);
    }

    public WorkerThread(
            String name,
            MonitorInterface monitor,
            int[] assignedTransitions,
            int cycles,
            TransitionLogger transitionLogger
    ) {
        super(name);
        this.monitor = monitor;
        this.assignedTransitions = assignedTransitions;
        this.cycles = cycles;
        this.transitionLogger = transitionLogger;
    }

    @Override
    public void run() {
        for (int cycle = 0; cycle < cycles; cycle++) {
            for (int transition : assignedTransitions) {
                boolean fired = monitor.fireTransition(transition);

                if (transitionLogger != null) {
                    transitionLogger.record(transition, fired);
                }

                if (!fired) {
                    return;
                }
            }
        }
    }
}
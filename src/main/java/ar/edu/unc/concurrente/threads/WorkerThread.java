package ar.edu.unc.concurrente.threads;

import ar.edu.unc.concurrente.analysis.SimulationState;
import ar.edu.unc.concurrente.log.TransitionLogger;
import ar.edu.unc.concurrente.monitor.MonitorInterface;

public class WorkerThread extends Thread {
    private final MonitorInterface monitor;
    private final int[] assignedTransitions;
    private final SimulationState simulationState;
    private final TransitionLogger transitionLogger;

    public WorkerThread(String name, MonitorInterface monitor, int[] assignedTransitions, SimulationState simulationState) {
        this(name, monitor, assignedTransitions, simulationState, null);
    }

    public WorkerThread(
            String name,
            MonitorInterface monitor,
            int[] assignedTransitions,
            SimulationState simulationState,
            TransitionLogger transitionLogger
    ) {
        super(name);
        this.monitor = monitor;
        this.assignedTransitions = assignedTransitions;
        this.simulationState = simulationState;
        this.transitionLogger = transitionLogger;
    }

    @Override
    public void run() {
        while (!simulationState.isFinished()) {
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

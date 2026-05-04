package ar.edu.unc.concurrente.app;

import ar.edu.unc.concurrente.analysis.InvariantChecker;
import ar.edu.unc.concurrente.analysis.SimulationResult;
import ar.edu.unc.concurrente.config.SimulationConfig;
import ar.edu.unc.concurrente.log.TransitionLogger;
import ar.edu.unc.concurrente.monitor.Monitor;
import ar.edu.unc.concurrente.monitor.MonitorInterface;
import ar.edu.unc.concurrente.petri.PetriNet;
import ar.edu.unc.concurrente.policy.PrioritySimplePolicy;
import ar.edu.unc.concurrente.threads.WorkerThread;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        SimulationConfig config = SimulationConfig.defaultConfig();
        PetriNet petriNet = new PetriNet(config.getInitialMarking(), config.getIncidenceMatrix());
        MonitorInterface monitor = new Monitor(petriNet, new PrioritySimplePolicy(0));
        TransitionLogger transitionLogger = new TransitionLogger(petriNet.getTransitionCount());

        System.out.println("Marcado inicial: " + config.getInitialMarking());
        System.out.println("Transiciones sensibilizadas: " + petriNet.getEnabledTransitions());

        WorkerThread[] workers = createWorkers(config, monitor, transitionLogger);
        for (WorkerThread worker : workers) {
            worker.start();
        }

        for (WorkerThread worker : workers) {
            worker.join();
        }

        InvariantChecker invariantChecker = new InvariantChecker();
        SimulationResult result = new SimulationResult(
                petriNet.getMarking(),
                transitionLogger.getTotalAttempts(),
                transitionLogger.getTotalFired(),
                transitionLogger.getFiredByTransition(),
                invariantChecker.keepsTokenTotal(config.getInitialMarking(), petriNet.getMarking())
        );

        System.out.println("Resumen: " + result);
    }

    private static WorkerThread[] createWorkers(
            SimulationConfig config,
            MonitorInterface monitor,
            TransitionLogger transitionLogger
    ) {
        int[][] workerTransitions = config.getWorkerTransitions();
        WorkerThread[] workers = new WorkerThread[config.getWorkerCount()];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new WorkerThread(
                    "Worker-" + (i + 1),
                    monitor,
                    workerTransitions[i],
                    config.getCyclesPerWorker(),
                    transitionLogger
            );
        }

        return workers;
    }
}

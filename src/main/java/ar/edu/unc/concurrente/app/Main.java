package ar.edu.unc.concurrente.app;

import ar.edu.unc.concurrente.analysis.InvariantChecker;
import ar.edu.unc.concurrente.analysis.SimulationResult;
import ar.edu.unc.concurrente.analysis.SimulationState;
import ar.edu.unc.concurrente.config.SimulationConfig;
import ar.edu.unc.concurrente.log.TransitionLogger;
import ar.edu.unc.concurrente.monitor.Monitor;
import ar.edu.unc.concurrente.monitor.MonitorInterface;
import ar.edu.unc.concurrente.petri.PetriNet;
import ar.edu.unc.concurrente.policy.Policy;
import ar.edu.unc.concurrente.policy.PrioritySimplePolicy;
import ar.edu.unc.concurrente.policy.RandomPolicy;
import ar.edu.unc.concurrente.threads.WorkerThread;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        String policyName = args.length > 0 ? args[0] : "random";
        int targetCompletedInvariants = args.length > 1 ? Integer.parseInt(args[1]) : 200;

        SimulationConfig config = SimulationConfig.defaultConfig(targetCompletedInvariants);
        PetriNet petriNet = new PetriNet(config.getInitialMarking(), config.getIncidenceMatrix());
        SimulationState simulationState = new SimulationState(
                config.getTargetCompletedInvariants(),
                config.getInputTransition(),
                config.getCompletionTransition(),
                config.getConflictTransitions()
        );
        Policy policy = createPolicy(policyName, config);
        MonitorInterface monitor = new Monitor(
                petriNet,
                policy,
                simulationState
        );
        TransitionLogger transitionLogger = new TransitionLogger(petriNet.getTransitionCount());

        System.out.println("Politica: " + policyName);
        System.out.println("Marcado inicial: " + config.getInitialMarking());
        System.out.println("Transiciones sensibilizadas: " + petriNet.getEnabledTransitions());
        System.out.println("Objetivo de invariantes completos: " + config.getTargetCompletedInvariants());

        WorkerThread[] workers = createWorkers(config, monitor, simulationState, transitionLogger);

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
        System.out.println("Invariantes completos: "
                + simulationState.getCompletedInvariants()
                + "/"
                + simulationState.getTargetCompletedInvariants());
        System.out.println("Invariantes iniciados: " + simulationState.getStartedInvariants());
        System.out.println("Procesados por modo [medio, simple, alto]: "
                + java.util.Arrays.toString(simulationState.getCompletedByMode()));
    }

    private static WorkerThread[] createWorkers(
            SimulationConfig config,
            MonitorInterface monitor,
            SimulationState simulationState,
            TransitionLogger transitionLogger
    ) {
        int[][] workerTransitions = config.getWorkerTransitions();
        WorkerThread[] workers = new WorkerThread[config.getWorkerCount()];

        for (int i = 0; i < workers.length; i++) {
            workers[i] = new WorkerThread(
                    "Worker-" + (i + 1),
                    monitor,
                    workerTransitions[i],
                    simulationState,
                    transitionLogger
            );
        }

        return workers;
    }

    private static Policy createPolicy(String policyName, SimulationConfig config) {
        if ("priority".equalsIgnoreCase(policyName) || "prioridad".equalsIgnoreCase(policyName)) {
            return new PrioritySimplePolicy(config.getSimpleModeTransition(), config.getConflictTransitions());
        }
        if ("random".equalsIgnoreCase(policyName) || "aleatoria".equalsIgnoreCase(policyName)) {
            return new RandomPolicy(config.getConflictTransitions());
        }

        throw new IllegalArgumentException("Politica desconocida: " + policyName);
    }
}

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
import ar.edu.unc.concurrente.time.SensibilizadoConTiempo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {
    /*
     * Configuracion principal de la ejecucion.
     *
     * Cambiar estos valores para probar distintos casos:
     *
     * POLICY_NAME:
     * - "priority" para priorizar el modo simple.
     * - "random" para politica aleatoria.
     *
     * TARGET_COMPLETED_INVARIANTS:
     * - cantidad de invariantes completos que debe ejecutar el sistema.
     *
     * VERBOSE:
     * - true para ver detalle de disparos y esperas por consola.
     * - false para ejecucion normal.
     *
     * LOG_PATH:
     * - archivo donde se registran las transiciones disparadas y los invariantes.
     */
    private static final String POLICY_NAME = "random";
    private static final int TARGET_COMPLETED_INVARIANTS = 200;
    private static final boolean VERBOSE = false;
    private static final Path LOG_PATH = Paths.get("logs", "simulation.log");

    public static void main(String[] args) throws InterruptedException {
        long startMillis = System.currentTimeMillis();

        SimulationConfig config = SimulationConfig.defaultConfig(TARGET_COMPLETED_INVARIANTS);
        PetriNet petriNet = new PetriNet(config.getInitialMarking(), config.getIncidenceMatrix());

        SimulationState simulationState = new SimulationState(
                config.getTargetCompletedInvariants(),
                config.getInputTransition(),
                config.getCompletionTransition(),
                config.getConflictTransitions()
        );

        Policy policy = createPolicy(POLICY_NAME, config);

        SensibilizadoConTiempo sensibilizadoConTiempo = new SensibilizadoConTiempo(
                config.getTemporalTransitions(),
                config.getAlfaMillis(),
                config.getBetaMillis()
        );

        TransitionLogger transitionLogger = new TransitionLogger(
                petriNet.getTransitionCount(),
                LOG_PATH,
                POLICY_NAME,
                config.getTargetCompletedInvariants(),
                config.getTemporalTransitions(),
                config.getAlfaMillis(),
                config.getBetaMillis()
        );

        InvariantChecker invariantChecker = new InvariantChecker(config.getInitialMarking());

        MonitorInterface monitor = new Monitor(
                petriNet,
                policy,
                simulationState,
                sensibilizadoConTiempo,
                VERBOSE,
                transitionLogger,
                invariantChecker
        );

        System.out.println("Politica: " + POLICY_NAME);
        System.out.println("Marcado inicial: " + config.getInitialMarking());
        System.out.println("Transiciones sensibilizadas: " + petriNet.getEnabledTransitions());
        System.out.println("Objetivo de invariantes completos: " + config.getTargetCompletedInvariants());
        System.out.println("Transiciones temporales: " + Arrays.toString(config.getTemporalTransitions()));
        System.out.println("Alfa ms: " + Arrays.toString(config.getAlfaMillis()));
        System.out.println("Beta ms: " + Arrays.toString(config.getBetaMillis()));

        WorkerThread[] workers = createWorkers(config, monitor, simulationState, transitionLogger);

        for (WorkerThread worker : workers) {
            worker.start();
        }

        for (WorkerThread worker : workers) {
            worker.join();
        }

        long elapsedMillis = System.currentTimeMillis() - startMillis;

        boolean placeInvariantsOk = invariantChecker.arePlaceInvariantsSatisfied(petriNet.getMarking());

        SimulationResult result = new SimulationResult(
                petriNet.getMarking(),
                transitionLogger.getTotalAttempts(),
                transitionLogger.getTotalFired(),
                transitionLogger.getFiredByTransition(),
                placeInvariantsOk
        );

        transitionLogger.writeSummary(
                simulationState.getCompletedInvariants(),
                simulationState.getCompletedByMode(),
                result.isInvariantOk(),
                elapsedMillis
        );

        transitionLogger.close();

        System.out.println("Resumen: " + result);
        System.out.println("Reporte de invariantes de plaza:");
        System.out.print(invariantChecker.buildReport(petriNet.getMarking()));
        System.out.println("Invariantes completos: "
                + simulationState.getCompletedInvariants()
                + "/"
                + simulationState.getTargetCompletedInvariants());

        System.out.println("Invariantes iniciados: " + simulationState.getStartedInvariants());

        System.out.println("Procesados por modo [medio, simple, alto]: "
                + Arrays.toString(simulationState.getCompletedByMode()));

        System.out.println("Tiempo total de ejecucion: " + elapsedMillis + " ms");
        System.out.println("Log generado en: " + LOG_PATH);
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
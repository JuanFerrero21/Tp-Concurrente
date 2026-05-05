package ar.edu.unc.concurrente.config;

import ar.edu.unc.concurrente.petri.Marking;

import java.util.Arrays;

public class SimulationConfig {
    private final Marking initialMarking;
    private final int[][] incidenceMatrix;
    private final int[][] workerTransitions;
    private final int targetCompletedInvariants;
    private final int inputTransition;
    private final int completionTransition;
    private final int[] conflictTransitions;
    private final int simpleModeTransition;

    public SimulationConfig(
            Marking initialMarking,
            int[][] incidenceMatrix,
            int[][] workerTransitions,
            int targetCompletedInvariants,
            int inputTransition,
            int completionTransition,
            int[] conflictTransitions,
            int simpleModeTransition
    ) {
        if (workerTransitions.length == 0) {
            throw new IllegalArgumentException("Debe existir al menos un worker");
        }
        if (targetCompletedInvariants <= 0) {
            throw new IllegalArgumentException("La cantidad objetivo de invariantes debe ser positiva");
        }

        this.initialMarking = initialMarking.copy();
        this.incidenceMatrix = copyMatrix(incidenceMatrix);
        this.workerTransitions = copyMatrix(workerTransitions);
        this.targetCompletedInvariants = targetCompletedInvariants;
        this.inputTransition = inputTransition;
        this.completionTransition = completionTransition;
        this.conflictTransitions = Arrays.copyOf(conflictTransitions, conflictTransitions.length);
        this.simpleModeTransition = simpleModeTransition;
    }

    public static SimulationConfig defaultConfig() {
        return defaultConfig(200);
    }

    public static SimulationConfig defaultConfig(int targetCompletedInvariants) {
        int[][] incidenceMatrix = {
                // T0  T1  T2  T3  T4  T5  T6  T7  T8  T9 T10 T11
                {-1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1}, // P0
                { 1, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0}, // P1
                {-1,  1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0}, // P2
                { 0,  1, -1,  0,  0, -1,  0, -1,  0,  0,  0,  0}, // P3
                { 0,  0,  1, -1,  0,  0,  0,  0,  0,  0,  0,  0}, // P4
                { 0,  0,  0,  1, -1,  0,  0,  0,  0,  0,  0,  0}, // P5
                { 0,  0, -1,  0,  1, -1,  1, -1,  0,  0,  1,  0}, // P6
                { 0,  0,  0,  0,  0,  1, -1,  0,  0,  0,  0,  0}, // P7
                { 0,  0,  0,  0,  0,  0,  0,  1, -1,  0,  0,  0}, // P8
                { 0,  0,  0,  0,  0,  0,  0,  0,  1, -1,  0,  0}, // P9
                { 0,  0,  0,  0,  0,  0,  0,  0,  0,  1, -1,  0}, // P10
                { 0,  0,  0,  0,  1,  0,  1,  0,  0,  0,  1, -1}  // P11
        };

        int[][] workerTransitions = {
                {0, 1},          // Worker-1: ingreso al buffer
                {2, 3, 4},       // Worker-2: modo medio
                {5, 6},          // Worker-3: modo simple
                {7, 8, 9, 10},   // Worker-4: modo alto
                {11},            // Worker-5: salida
                {11},            // Worker-6: salida
                {11}             // Worker-7: salida
        };

        Marking initialMarking = new Marking(new int[] {
                targetCompletedInvariants, // P0
                0, // P1
                1, // P2
                0, // P3
                0, // P4
                0, // P5
                1, // P6
                0, // P7
                0, // P8
                0, // P9
                0, // P10
                0  // P11
        });

        int inputTransition = 0;
        int completionTransition = 11;
        int[] conflictTransitions = {2, 5, 7};
        int simpleModeTransition = 5;

        return new SimulationConfig(
                initialMarking,
                incidenceMatrix,
                workerTransitions,
                targetCompletedInvariants,
                inputTransition,
                completionTransition,
                conflictTransitions,
                simpleModeTransition
        );
    }

    public Marking getInitialMarking() {
        return initialMarking.copy();
    }

    public int[][] getIncidenceMatrix() {
        return copyMatrix(incidenceMatrix);
    }

    public int[][] getWorkerTransitions() {
        return copyMatrix(workerTransitions);
    }

    public int getTargetCompletedInvariants() {
        return targetCompletedInvariants;
    }

    public int getInputTransition() {
        return inputTransition;
    }

    public int getCompletionTransition() {
        return completionTransition;
    }

    public int getWorkerCount() {
        return workerTransitions.length;
    }

    public int[] getConflictTransitions() {
        return Arrays.copyOf(conflictTransitions, conflictTransitions.length);
    }

    public int getSimpleModeTransition() {
        return simpleModeTransition;
    }

    private static int[][] copyMatrix(int[][] matrix) {
        int[][] copy = new int[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            copy[i] = Arrays.copyOf(matrix[i], matrix[i].length);
        }

        return copy;
    }
}

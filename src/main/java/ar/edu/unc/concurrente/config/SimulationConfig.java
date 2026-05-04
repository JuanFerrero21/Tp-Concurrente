package ar.edu.unc.concurrente.config;

import ar.edu.unc.concurrente.petri.Marking;

import java.util.Arrays;

public class SimulationConfig {
    private final Marking initialMarking;
    private final int[][] incidenceMatrix;
    private final int[][] workerTransitions;
    private final int cyclesPerWorker;

    public SimulationConfig(Marking initialMarking, int[][] incidenceMatrix, int[][] workerTransitions, int cyclesPerWorker) {
        if (cyclesPerWorker <= 0) {
            throw new IllegalArgumentException("La cantidad de ciclos debe ser positiva");
        }
        if (workerTransitions.length == 0) {
            throw new IllegalArgumentException("Debe existir al menos un worker");
        }

        this.initialMarking = initialMarking.copy();
        this.incidenceMatrix = copyMatrix(incidenceMatrix);
        this.workerTransitions = copyMatrix(workerTransitions);
        this.cyclesPerWorker = cyclesPerWorker;
    }

    public static SimulationConfig defaultConfig() {
        int[][] incidenceMatrix = {
                {-1, 1},
                {1, -1}
        };

        int[][] workerTransitions = {
                {0},
                {1}
        };

        return new SimulationConfig(new Marking(new int[] {1, 0}), incidenceMatrix, workerTransitions, 8);
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

    public int getCyclesPerWorker() {
        return cyclesPerWorker;
    }

    public int getWorkerCount() {
        return workerTransitions.length;
    }

    private static int[][] copyMatrix(int[][] matrix) {
        int[][] copy = new int[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            copy[i] = Arrays.copyOf(matrix[i], matrix[i].length);
        }

        return copy;
    }
}

package ar.edu.unc.concurrente.config;

import ar.edu.unc.concurrente.petri.Marking;

import java.util.Arrays;

public class SimulationConfig {
    private final Marking initialMarking;
    private final int[][] incidenceMatrix;
    private final int[][] workerTransitions;
    private final int[] cyclesByWorker;

    public SimulationConfig(Marking initialMarking, int[][] incidenceMatrix, int[][] workerTransitions, int cyclesPerWorker) {
        this(initialMarking, incidenceMatrix, workerTransitions, fillCycles(workerTransitions.length, cyclesPerWorker));
    }

    public SimulationConfig(Marking initialMarking, int[][] incidenceMatrix, int[][] workerTransitions, int[] cyclesByWorker) {
        if (workerTransitions.length == 0) {
            throw new IllegalArgumentException("Debe existir al menos un worker");
        }
        if (cyclesByWorker.length != workerTransitions.length) {
            throw new IllegalArgumentException("Debe existir una cantidad de ciclos por cada worker");
        }
        for (int cycles : cyclesByWorker) {
            if (cycles <= 0) {
                throw new IllegalArgumentException("La cantidad de ciclos debe ser positiva");
            }
        }

        this.initialMarking = initialMarking.copy();
        this.incidenceMatrix = copyMatrix(incidenceMatrix);
        this.workerTransitions = copyMatrix(workerTransitions);
        this.cyclesByWorker = Arrays.copyOf(cyclesByWorker, cyclesByWorker.length);
    }

    public static SimulationConfig defaultConfig() {
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

        int[] cyclesByWorker = {
                3, // ingreso: mete 3 datos desde P0 hacia P3
                1, // modo medio: procesa 1 dato
                1, // modo simple: procesa 1 dato
                1, // modo alto: procesa 1 dato
                1, // salida 1
                1, // salida 2
                1  // salida 3
        };

        Marking initialMarking = new Marking(new int[] {
                3, // P0
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

        return new SimulationConfig(initialMarking, incidenceMatrix, workerTransitions, cyclesByWorker);
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
        return cyclesByWorker[0];
    }

    public int getCyclesForWorker(int workerIndex) {
        return cyclesByWorker[workerIndex];
    }

    public int getWorkerCount() {
        return workerTransitions.length;
    }

    private static int[] fillCycles(int workerCount, int cyclesPerWorker) {
        int[] cycles = new int[workerCount];
        Arrays.fill(cycles, cyclesPerWorker);
        return cycles;
    }

    private static int[][] copyMatrix(int[][] matrix) {
        int[][] copy = new int[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            copy[i] = Arrays.copyOf(matrix[i], matrix[i].length);
        }

        return copy;
    }
}
package ar.edu.unc.concurrente.petri;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class PetriNet {
    private Marking marking;
    private final int[][] incidenceMatrix;

    public PetriNet(Marking initialMarking, int[][] incidenceMatrix) {
        validateMatrix(initialMarking, incidenceMatrix);
        this.marking = initialMarking;
        this.incidenceMatrix = copyMatrix(incidenceMatrix);
    }

    public boolean isEnabled(int transition) {
        validateTransition(transition);
        Marking nextMarking = calculateNextMarking(transition);
        return !nextMarking.hasNegativeTokens();
    }

    public boolean fire(int transition) {
        if (!isEnabled(transition)) {
            return false;
        }

        marking = calculateNextMarking(transition);
        return true;
    }

    public Set<Integer> getEnabledTransitions() {
        Set<Integer> enabledTransitions = new LinkedHashSet<>();
        for (int transition = 0; transition < getTransitionCount(); transition++) {
            if (isEnabled(transition)) {
                enabledTransitions.add(transition);
            }
        }

        return enabledTransitions;
    }

    public Marking getMarking() {
        return marking.copy();
    }

    public int getPlaceCount() {
        return incidenceMatrix.length;
    }

    public int getTransitionCount() {
        return incidenceMatrix[0].length;
    }

    private Marking calculateNextMarking(int transition) {
        return marking.add(getColumn(transition));
    }

    private int[] getColumn(int transition) {
        int[] column = new int[getPlaceCount()];
        for (int place = 0; place < getPlaceCount(); place++) {
            column[place] = incidenceMatrix[place][transition];
        }

        return column;
    }

    private void validateTransition(int transition) {
        if (transition < 0 || transition >= getTransitionCount()) {
            throw new IllegalArgumentException("Transicion fuera de rango: " + transition);
        }
    }

    private static void validateMatrix(Marking initialMarking, int[][] incidenceMatrix) {
        if (incidenceMatrix.length == 0) {
            throw new IllegalArgumentException("La matriz de incidencia no puede estar vacia");
        }

        int transitionCount = incidenceMatrix[0].length;
        if (transitionCount == 0) {
            throw new IllegalArgumentException("La matriz debe tener al menos una transicion");
        }

        if (initialMarking.size() != incidenceMatrix.length) {
            throw new IllegalArgumentException("El marcado inicial debe tener una componente por cada plaza");
        }

        for (int[] row : incidenceMatrix) {
            if (row.length != transitionCount) {
                throw new IllegalArgumentException("Todas las filas de la matriz deben tener la misma longitud");
            }
        }
    }

    private static int[][] copyMatrix(int[][] matrix) {
        int[][] copy = new int[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            copy[i] = Arrays.copyOf(matrix[i], matrix[i].length);
        }

        return copy;
    }
}

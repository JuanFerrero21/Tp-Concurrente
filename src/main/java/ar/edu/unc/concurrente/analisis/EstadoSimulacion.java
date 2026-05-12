package ar.edu.unc.concurrente.analisis;

import java.util.Arrays;

public class EstadoSimulacion {
    private final int objetivoInvariantesCompletos;
    private final int transicionIngreso;
    private final int transicionFinalizacion;
    private final int[] transicionesInicioModo;
    private final int[] completosPorModo;
    private int invariantesIniciados;
    private int invariantesCompletos;

    public EstadoSimulacion(
            int objetivoInvariantesCompletos,
            int transicionIngreso,
            int transicionFinalizacion,
            int[] transicionesInicioModo
    ) {
        if (objetivoInvariantesCompletos <= 0) {
            throw new IllegalArgumentException("La cantidad objetivo de invariantes debe ser positiva");
        }

        this.objetivoInvariantesCompletos = objetivoInvariantesCompletos;
        this.transicionIngreso = transicionIngreso;
        this.transicionFinalizacion = transicionFinalizacion;
        this.transicionesInicioModo = Arrays.copyOf(transicionesInicioModo, transicionesInicioModo.length);
        this.completosPorModo = new int[transicionesInicioModo.length];
    }

    public synchronized void registrarTransicionDisparada(int transicion) {
        if (transicion == transicionIngreso && invariantesIniciados < objetivoInvariantesCompletos) {
            invariantesIniciados++;
        }

        for (int i = 0; i < transicionesInicioModo.length; i++) {
            if (transicionesInicioModo[i] == transicion) {
                completosPorModo[i]++;
                break;
            }
        }

        if (transicion == transicionFinalizacion && invariantesCompletos < objetivoInvariantesCompletos) {
            invariantesCompletos++;
        }
    }

    public synchronized boolean estaFinalizada() {
        return invariantesCompletos >= objetivoInvariantesCompletos;
    }

    public synchronized boolean puedeDispararTransicion(int transicion) {
        return transicion != transicionIngreso || invariantesIniciados < objetivoInvariantesCompletos;
    }

    public synchronized int obtenerInvariantesCompletos() {
        return invariantesCompletos;
    }

    public synchronized int obtenerInvariantesIniciados() {
        return invariantesIniciados;
    }

    public int obtenerObjetivoInvariantesCompletos() {
        return objetivoInvariantesCompletos;
    }

    public synchronized int[] obtenerCompletosPorModo() {
        return Arrays.copyOf(completosPorModo, completosPorModo.length);
    }
}

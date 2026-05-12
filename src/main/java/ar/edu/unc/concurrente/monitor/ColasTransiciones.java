package ar.edu.unc.concurrente.monitor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class ColasTransiciones {
    private final Condition[] condicionesPorTransicion;
    private final int[] esperandoPorTransicion;

    public ColasTransiciones(Mutex mutex, int cantidadTransiciones) {
        this.condicionesPorTransicion = new Condition[cantidadTransiciones];
        this.esperandoPorTransicion = new int[cantidadTransiciones];

        for (int i = 0; i < cantidadTransiciones; i++) {
            condicionesPorTransicion[i] = mutex.newCondition();
        }
    }

    public void empezarEspera(int transicion) {
        esperandoPorTransicion[transicion]++;
    }

    public void terminarEspera(int transicion) {
        esperandoPorTransicion[transicion]--;
    }

    public boolean tieneHiloEsperando(int transicion) {
        return esperandoPorTransicion[transicion] > 0;
    }

    public int obtenerCantidadTransiciones() {
        return esperandoPorTransicion.length;
    }

    public void esperar(int transicion) throws InterruptedException {
        condicionesPorTransicion[transicion].await();
    }

    public void esperarMillis(int transicion, long millis) throws InterruptedException {
        if (millis <= 0) {
            return;
        }

        condicionesPorTransicion[transicion].await(millis, TimeUnit.MILLISECONDS);
    }

    public void despertar(int transicion) {
        condicionesPorTransicion[transicion].signal();
    }

    public void despertarTodos() {
        for (Condition condicion : condicionesPorTransicion) {
            condicion.signalAll();
        }
    }
}

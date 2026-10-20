package ar.edu.unc.concurrente.monitor;

import java.util.concurrent.Semaphore;

/** Todas las operaciones salvo acquire() ocurren con el control del monitor. */
public class ColasTransiciones {
    private final Mutex mutex;
    private final Semaphore[] colas;
    private final int[] esperando;
    private volatile boolean finalizando;

    public ColasTransiciones(Mutex mutex, int cantidadTransiciones) {
        this.mutex = mutex;
        this.colas = new Semaphore[cantidadTransiciones];
        this.esperando = new int[cantidadTransiciones];
        for (int i = 0; i < cantidadTransiciones; i++) {
            colas[i] = new Semaphore(0, true);
        }
    }

    public int obtenerCantidadTransiciones() {
        return colas.length;
    }

    public boolean tieneHiloEsperando(int transicion) {
        return esperando[transicion] > 0;
    }

    /** Registra la espera ANTES de soltar el mutex; evita perder señales. */
    public void esperar(int transicion) {
        esperando[transicion]++;
        mutex.release();
        colas[transicion].acquireUninterruptibly();
        // En ejecución normal el permiso del monitor se transfirió directamente.
        // Al finalizar no hay handoff: cada trabajador debe recuperar el mutex.
        if (finalizando) {
            mutex.acquire();
        }
    }

    /** La espera temporal no recibe handoff: vuelve a adquirir el mutex. */
    public void esperarMillis(long millis) {
        if (millis <= 0) return;
        mutex.release();
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.acquire();
        }
    }

    /** Si devuelve true, se transfirió el control; NO liberar mutex. */
    public boolean despertar(int transicion) {
        if (esperando[transicion] == 0) return false;
        esperando[transicion]--;
        colas[transicion].release();
        return true;
    }

    /** Solo se invoca con el monitor controlado y al terminar la simulación. */
    public void despertarTodos() {
        if (finalizando) return;
        finalizando = true;
        for (int t = 0; t < colas.length; t++) {
            int cantidad = esperando[t];
            esperando[t] = 0;
            if (cantidad > 0) colas[t].release(cantidad);
        }
    }
}

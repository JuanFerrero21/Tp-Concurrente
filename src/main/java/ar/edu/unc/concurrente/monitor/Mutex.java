package ar.edu.unc.concurrente.monitor;

import java.util.concurrent.Semaphore;

public class Mutex {
    private final Semaphore semaforo;
    private int reingresosPreferentesPendientes;

    public Mutex() {
        this.semaforo = new Semaphore(1, true);
    }

    public void acquire() {
        while (true) {
            semaforo.acquireUninterruptibly();

            synchronized (this) {
                if (reingresosPreferentesPendientes == 0) {
                    return;
                }
            }

            semaforo.release();
            Thread.yield();
        }
    }

    public void adquirirComoPreferente() {
        semaforo.acquireUninterruptibly();

        synchronized (this) {
            if (reingresosPreferentesPendientes > 0) {
                reingresosPreferentesPendientes--;
            }
        }
    }

    public synchronized void registrarReingresoPreferente() {
        reingresosPreferentesPendientes++;
    }

    public void release() {
        semaforo.release();
    }
}

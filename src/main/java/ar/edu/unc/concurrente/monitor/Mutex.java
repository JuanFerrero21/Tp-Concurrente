package ar.edu.unc.concurrente.monitor;

import java.util.concurrent.Semaphore;

public class Mutex {
    private final Semaphore semaforo;

    public Mutex() {
        this.semaforo = new Semaphore(1, true);
    }

    public void acquire() {
        semaforo.acquireUninterruptibly();
    }

    public void release() {
        semaforo.release();
    }
}

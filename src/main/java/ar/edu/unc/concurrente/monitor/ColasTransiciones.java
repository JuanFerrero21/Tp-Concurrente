package ar.edu.unc.concurrente.monitor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;

public class ColasTransiciones {
    private final Mutex mutex;
    private final Semaphore[] semaforosPorTransicion;
    private final int[] esperandoPorTransicion;
    private final int[] bloqueadosPorTransicion;
    private final int[] permisosPendientesPorTransicion;
    private final int[] permisosHandoffPendientesPorTransicion;
    private final Semaphore controlColas;

    public ColasTransiciones(Mutex mutex, int cantidadTransiciones) {
        this.mutex = mutex;
        this.semaforosPorTransicion = new Semaphore[cantidadTransiciones];
        this.esperandoPorTransicion = new int[cantidadTransiciones];
        this.bloqueadosPorTransicion = new int[cantidadTransiciones];
        this.permisosPendientesPorTransicion = new int[cantidadTransiciones];
        this.permisosHandoffPendientesPorTransicion = new int[cantidadTransiciones];
        this.controlColas = new Semaphore(1, true);

        for (int i = 0; i < cantidadTransiciones; i++) {
            semaforosPorTransicion[i] = new Semaphore(0, true);
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
        esperarEnSemaforo(transicion, 0, false);
    }

    public void esperarMillis(int transicion, long millis) throws InterruptedException {
        if (millis <= 0) {
            return;
        }

        esperarEnSemaforo(transicion, millis, true);
    }

    public boolean despertar(int transicion) {
        controlColas.acquireUninterruptibly();

        try {
            if (permisosPendientesPorTransicion[transicion]
                    < bloqueadosPorTransicion[transicion]) {

                permisosPendientesPorTransicion[transicion]++;
                permisosHandoffPendientesPorTransicion[transicion]++;
                semaforosPorTransicion[transicion].release();
                return true;
            }

            return false;
        } finally {
            controlColas.release();
        }
    }

    public void despertarTodos() {
        controlColas.acquireUninterruptibly();

        try {
            for (int transicion = 0; transicion < semaforosPorTransicion.length; transicion++) {

                int permisosNecesarios = bloqueadosPorTransicion[transicion] - permisosPendientesPorTransicion[transicion];

                if (permisosNecesarios > 0) {
                    permisosPendientesPorTransicion[transicion] += permisosNecesarios;

                    semaforosPorTransicion[transicion].release(permisosNecesarios);
                }
            }
        } finally {
            controlColas.release();
        }
    }

    private void esperarEnSemaforo(int transicion, long millis, boolean conTimeout) throws InterruptedException {
        boolean permisoConsumido = false;
        boolean mutexTransferido = false;
        controlColas.acquireUninterruptibly();

        try {
            bloqueadosPorTransicion[transicion]++;
        } finally {
            controlColas.release();
        }

        mutex.release();

        try {
            if (conTimeout) {
                permisoConsumido = semaforosPorTransicion[transicion].tryAcquire(millis, TimeUnit.MILLISECONDS);
            } else {
                semaforosPorTransicion[transicion].acquire();
                permisoConsumido = true;
            }
        } finally {
            controlColas.acquireUninterruptibly();

            try {
                if (!permisoConsumido) {
                    permisoConsumido =
                            semaforosPorTransicion[transicion].tryAcquire();
                }

                bloqueadosPorTransicion[transicion]--;

                if (permisoConsumido) {
                    permisosPendientesPorTransicion[transicion]--;

                    if (permisosHandoffPendientesPorTransicion[transicion] > 0) {
                        permisosHandoffPendientesPorTransicion[transicion]--;
                        mutexTransferido = true;
                    }
                }
            } finally {
                controlColas.release();
            }

            if (!mutexTransferido) {
                mutex.acquire();
            }
        }
    }
}

package ar.edu.unc.concurrente.monitor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;

public class ColasTransiciones {
    private final Mutex mutex;
    private final Semaphore[] semaforosPorTransicion;
    private final int[] esperandoPorTransicion;
    private final int[] bloqueadosPorTransicion;
    private final int[] permisosPendientesPorTransicion;

    public ColasTransiciones(Mutex mutex, int cantidadTransiciones) {
        this.mutex = mutex;
        this.semaforosPorTransicion = new Semaphore[cantidadTransiciones];
        this.esperandoPorTransicion = new int[cantidadTransiciones];
        this.bloqueadosPorTransicion = new int[cantidadTransiciones];
        this.permisosPendientesPorTransicion = new int[cantidadTransiciones];

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

    public void despertar(int transicion) {
        if (permisosPendientesPorTransicion[transicion] < bloqueadosPorTransicion[transicion]) {
            permisosPendientesPorTransicion[transicion]++;
            mutex.registrarReingresoPreferente();
            semaforosPorTransicion[transicion].release();
        }
    }

    public void despertarTodos() {
        for (int transicion = 0; transicion < semaforosPorTransicion.length; transicion++) {
            int permisosNecesarios = bloqueadosPorTransicion[transicion] - permisosPendientesPorTransicion[transicion];

            if (permisosNecesarios > 0) {
                permisosPendientesPorTransicion[transicion] += permisosNecesarios;
                for (int i = 0; i < permisosNecesarios; i++) {
                    mutex.registrarReingresoPreferente();
                }
                semaforosPorTransicion[transicion].release(permisosNecesarios);
            }
        }
    }

    private void esperarEnSemaforo(int transicion, long millis, boolean conTimeout) throws InterruptedException {
        boolean permisoConsumido = false;
        bloqueadosPorTransicion[transicion]++;
        mutex.release();

        try {
            if (conTimeout) {
                permisoConsumido = semaforosPorTransicion[transicion].tryAcquire(millis, TimeUnit.MILLISECONDS);
            } else {
                semaforosPorTransicion[transicion].acquire();
                permisoConsumido = true;
            }
        } finally {
            if (!permisoConsumido) {
                permisoConsumido = semaforosPorTransicion[transicion].tryAcquire();
            }

            if (permisoConsumido) {
                mutex.adquirirComoPreferente();
            } else {
                mutex.acquire();
            }

            bloqueadosPorTransicion[transicion]--;

            if (permisoConsumido) {
                permisosPendientesPorTransicion[transicion]--;
            }
        }
    }
}

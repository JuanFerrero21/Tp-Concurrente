package ar.edu.unc.concurrente.monitor;

import ar.edu.unc.concurrente.redpetri.RedDePetri;
import ar.edu.unc.concurrente.politica.Politica;

import java.util.LinkedHashSet;
import java.util.Set;

public class Monitor implements MonitorInterface {
    private final RedDePetri redDePetri;
    private final Politica politica;
    private final Mutex mutex;
    private final ColasTransiciones colasTransiciones;
    private final boolean detallado;

    public Monitor(
            RedDePetri redDePetri,
            Politica politica,
            boolean detallado
    ) {
        this.redDePetri = redDePetri;
        this.politica = politica;
        this.mutex = new Mutex();
        this.colasTransiciones = new ColasTransiciones(mutex, redDePetri.obtenerCantidadTransiciones());
        this.detallado = detallado;
    }

    @Override
    public boolean fireTransition(int transicion) {
        mutex.acquire();
        boolean liberarMutex = true;

        try {
            validarTransicion(transicion);

            if (redDePetri.simulacionFinalizada() || !redDePetri.puedeDispararPorEstadoSimulacion(transicion)) {
                return false;
            }

            colasTransiciones.empezarEspera(transicion);
            log("\n" + Thread.currentThread().getName() + " intenta T" + transicion);

            while (true) {
                if (redDePetri.simulacionFinalizada() || !redDePetri.puedeDispararPorEstadoSimulacion(transicion)) {
                    colasTransiciones.terminarEspera(transicion);
                    colasTransiciones.despertarTodos();
                    return false;
                }

                if (redDePetri.puedeDisparar(transicion)) {
                    break;
                }

                esperarSegunEstadoTransicion(transicion);

                log(Thread.currentThread().getName()
                        + " se despierta y reintenta T" + transicion);
            }

            colasTransiciones.terminarEspera(transicion);

            boolean disparada = redDePetri.disparar(transicion);

            if (!disparada) {
                throw new IllegalStateException("La transicion T" + transicion + " fue seleccionada pero no pudo dispararse");
            }

            log(Thread.currentThread().getName()
                    + " ejecuto T" + transicion
                    + ". Marcado: " + redDePetri.obtenerMarcado());

            if (redDePetri.simulacionFinalizada()) {
                colasTransiciones.despertarTodos();
            } else {
                liberarMutex = !despertarTransicionElegidaPorPolitica();
            }

            return true;
        } catch (InterruptedException excepcion) {
            colasTransiciones.terminarEspera(transicion);
            colasTransiciones.despertarTodos();

            Thread.currentThread().interrupt();

            log(Thread.currentThread().getName()
                    + " fue interrumpido mientras esperaba T" + transicion);

            return false;
        } finally {
            if (liberarMutex) {
                mutex.release();
            }
        }
    }

    private void esperarSegunEstadoTransicion(int transicion) throws InterruptedException {
        log(Thread.currentThread().getName()
                + " espera T" + transicion
                + ". Motivo: " + redDePetri.describirMotivoEspera(transicion)
                + ". Habilitadas con hilos esperando: "
                + obtenerTransicionesSensibilizadasConHilosEsperando());

        if (redDePetri.estaAntesDeLaVentanaTemporal(transicion)) {
            colasTransiciones.esperarMillis(transicion, redDePetri.milisegundosHastaVentanaTemporal(transicion));
        } else {
            colasTransiciones.esperar(transicion);
        }
    }

    private void validarTransicion(int transicion) {
        if (transicion < 0 || transicion >= colasTransiciones.obtenerCantidadTransiciones()) {
            throw new IllegalArgumentException("Transicion fuera de rango: " + transicion);
        }
    }

    private boolean despertarTransicionElegidaPorPolitica() {
        Set<Integer> transicionesSensibilizadasConHilosEsperando = obtenerTransicionesSensibilizadasConHilosEsperando();

        if (transicionesSensibilizadasConHilosEsperando.isEmpty()) {
            return false;
        }

        int transicionSeleccionada = politica.elegirTransicion(transicionesSensibilizadasConHilosEsperando);

        return colasTransiciones.despertar(transicionSeleccionada);
    }

    private Set<Integer> obtenerTransicionesSensibilizadasConHilosEsperando() {
        Set<Integer> transicionesSensibilizadasConHilosEsperando = new LinkedHashSet<>();
        Set<Integer> transicionesSensibilizadas = redDePetri.obtenerTransicionesDisparables();

        for (int transicion : transicionesSensibilizadas) {
            if (colasTransiciones.tieneHiloEsperando(transicion)
                    && redDePetri.puedeDispararPorEstadoSimulacion(transicion)) {
                transicionesSensibilizadasConHilosEsperando.add(transicion);
            }
        }

        return transicionesSensibilizadasConHilosEsperando;
    }

    private void log(String mensaje) {
        if (detallado) {
            System.out.println(mensaje);
        }
    }
}

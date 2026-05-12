package ar.edu.unc.concurrente.monitor;

import ar.edu.unc.concurrente.analisis.VerificadorInvariantes;
import ar.edu.unc.concurrente.analisis.EstadoSimulacion;
import ar.edu.unc.concurrente.registro.RegistradorTransiciones;
import ar.edu.unc.concurrente.redpetri.RedDePetri;
import ar.edu.unc.concurrente.politica.Politica;
import ar.edu.unc.concurrente.politica.PoliticaPrioridadSimple;
import ar.edu.unc.concurrente.tiempo.SensibilizadoConTiempo;

import java.util.LinkedHashSet;
import java.util.Set;

public class Monitor implements MonitorInterface {
    private final RedDePetri redDePetri;
    private final Politica politica;
    private final EstadoSimulacion estadoSimulacion;
    private final Mutex mutex;
    private final ColasTransiciones colasTransiciones;
    private final SensibilizadoConTiempo sensibilizadoConTiempo;
    private final boolean detallado;
    private final RegistradorTransiciones registradorTransiciones;
    private final VerificadorInvariantes verificadorInvariantes;
    private Integer transicionSeleccionada;

    public Monitor(RedDePetri redDePetri) {
        this(redDePetri, new PoliticaPrioridadSimple(), null, null, false, null, null);
    }

    public Monitor(RedDePetri redDePetri, Politica politica) {
        this(redDePetri, politica, null, null, false, null, null);
    }

    public Monitor(RedDePetri redDePetri, Politica politica, EstadoSimulacion estadoSimulacion) {
        this(redDePetri, politica, estadoSimulacion, null, false, null, null);
    }

    public Monitor(RedDePetri redDePetri, Politica politica, EstadoSimulacion estadoSimulacion, boolean detallado) {
        this(redDePetri, politica, estadoSimulacion, null, detallado, null, null);
    }

    public Monitor(
            RedDePetri redDePetri,
            Politica politica,
            EstadoSimulacion estadoSimulacion,
            SensibilizadoConTiempo sensibilizadoConTiempo,
            boolean detallado
    ) {
        this(redDePetri, politica, estadoSimulacion, sensibilizadoConTiempo, detallado, null, null);
    }

    public Monitor(
            RedDePetri redDePetri,
            Politica politica,
            EstadoSimulacion estadoSimulacion,
            SensibilizadoConTiempo sensibilizadoConTiempo,
            boolean detallado,
            RegistradorTransiciones registradorTransiciones
    ) {
        this(redDePetri, politica, estadoSimulacion, sensibilizadoConTiempo, detallado, registradorTransiciones, null);
    }

    public Monitor(
            RedDePetri redDePetri,
            Politica politica,
            EstadoSimulacion estadoSimulacion,
            SensibilizadoConTiempo sensibilizadoConTiempo,
            boolean detallado,
            RegistradorTransiciones registradorTransiciones,
            VerificadorInvariantes verificadorInvariantes
    ) {
        this.redDePetri = redDePetri;
        this.politica = politica;
        this.estadoSimulacion = estadoSimulacion;
        this.mutex = new Mutex();
        this.colasTransiciones = new ColasTransiciones(mutex, redDePetri.obtenerCantidadTransiciones());
        this.sensibilizadoConTiempo = sensibilizadoConTiempo;
        this.detallado = detallado;
        this.registradorTransiciones = registradorTransiciones;
        this.verificadorInvariantes = verificadorInvariantes;

        actualizarSensibilizadasTemporales();
    }

    @Override
    public boolean fireTransition(int transicion) {
        mutex.acquire();

        try {
            validarTransicion(transicion);

            if (simulacionFinalizada() || !puedeDispararPorEstadoSimulacion(transicion)) {
                return false;
            }

            colasTransiciones.empezarEspera(transicion);
            log("\n" + Thread.currentThread().getName() + " intenta T" + transicion);

            while (true) {
                if (simulacionFinalizada() || !puedeDispararPorEstadoSimulacion(transicion)) {
                    colasTransiciones.terminarEspera(transicion);
                    transicionSeleccionada = null;
                    colasTransiciones.despertarTodos();
                    return false;
                }

                actualizarSensibilizadasTemporales();
                actualizarTransicionSeleccionadaSiHaceFalta();

                if (transicionSeleccionada != null && transicionSeleccionada == transicion) {
                    break;
                }

                if (!redDePetri.estaSensibilizada(transicion)) {
                    log(Thread.currentThread().getName()
                            + " NO puede T" + transicion
                            + " porque no esta sensibilizada por tokens. Habilitadas con hilos esperando: "
                            + obtenerTransicionesSensibilizadasConHilosEsperando()
                            + ". Espera.");

                    despertarTransicionSeleccionada();
                    colasTransiciones.esperar(transicion);
                } else if (!puedeDispararPorTiempo(transicion)) {
                    esperarPorVentanaTemporal(transicion);
                } else {
                    log(Thread.currentThread().getName()
                            + " espera para T" + transicion
                            + ". La politica selecciono " + formatearTransicionSeleccionada(transicionSeleccionada)
                            + ". Habilitadas con hilos esperando: "
                            + obtenerTransicionesSensibilizadasConHilosEsperando()
                            + ". Espera.");

                    despertarTransicionSeleccionada();
                    colasTransiciones.esperar(transicion);
                }

                log(Thread.currentThread().getName()
                        + " se despierta y reintenta T" + transicion);
            }

            colasTransiciones.terminarEspera(transicion);

            boolean disparada = redDePetri.disparar(transicion);

            if (!disparada) {
                throw new IllegalStateException("La transicion T" + transicion + " fue seleccionada pero no pudo dispararse");
            }

            verificarInvariantesPlazaLuegoDelDisparo();

            actualizarSensibilizadasTemporales();

            if (estadoSimulacion != null) {
                estadoSimulacion.registrarTransicionDisparada(transicion);
            }

            /*
             * Se registra en archivo solo cuando el disparo fue real.
             * Está dentro del monitor, por lo tanto el orden del log respeta
             * el orden real de disparo protegido por el mutex.
             */
            if (registradorTransiciones != null) {
                registradorTransiciones.registrarEventoDisparo(transicion);
            }

            log(Thread.currentThread().getName()
                    + " ejecuto T" + transicion
                    + ". Marcado: " + redDePetri.obtenerMarcado());

            transicionSeleccionada = null;

            if (simulacionFinalizada()) {
                colasTransiciones.despertarTodos();
            } else {
                despertarTransicionSeleccionada();
            }

            return true;
        } catch (InterruptedException excepcion) {
            colasTransiciones.terminarEspera(transicion);
            transicionSeleccionada = null;
            colasTransiciones.despertarTodos();

            Thread.currentThread().interrupt();

            log(Thread.currentThread().getName()
                    + " fue interrumpido mientras esperaba T" + transicion);

            return false;
        } finally {
            mutex.release();
        }
    }

    private void verificarInvariantesPlazaLuegoDelDisparo() {
        if (verificadorInvariantes != null) {
            verificadorInvariantes.verificarInvariantesPlazaOLanzar(redDePetri.obtenerMarcado());
        }
    }

    private void esperarPorVentanaTemporal(int transicion) throws InterruptedException {
        if (sensibilizadoConTiempo == null || !sensibilizadoConTiempo.esTemporal(transicion)) {
            despertarTransicionSeleccionada();
            colasTransiciones.esperar(transicion);
            return;
        }

        if (sensibilizadoConTiempo.estaAntesDeLaVentana(transicion)) {
            long millisHastaAlfa = sensibilizadoConTiempo.milisegundosHastaAlfa(transicion);

            log(Thread.currentThread().getName()
                    + " espera T" + transicion
                    + " porque todavia no llego a alfa. Falta "
                    + millisHastaAlfa
                    + " ms. Estado temporal: "
                    + sensibilizadoConTiempo.describirEstado(transicion));

            despertarTransicionSeleccionada();
            colasTransiciones.esperarMillis(transicion, millisHastaAlfa);
            return;
        }

        if (sensibilizadoConTiempo.estaVencida(transicion)) {
            log(Thread.currentThread().getName()
                    + " espera T" + transicion
                    + " porque se paso beta en esta ventana. Debe esperar a que la transicion deje de estar sensibilizada y vuelva a sensibilizarse. Estado temporal: "
                    + sensibilizadoConTiempo.describirEstado(transicion));

            despertarTransicionSeleccionada();
            colasTransiciones.esperar(transicion);
            return;
        }

        despertarTransicionSeleccionada();
        colasTransiciones.esperar(transicion);
    }

    private void validarTransicion(int transicion) {
        if (transicion < 0 || transicion >= colasTransiciones.obtenerCantidadTransiciones()) {
            throw new IllegalArgumentException("Transicion fuera de rango: " + transicion);
        }
    }

    private void actualizarTransicionSeleccionadaSiHaceFalta() {
        Set<Integer> transicionesSensibilizadasConHilosEsperando = obtenerTransicionesSensibilizadasConHilosEsperando();

        if (transicionesSensibilizadasConHilosEsperando.isEmpty()) {
            transicionSeleccionada = null;
            return;
        }

        if (transicionSeleccionada == null || !transicionesSensibilizadasConHilosEsperando.contains(transicionSeleccionada)) {
            transicionSeleccionada = politica.elegirTransicion(transicionesSensibilizadasConHilosEsperando);
        }
    }

    private void despertarTransicionSeleccionada() {
        actualizarTransicionSeleccionadaSiHaceFalta();

        if (transicionSeleccionada != null) {
            colasTransiciones.despertar(transicionSeleccionada);
        }
    }

    private Set<Integer> obtenerTransicionesSensibilizadasConHilosEsperando() {
        Set<Integer> transicionesSensibilizadasConHilosEsperando = new LinkedHashSet<>();
        Set<Integer> transicionesSensibilizadas = redDePetri.obtenerTransicionesSensibilizadas();

        actualizarSensibilizadasTemporales(transicionesSensibilizadas);

        for (int transicion : transicionesSensibilizadas) {
            if (colasTransiciones.tieneHiloEsperando(transicion)
                    && puedeDispararPorEstadoSimulacion(transicion)
                    && puedeDispararPorTiempo(transicion)) {
                transicionesSensibilizadasConHilosEsperando.add(transicion);
            }
        }

        return transicionesSensibilizadasConHilosEsperando;
    }

    private boolean puedeDispararPorTiempo(int transicion) {
        return sensibilizadoConTiempo == null || sensibilizadoConTiempo.puedeDispararAhora(transicion);
    }

    private void actualizarSensibilizadasTemporales() {
        actualizarSensibilizadasTemporales(redDePetri.obtenerTransicionesSensibilizadas());
    }

    private void actualizarSensibilizadasTemporales(Set<Integer> transicionesSensibilizadas) {
        if (sensibilizadoConTiempo != null) {
            sensibilizadoConTiempo.actualizarSensibilizadas(transicionesSensibilizadas);
        }
    }

    private String formatearTransicionSeleccionada(Integer transicionSeleccionada) {
        return transicionSeleccionada == null ? "ninguna transicion" : "T" + transicionSeleccionada;
    }

    private boolean simulacionFinalizada() {
        return estadoSimulacion != null && estadoSimulacion.estaFinalizada();
    }

    private boolean puedeDispararPorEstadoSimulacion(int transicion) {
        return estadoSimulacion == null || estadoSimulacion.puedeDispararTransicion(transicion);
    }

    private void log(String mensaje) {
        if (detallado) {
            System.out.println(mensaje);
        }
    }
}
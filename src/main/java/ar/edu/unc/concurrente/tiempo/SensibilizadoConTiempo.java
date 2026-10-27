package ar.edu.unc.concurrente.tiempo;

import java.util.Arrays;
import java.util.Set;

public class SensibilizadoConTiempo {
    private static final long SIN_TIMESTAMP = -1L;

    private final boolean[] temporales;
    private final long[] alfaMillis;
    private final long[] betaMillis;
    private final long[] timestampMillis;
    private final boolean[] estabaSensibilizada;
    private final boolean[] ventanaVencida;

    /*
     * Marca "esperando" del diagrama de calculo de disparo.
     * Guarda el hilo que esta durmiendo hasta alfa para cada transicion
     * temporal (null si nadie espera). Mientras la marca exista, solo ese
     * hilo ("viene de un sleep") puede disparar la transicion: otro hilo que
     * llegue con la ventana abierta obtiene k=false y va a la cola.
     */
    private final Thread[] hiloEsperando;

    public SensibilizadoConTiempo(boolean[] temporales, long[] alfaMillis, long[] betaMillis) {
        if (temporales.length != alfaMillis.length || temporales.length != betaMillis.length) {
            throw new IllegalArgumentException("Los arreglos temporales, alfa y beta deben tener la misma longitud");
        }

        this.temporales = Arrays.copyOf(temporales, temporales.length);
        this.alfaMillis = Arrays.copyOf(alfaMillis, alfaMillis.length);
        this.betaMillis = Arrays.copyOf(betaMillis, betaMillis.length);
        this.timestampMillis = new long[temporales.length];
        this.estabaSensibilizada = new boolean[temporales.length];
        this.ventanaVencida = new boolean[temporales.length];
        this.hiloEsperando = new Thread[temporales.length];

        Arrays.fill(this.timestampMillis, SIN_TIMESTAMP);

        validarVentanas();
    }

    public void actualizarSensibilizadas(Set<Integer> transicionesSensibilizadas) {
        long ahora = ahora();

        for (int transicion = 0; transicion < temporales.length; transicion++) {
            boolean sensibilizadaAhora = transicionesSensibilizadas.contains(transicion);

            if (!sensibilizadaAhora) {
                timestampMillis[transicion] = SIN_TIMESTAMP;
                estabaSensibilizada[transicion] = false;
                ventanaVencida[transicion] = false;
                // Si perdio la sensibilizacion, la espera registrada ya no aplica.
                hiloEsperando[transicion] = null;
                continue;
            }

            if (!estabaSensibilizada[transicion]) {
                timestampMillis[transicion] = ahora;
                ventanaVencida[transicion] = false;
            }

            estabaSensibilizada[transicion] = true;

            if (temporales[transicion] && timestampMillis[transicion] != SIN_TIMESTAMP) {
                long tiempoSensibilizada = ahora - timestampMillis[transicion];

                if (tiempoSensibilizada > betaMillis[transicion]) {
                    ventanaVencida[transicion] = true;
                }
            }
        }
    }

    public boolean puedeDispararAhora(int transicion) {
        validarTransicion(transicion);

        if (!temporales[transicion]) {
            return true;
        }

        if (timestampMillis[transicion] == SIN_TIMESTAMP || ventanaVencida[transicion]) {
            return false;
        }

        long tiempoSensibilizada = ahora() - timestampMillis[transicion];

        boolean dentroDeLaVentana = tiempoSensibilizada >= alfaMillis[transicion]
                && tiempoSensibilizada <= betaMillis[transicion];

        // Diagrama: ventana==true y (esperando==false o viene de un sleep) -> k=true.
        return dentroDeLaVentana && !estaEsperandoOtroHilo(transicion);
    }

    /** setEsperando del diagrama: el hilo actual va a dormir hasta alfa. */
    public void setEsperando(int transicion) {
        validarTransicion(transicion);

        if (temporales[transicion] && hiloEsperando[transicion] == null) {
            hiloEsperando[transicion] = Thread.currentThread();
        }
    }

    /** resetEsperando del diagrama: se invoca al disparar la transicion. */
    public void resetEsperando(int transicion) {
        validarTransicion(transicion);
        hiloEsperando[transicion] = null;
    }

    /** true si algun hilo esta durmiendo hasta alfa para esta transicion. */
    public boolean hayHiloEsperando(int transicion) {
        validarTransicion(transicion);
        return hiloEsperando[transicion] != null;
    }

    /** true si la espera registrada pertenece a un hilo distinto del actual. */
    public boolean estaEsperandoOtroHilo(int transicion) {
        validarTransicion(transicion);
        Thread hilo = hiloEsperando[transicion];
        return hilo != null && hilo != Thread.currentThread();
    }

    /**
     * Al disparar se limpia la marca de espera y se fuerza un nuevo timestamp
     * si la transicion sigue sensibilizada (setNuevoTimeStamp del diagrama).
     */
    public void registrarDisparo(int transicion) {
        validarTransicion(transicion);
        resetEsperando(transicion);
        estabaSensibilizada[transicion] = false;
    }

    public boolean estaAntesDeLaVentana(int transicion) {
        validarTransicion(transicion);

        if (!temporales[transicion] || timestampMillis[transicion] == SIN_TIMESTAMP || ventanaVencida[transicion]) {
            return false;
        }

        return ahora() - timestampMillis[transicion] < alfaMillis[transicion];
    }

    public boolean estaVencida(int transicion) {
        validarTransicion(transicion);

        if (!temporales[transicion] || timestampMillis[transicion] == SIN_TIMESTAMP) {
            return false;
        }

        if (ventanaVencida[transicion]) {
            return true;
        }

        long tiempoSensibilizada = ahora() - timestampMillis[transicion];

        if (tiempoSensibilizada > betaMillis[transicion]) {
            ventanaVencida[transicion] = true;
            return true;
        }

        return false;
    }

    public long milisegundosHastaAlfa(int transicion) {
        validarTransicion(transicion);

        if (!estaAntesDeLaVentana(transicion)) {
            return 0L;
        }

        long tiempoSensibilizada = ahora() - timestampMillis[transicion];

        return Math.max(0L, alfaMillis[transicion] - tiempoSensibilizada);
    }

    public boolean esTemporal(int transicion) {
        validarTransicion(transicion);
        return temporales[transicion];
    }

    public String describirEstado(int transicion) {
        validarTransicion(transicion);

        if (!temporales[transicion]) {
            return "no temporal";
        }

        if (timestampMillis[transicion] == SIN_TIMESTAMP) {
            return "temporal sin timestamp";
        }

        long tiempoSensibilizada = ahora() - timestampMillis[transicion];

        return "temporal elapsed=" + tiempoSensibilizada
                + "ms alfa=" + alfaMillis[transicion]
                + "ms beta=" + betaMillis[transicion]
                + "ms vencida=" + estaVencida(transicion)
                + " esperando=" + (hiloEsperando[transicion] == null ? "nadie" : hiloEsperando[transicion].getName());
    }

    private void validarVentanas() {
        for (int transicion = 0; transicion < temporales.length; transicion++) {
            if (!temporales[transicion]) {
                continue;
            }

            if (alfaMillis[transicion] < 0) {
                throw new IllegalArgumentException("Alfa no puede ser negativa para T" + transicion);
            }

            if (betaMillis[transicion] < alfaMillis[transicion]) {
                throw new IllegalArgumentException("Beta debe ser mayor o igual que alfa para T" + transicion);
            }
        }
    }

    private void validarTransicion(int transicion) {
        if (transicion < 0 || transicion >= temporales.length) {
            throw new IllegalArgumentException("Transicion fuera de rango: " + transicion);
        }
    }

    private long ahora() {
        return System.currentTimeMillis();
    }
}
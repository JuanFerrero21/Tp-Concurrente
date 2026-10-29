package ar.edu.unc.concurrente.registro;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Registra los disparos reales en su orden global, sin identificar tokens. */
public class RegistradorTransiciones implements AutoCloseable {
    private static final int MEDIO = 0;
    private static final int SIMPLE = 1;
    private static final int ALTO = 2;

    private final int[] intentosPorTransicion;
    private final int[] disparosPorTransicion;
    private final PrintWriter escritor;

    public RegistradorTransiciones(
            int cantidadTransiciones,
            Path rutaLog,
            String nombrePolitica,
            int objetivoInvariantesCompletos,
            boolean[] transicionesTemporales,
            long[] alfaMillis,
            long[] betaMillis
    ) {
        if (cantidadTransiciones <= 0) {
            throw new IllegalArgumentException("Debe existir al menos una transicion");
        }
        intentosPorTransicion = new int[cantidadTransiciones];
        disparosPorTransicion = new int[cantidadTransiciones];
        escritor = crearEscritor(rutaLog);
        if (escritor != null) {
            escritor.println("EJECUCION politica=" + nombrePolitica + " objetivo=" + objetivoInvariantesCompletos);
            escritor.println("TEMPORAL transiciones=" + Arrays.toString(transicionesTemporales));
            escritor.println("ALFA millis=" + Arrays.toString(alfaMillis));
            escritor.println("BETA millis=" + Arrays.toString(betaMillis));
            escritor.println("FORMATO disparada=T<transicion>");
            escritor.println("VALIDACION regex global sobre la secuencia de disparos");
            escritor.println();
            escritor.flush();
        }
    }

    /** Lo usa Hilo para llevar las estadisticas de los intentos. */
    public synchronized void registrarIntento(int transicion, boolean disparada) {
        validarTransicion(transicion);
        intentosPorTransicion[transicion]++;
        if (disparada) {
            disparosPorTransicion[transicion]++;
        }
    }

    /** Llamado por RedDePetri tras un disparo exitoso. */
    public synchronized void registrarEventoDisparo(int transicion) {
        validarTransicion(transicion);
        if (escritor != null) {
            escritor.println("disparada=T" + transicion);
            escritor.flush();
        }
    }

    private void validarTransicion(int transicion) {
        if (transicion < 0 || transicion >= disparosPorTransicion.length) {
            throw new IllegalArgumentException("Transicion fuera de rango: T" + transicion);
        }
    }

    public synchronized int obtenerIntentosTotales() {
        return sumarValores(intentosPorTransicion);
    }

    public synchronized int obtenerDisparosTotales() {
        return sumarValores(disparosPorTransicion);
    }

    public synchronized int[] obtenerDisparosPorTransicion() {
        return Arrays.copyOf(disparosPorTransicion, disparosPorTransicion.length);
    }

    public synchronized void escribirResumen(
            int invariantesCompletos,
            int[] completosPorModo,
            boolean invariantesPlazaCumplidos,
            long tiempoTranscurridoMillis
    ) {
        if (escritor == null) return;
        int medio = completosPorModo.length > MEDIO ? completosPorModo[MEDIO] : 0;
        int simple = completosPorModo.length > SIMPLE ? completosPorModo[SIMPLE] : 0;
        int alto = completosPorModo.length > ALTO ? completosPorModo[ALTO] : 0;
        escritor.println();
        escritor.println("RESUMEN completo=" + invariantesCompletos
                + " medio=" + medio + " simple=" + simple + " alto=" + alto
                + " invariantesPlazaCumplidos=" + invariantesPlazaCumplidos
                + " tiempoMillis=" + tiempoTranscurridoMillis);
        escritor.flush();
    }

    @Override
    public synchronized void close() {
        if (escritor != null) escritor.close();
    }

    private PrintWriter crearEscritor(Path rutaLog) {
        if (rutaLog == null) return null;
        try {
            Path padre = rutaLog.getParent();
            if (padre != null) Files.createDirectories(padre);
            BufferedWriter buffer = Files.newBufferedWriter(rutaLog);
            return new PrintWriter(buffer);
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo crear el archivo de log: " + rutaLog, excepcion);
        }
    }

    private static int sumarValores(int[] valores) {
        int total = 0;
        for (int valor : valores) total += valor;
        return total;
    }
}
package ar.edu.unc.concurrente.registro;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class RegistradorTransiciones implements AutoCloseable {
    private static final int INDICE_MODO_MEDIO = 0;
    private static final int INDICE_MODO_SIMPLE = 1;
    private static final int INDICE_MODO_ALTO = 2;

    private final int[] intentosPorTransicion;
    private final int[] disparosPorTransicion;
    private final PrintWriter escritor;

    /*
     * Estas colas NO modifican la Red de Petri.
     * Solo sirven para reconstruir en el archivo log que recorrido hizo cada idToken.
     */
    private final Deque<Integer> p1Tokens = new ArrayDeque<>();
    private final Deque<Integer> p3Tokens = new ArrayDeque<>();
    private final Deque<Integer> p4Tokens = new ArrayDeque<>();
    private final Deque<Integer> p5Tokens = new ArrayDeque<>();
    private final Deque<Integer> p7Tokens = new ArrayDeque<>();
    private final Deque<Integer> p8Tokens = new ArrayDeque<>();
    private final Deque<Integer> p9Tokens = new ArrayDeque<>();
    private final Deque<Integer> p10Tokens = new ArrayDeque<>();
    private final Deque<Integer> p11Tokens = new ArrayDeque<>();

    private final Map<Integer, String> modoPorIdToken = new HashMap<>();

    private int siguienteIdToken = 1;

    public RegistradorTransiciones(int cantidadTransiciones) {
        this(cantidadTransiciones, null, null, 0, null, null, null);
    }

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

        this.intentosPorTransicion = new int[cantidadTransiciones];
        this.disparosPorTransicion = new int[cantidadTransiciones];
        this.escritor = crearEscritor(rutaLog);

        if (escritor != null) {
            escritor.println("EJECUCION politica=" + nombrePolitica + " objetivo=" + objetivoInvariantesCompletos);
            escritor.println("TEMPORAL transiciones=" + Arrays.toString(transicionesTemporales));
            escritor.println("ALFA millis=" + Arrays.toString(alfaMillis));
            escritor.println("BETA millis=" + Arrays.toString(betaMillis));
            escritor.println("FORMATO idToken=<id> disparada=T<transicion>");
            escritor.println("FORMATO idToken=<id> INVARIANTE modo=<modo> camino=<camino> completo=true");
            escritor.println();
            escritor.flush();
        }
    }

    /*
     * Este metodo lo usa Hilo para contar intentos y disparos.
     */
    public synchronized void registrarIntento(int transicion, boolean disparada) {
        intentosPorTransicion[transicion]++;

        if (disparada) {
            disparosPorTransicion[transicion]++;
        }
    }

    /*
     * Este metodo lo llama el Monitor solamente cuando una transicion
     * realmente se disparo.
     */
    public synchronized void registrarEventoDisparo(int transicion) {
        if (escritor == null) {
            return;
        }

        switch (transicion) {
            case 0 -> registrarT0();
            case 1 -> moverYRegistrar(transicion, p1Tokens, p3Tokens, null);
            case 2 -> moverYRegistrar(transicion, p3Tokens, p4Tokens, "MEDIO");
            case 3 -> moverYRegistrar(transicion, p4Tokens, p5Tokens, null);
            case 4 -> moverYRegistrar(transicion, p5Tokens, p11Tokens, null);
            case 5 -> moverYRegistrar(transicion, p3Tokens, p7Tokens, "SIMPLE");
            case 6 -> moverYRegistrar(transicion, p7Tokens, p11Tokens, null);
            case 7 -> moverYRegistrar(transicion, p3Tokens, p8Tokens, "ALTO");
            case 8 -> moverYRegistrar(transicion, p8Tokens, p9Tokens, null);
            case 9 -> moverYRegistrar(transicion, p9Tokens, p10Tokens, null);
            case 10 -> moverYRegistrar(transicion, p10Tokens, p11Tokens, null);
            case 11 -> registrarT11();
            default -> throw new IllegalArgumentException("Transicion sin trazabilidad definida: T" + transicion);
        }

        escritor.flush();
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
        if (escritor == null) {
            return;
        }

        int medio = completosPorModo.length > INDICE_MODO_MEDIO ? completosPorModo[INDICE_MODO_MEDIO] : 0;
        int simple = completosPorModo.length > INDICE_MODO_SIMPLE ? completosPorModo[INDICE_MODO_SIMPLE] : 0;
        int alto = completosPorModo.length > INDICE_MODO_ALTO ? completosPorModo[INDICE_MODO_ALTO] : 0;

        escritor.println();
        escritor.println("RESUMEN completo=" + invariantesCompletos
                + " medio=" + medio
                + " simple=" + simple
                + " alto=" + alto
                + " invariantesPlazaCumplidos=" + invariantesPlazaCumplidos
                + " tiempoMillis=" + tiempoTranscurridoMillis);

        escritor.flush();
    }

    @Override
    public synchronized void close() {
        if (escritor != null) {
            escritor.close();
        }
    }

    private void registrarT0() {
        int idToken = siguienteIdToken++;
        p1Tokens.addLast(idToken);
        escribirDisparo(idToken, 0);
    }

    private void registrarT11() {
        int idToken = quitarPrimerToken(p11Tokens, 11);
        escribirDisparo(idToken, 11);

        String modo = modoPorIdToken.remove(idToken);

        if (modo == null) {
            modo = "DESCONOCIDO";
        }

        escritor.println("idToken=" + idToken
                + " INVARIANTE modo=" + modo
                + " camino=" + caminoPorModo(modo)
                + " completo=true");
    }

    private void moverYRegistrar(int transicion, Deque<Integer> origen, Deque<Integer> destino, String modo) {
        int idToken = quitarPrimerToken(origen, transicion);
        destino.addLast(idToken);

        if (modo != null) {
            modoPorIdToken.put(idToken, modo);
        }

        escribirDisparo(idToken, transicion);
    }

    private int quitarPrimerToken(Deque<Integer> origen, int transicion) {
        Integer idToken = origen.pollFirst();

        if (idToken == null) {
            throw new IllegalStateException("No hay idToken disponible para registrar T" + transicion);
        }

        return idToken;
    }

    private void escribirDisparo(int idToken, int transicion) {
        escritor.println("idToken=" + idToken + " disparada=T" + transicion);
    }

    private String caminoPorModo(String modo) {
        return switch (modo) {
            case "MEDIO" -> "T0,T1,T2,T3,T4,T11";
            case "SIMPLE" -> "T0,T1,T5,T6,T11";
            case "ALTO" -> "T0,T1,T7,T8,T9,T10,T11";
            default -> "DESCONOCIDO";
        };
    }

    private PrintWriter crearEscritor(Path rutaLog) {
        if (rutaLog == null) {
            return null;
        }

        try {
            Path padre = rutaLog.getParent();

            if (padre != null) {
                Files.createDirectories(padre);
            }

            BufferedWriter escritorBuffer = Files.newBufferedWriter(rutaLog);
            return new PrintWriter(escritorBuffer);
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo crear el archivo de log: " + rutaLog, excepcion);
        }
    }

    private static int sumarValores(int[] valores) {
        int total = 0;

        for (int valor : valores) {
            total += valor;
        }

        return total;
    }
}
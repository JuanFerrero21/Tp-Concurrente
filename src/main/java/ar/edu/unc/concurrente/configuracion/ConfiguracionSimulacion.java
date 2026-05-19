package ar.edu.unc.concurrente.configuracion;

import ar.edu.unc.concurrente.redpetri.Marcado;

import java.util.Arrays;

public class ConfiguracionSimulacion {
    private static final int TOKENS_DATOS_INICIALES = 3;

    private final Marcado marcadoInicial;
    private final int[][] matrizIncidencia;
    private final int[][] transicionesHilos;
    private final int objetivoInvariantesCompletos;
    private final int transicionIngreso;
    private final int transicionFinalizacion;
    private final int[] transicionesConflicto;
    private final int transicionModoSimple;
    private final boolean[] transicionesTemporales;
    private final long[] alfaMillis;
    private final long[] betaMillis;

    public ConfiguracionSimulacion(
            Marcado marcadoInicial,
            int[][] matrizIncidencia,
            int[][] transicionesHilos,
            int objetivoInvariantesCompletos,
            int transicionIngreso,
            int transicionFinalizacion,
            int[] transicionesConflicto,
            int transicionModoSimple,
            boolean[] transicionesTemporales,
            long[] alfaMillis,
            long[] betaMillis
    ) {
        if (transicionesHilos.length == 0) {
            throw new IllegalArgumentException("Debe existir al menos un hilo");
        }

        if (objetivoInvariantesCompletos <= 0) {
            throw new IllegalArgumentException("La cantidad objetivo de invariantes debe ser positiva");
        }

        if (transicionesTemporales.length != matrizIncidencia[0].length
                || alfaMillis.length != matrizIncidencia[0].length
                || betaMillis.length != matrizIncidencia[0].length) {
            throw new IllegalArgumentException("La configuracion temporal debe tener una entrada por transicion");
        }

        this.marcadoInicial = marcadoInicial.copia();
        this.matrizIncidencia = copiarMatriz(matrizIncidencia);
        this.transicionesHilos = copiarMatriz(transicionesHilos);
        this.objetivoInvariantesCompletos = objetivoInvariantesCompletos;
        this.transicionIngreso = transicionIngreso;
        this.transicionFinalizacion = transicionFinalizacion;
        this.transicionesConflicto = Arrays.copyOf(transicionesConflicto, transicionesConflicto.length);
        this.transicionModoSimple = transicionModoSimple;
        this.transicionesTemporales = Arrays.copyOf(transicionesTemporales, transicionesTemporales.length);
        this.alfaMillis = Arrays.copyOf(alfaMillis, alfaMillis.length);
        this.betaMillis = Arrays.copyOf(betaMillis, betaMillis.length);
    }

    public static ConfiguracionSimulacion configuracionPorDefecto() {
        return configuracionPorDefecto(200);
    }

    public static ConfiguracionSimulacion configuracionPorDefecto(int objetivoInvariantesCompletos) {
        return configuracionPorDefecto(objetivoInvariantesCompletos, "equilibrado");
    }

    public static ConfiguracionSimulacion configuracionPorDefecto(
            int objetivoInvariantesCompletos,
            String perfilTemporal
    ) {
        int[][] matrizIncidencia = {
                // T0  T1  T2  T3  T4  T5  T6  T7  T8  T9 T10 T11
                {-1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1}, // P0
                { 1, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0}, // P1
                {-1,  1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0}, // P2
                { 0,  1, -1,  0,  0, -1,  0, -1,  0,  0,  0,  0}, // P3
                { 0,  0,  1, -1,  0,  0,  0,  0,  0,  0,  0,  0}, // P4
                { 0,  0,  0,  1, -1,  0,  0,  0,  0,  0,  0,  0}, // P5
                { 0,  0, -1,  0,  1, -1,  1, -1,  0,  0,  1,  0}, // P6
                { 0,  0,  0,  0,  0,  1, -1,  0,  0,  0,  0,  0}, // P7
                { 0,  0,  0,  0,  0,  0,  0,  1, -1,  0,  0,  0}, // P8
                { 0,  0,  0,  0,  0,  0,  0,  0,  1, -1,  0,  0}, // P9
                { 0,  0,  0,  0,  0,  0,  0,  0,  0,  1, -1,  0}, // P10
                { 0,  0,  0,  0,  1,  0,  1,  0,  0,  0,  1, -1}  // P11
        };

        int[][] transicionesHilos = {
                {0, 1},          // Hilo-1: ingreso al buffer
                {2, 3, 4},       // Hilo-2: modo medio
                {5, 6},          // Hilo-3: modo simple
                {7, 8, 9, 10},   // Hilo-4: modo alto
                {11},            // Hilo-5: salida
                {11},            // Hilo-6: salida
                {11}             // Hilo-7: salida
        };

        Marcado marcadoInicial = new Marcado(new int[] {
                TOKENS_DATOS_INICIALES, // P0
                0, // P1
                1, // P2
                0, // P3
                0, // P4
                0, // P5
                1, // P6
                0, // P7
                0, // P8
                0, // P9
                0, // P10
                0  // P11
        });

        int transicionIngreso = 0;
        int transicionFinalizacion = 11;
        int[] transicionesConflicto = {2, 5, 7};
        int transicionModoSimple = 5;

        boolean[] transicionesTemporales = new boolean[12];
        long[] alfaMillis = new long[12];
        long[] betaMillis = new long[12];

        configurarPerfilTemporal(perfilTemporal, transicionesTemporales, alfaMillis, betaMillis);

        return new ConfiguracionSimulacion(
                marcadoInicial,
                matrizIncidencia,
                transicionesHilos,
                objetivoInvariantesCompletos,
                transicionIngreso,
                transicionFinalizacion,
                transicionesConflicto,
                transicionModoSimple,
                transicionesTemporales,
                alfaMillis,
                betaMillis
        );
    }

    private static void configurarPerfilTemporal(
            String perfilTemporal,
            boolean[] transicionesTemporales,
            long[] alfaMillis,
            long[] betaMillis
    ) {
        switch (perfilTemporal.toLowerCase()) {
            case "rapido":
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 1, 40, 400);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 3, 60, 600);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 4, 60, 600);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 6, 70, 700);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 8, 60, 600);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 9, 60, 600);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 10, 60, 600);
                return;
            case "equilibrado":
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 1, 60, 600);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 3, 90, 900);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 4, 90, 900);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 6, 100, 1000);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 8, 90, 900);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 9, 90, 900);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 10, 90, 900);
                return;
            case "lento":
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 1, 80, 800);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 3, 120, 1200);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 4, 120, 1200);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 6, 140, 1400);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 8, 120, 1200);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 9, 120, 1200);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 10, 120, 1200);
                return;
            case "simple-lento":
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 1, 60, 600);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 3, 90, 900);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 4, 90, 900);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 6, 180, 1800);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 8, 90, 900);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 9, 90, 900);
                configurarTransicionTemporal(transicionesTemporales, alfaMillis, betaMillis, 10, 90, 900);
                return;
            default:
                throw new IllegalArgumentException("Perfil temporal desconocido: " + perfilTemporal);
        }
    }

    private static void configurarTransicionTemporal(
            boolean[] transicionesTemporales,
            long[] alfaMillis,
            long[] betaMillis,
            int transicion,
            long alfa,
            long beta
    ) {
        transicionesTemporales[transicion] = true;
        alfaMillis[transicion] = alfa;
        betaMillis[transicion] = beta;
    }

    public Marcado obtenerMarcadoInicial() {
        return marcadoInicial.copia();
    }

    public int[][] obtenerMatrizIncidencia() {
        return copiarMatriz(matrizIncidencia);
    }

    public int[][] obtenerTransicionesHilos() {
        return copiarMatriz(transicionesHilos);
    }

    public int obtenerObjetivoInvariantesCompletos() {
        return objetivoInvariantesCompletos;
    }

    public int obtenerTransicionIngreso() {
        return transicionIngreso;
    }

    public int obtenerTransicionFinalizacion() {
        return transicionFinalizacion;
    }

    public int obtenerCantidadHilos() {
        return transicionesHilos.length;
    }

    public int[] obtenerTransicionesConflicto() {
        return Arrays.copyOf(transicionesConflicto, transicionesConflicto.length);
    }

    public int obtenerTransicionModoSimple() {
        return transicionModoSimple;
    }

    public boolean[] obtenerTransicionesTemporales() {
        return Arrays.copyOf(transicionesTemporales, transicionesTemporales.length);
    }

    public long[] obtenerAlfaMillis() {
        return Arrays.copyOf(alfaMillis, alfaMillis.length);
    }

    public long[] obtenerBetaMillis() {
        return Arrays.copyOf(betaMillis, betaMillis.length);
    }

    private static int[][] copiarMatriz(int[][] matriz) {
        int[][] copia = new int[matriz.length][];

        for (int i = 0; i < matriz.length; i++) {
            copia[i] = Arrays.copyOf(matriz[i], matriz[i].length);
        }

        return copia;
    }
}

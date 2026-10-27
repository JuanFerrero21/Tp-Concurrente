package ar.edu.unc.concurrente.analisis;

import ar.edu.unc.concurrente.configuracion.ConfiguracionSimulacion;
import ar.edu.unc.concurrente.redpetri.Marcado;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalizadorRegexInvariantesTransicion {
    private static final Pattern PATRON_LINEA_DISPARO =
            Pattern.compile("^idToken=(\\d+) disparada=(T\\d+)$");

    private static final Pattern PATRON_INVARIANTE_COMPLETO =
            Pattern.compile("^idToken=(\\d+) INVARIANTE modo=([A-Z]+) camino=([T0-9,]+) completo=true$");

    private static final Pattern PATRON_RESUMEN =
            Pattern.compile("^RESUMEN completo=(\\d+) medio=(\\d+) simple=(\\d+) alto=(\\d+) invariantesPlazaCumplidos=(true|false) tiempoMillis=(\\d+)$");

    private static final Pattern PATRON_SIMPLE =
            Pattern.compile("^T0,T1,T5,T6,T11$");

    private static final Pattern PATRON_MEDIO =
            Pattern.compile("^T0,T1,T2,T3,T4,T11$");

    private static final Pattern PATRON_ALTO =
            Pattern.compile("^T0,T1,T7,T8,T9,T10,T11$");

    /*
     * Regex sobre la secuencia completa de disparos (ej. "T0 T1 T0 T5 T1 ...").
     * Encuentra un invariante de transicion aunque este intercalado con otros:
     *   T0 ... T1 ... ( T5 ... T6 | T2 ... T3 ... T4 | T7 ... T8 ... T9 ... T10 ) ... T11
     * Los grupos (.*?) capturan lo que hay entre medio (disparos de otros datos),
     * que se conserva al borrar el invariante encontrado. Los grupos atomicos (?>...)
     * toman siempre la ocurrencia mas cercana (incluida la eleccion del modo, via
     * lookahead) y no reintentan otras: asi el costo es lineal por intento aun con
     * un log alterado. En un log valido la ocurrencia mas cercana siempre completa
     * un invariante, porque el log respeta el orden real de disparo.
     * Grupos: 3 = rama simple, 5 = rama media, 8 = rama alta.
     */
    private static final Pattern PATRON_INVARIANTE_EN_SECUENCIA = Pattern.compile(
            "T0 (?>(.*?)T1 )"
                    + "(?>(.*?)(?=T5 |T2 |T7 ))"
                    + "(?:(T5 )(?>(.*?)T6 )"
                    + "|(T2 )(?>(.*?)T3 )(?>(.*?)T4 )"
                    + "|(T7 )(?>(.*?)T8 )(?>(.*?)T9 )(?>(.*?)T10 ))"
                    + "(?>(.*?)T11 )");

    private static final int[] GRUPOS_INTERMEDIOS = {1, 2, 4, 6, 7, 9, 10, 11, 12};

    public ResultadoAnalisisRegex analizar(Path rutaLog) {
        List<String> lineas = leerLineas(rutaLog);

        Map<Integer, List<String>> transicionesPorIdToken = new LinkedHashMap<>();
        Map<Integer, LineaInvarianteCompleto> completosPorIdToken = new LinkedHashMap<>();
        List<DisparoEnLog> secuenciaDisparos = new ArrayList<>();

        LineaResumen lineaResumen = null;

        for (int numeroLinea = 1; numeroLinea <= lineas.size(); numeroLinea++) {
            String linea = lineas.get(numeroLinea - 1);
            Matcher matcherDisparo = PATRON_LINEA_DISPARO.matcher(linea);

            if (matcherDisparo.matches()) {
                int idToken = Integer.parseInt(matcherDisparo.group(1));
                String transicion = matcherDisparo.group(2);

                transicionesPorIdToken
                        .computeIfAbsent(idToken, ignorado -> new ArrayList<>())
                        .add(transicion);

                secuenciaDisparos.add(new DisparoEnLog(numeroLinea, idToken, transicion));
                continue;
            }

            Matcher matcherCompletado = PATRON_INVARIANTE_COMPLETO.matcher(linea);

            if (matcherCompletado.matches()) {
                int idToken = Integer.parseInt(matcherCompletado.group(1));
                String modo = matcherCompletado.group(2);
                String camino = matcherCompletado.group(3);

                completosPorIdToken.put(idToken, new LineaInvarianteCompleto(modo, camino));
                continue;
            }

            Matcher matcherResumen = PATRON_RESUMEN.matcher(linea);

            if (matcherResumen.matches()) {
                lineaResumen = new LineaResumen(
                        Integer.parseInt(matcherResumen.group(1)),
                        Integer.parseInt(matcherResumen.group(2)),
                        Integer.parseInt(matcherResumen.group(3)),
                        Integer.parseInt(matcherResumen.group(4)),
                        Boolean.parseBoolean(matcherResumen.group(5)),
                        Long.parseLong(matcherResumen.group(6))
                );
            }
        }

        String errorSecuencia = reproducirSecuenciaEnLaRed(secuenciaDisparos);
        RegexSobreSecuencia regexSecuencia = aplicarRegexSobreSecuencia(secuenciaDisparos);

        return construirResultado(
                transicionesPorIdToken,
                completosPorIdToken,
                lineaResumen,
                errorSecuencia,
                regexSecuencia
        );
    }

    /*
     * Une todos los disparos en un solo texto y, mientras la regex encuentre un
     * invariante completo, lo borra dejando lo que habia entre medio. En un log
     * valido el texto termina vacio; si queda algo, son disparos que no forman
     * ningun invariante (el log fue alterado o esta incompleto).
     */
    private RegexSobreSecuencia aplicarRegexSobreSecuencia(List<DisparoEnLog> secuenciaDisparos) {
        StringBuilder constructor = new StringBuilder();

        for (DisparoEnLog disparo : secuenciaDisparos) {
            constructor.append(disparo.transicion()).append(' ');
        }

        String secuencia = constructor.toString();
        int simple = 0;
        int medio = 0;
        int alto = 0;

        Matcher matcher;

        while ((matcher = buscarInvariante(secuencia)) != null) {
            if (matcher.group(3) != null) {
                simple++;
            } else if (matcher.group(5) != null) {
                medio++;
            } else {
                alto++;
            }

            StringBuilder intermedios = new StringBuilder();

            for (int grupo : GRUPOS_INTERMEDIOS) {
                if (matcher.group(grupo) != null) {
                    intermedios.append(matcher.group(grupo));
                }
            }

            secuencia = secuencia.substring(0, matcher.start())
                    + intermedios
                    + secuencia.substring(matcher.end());
        }

        return new RegexSobreSecuencia(simple, medio, alto, secuencia.trim());
    }

    /*
     * Busca el primer invariante probando cada T0 como inicio con lookingAt.
     * Cada intento arranca con los grupos limpios: con find(), un intento fallido
     * a mitad de camino puede dejar capturas viejas que contaminan el reemplazo.
     */
    private Matcher buscarInvariante(String secuencia) {
        Matcher matcher = PATRON_INVARIANTE_EN_SECUENCIA.matcher(secuencia);
        int inicio = 0;

        while (inicio < secuencia.length()) {
            if (secuencia.startsWith("T0 ", inicio)) {
                matcher.region(inicio, secuencia.length());

                if (matcher.lookingAt()) {
                    return matcher;
                }
            }

            int espacio = secuencia.indexOf(' ', inicio);

            if (espacio < 0) {
                return null;
            }

            inicio = espacio + 1;
        }

        return null;
    }

    /*
     * Reproduce en orden todos los disparos del log sobre la Red de Petri,
     * partiendo del marcado inicial: M' = M + C * e_t.
     * Si algun disparo deja una plaza con tokens negativos, esa transicion no
     * estaba sensibilizada en ese momento y el log describe una ejecucion
     * imposible (por ejemplo, dos datos usando a la vez el bus P2 o la unidad
     * de procesamiento P6). Devuelve null si toda la secuencia es valida.
     *
     * El log no guarda instantes de tiempo, por lo que aca se verifica la
     * sensibilizacion por tokens; alfa se controla durante la simulacion.
     */
    private String reproducirSecuenciaEnLaRed(List<DisparoEnLog> secuenciaDisparos) {
        ConfiguracionSimulacion configuracion = ConfiguracionSimulacion.configuracionPorDefecto();
        int[][] matrizIncidencia = configuracion.obtenerMatrizIncidencia();
        Marcado marcado = configuracion.obtenerMarcadoInicial();

        for (DisparoEnLog disparo : secuenciaDisparos) {
            int transicion = Integer.parseInt(disparo.transicion().substring(1));

            if (transicion < 0 || transicion >= matrizIncidencia[0].length) {
                return "linea " + disparo.numeroLinea() + ": " + disparo.transicion() + " no existe en la red";
            }

            int[] columna = new int[matrizIncidencia.length];

            for (int plaza = 0; plaza < matrizIncidencia.length; plaza++) {
                columna[plaza] = matrizIncidencia[plaza][transicion];
            }

            Marcado siguiente = marcado.sumar(columna);

            if (siguiente.tieneTokensNegativos()) {
                return "linea " + disparo.numeroLinea()
                        + ": idToken=" + disparo.idToken()
                        + " dispara " + disparo.transicion()
                        + " pero no estaba sensibilizada. Marcado en ese momento=" + marcado;
            }

            marcado = siguiente;
        }

        return null;
    }

    private ResultadoAnalisisRegex construirResultado(
            Map<Integer, List<String>> transicionesPorIdToken,
            Map<Integer, LineaInvarianteCompleto> completosPorIdToken,
            LineaResumen lineaResumen,
            String errorSecuencia,
            RegexSobreSecuencia regexSecuencia
    ) {
        int cantidadSimple = 0;
        int cantidadMedio = 0;
        int cantidadAlto = 0;
        int cantidadInvalidos = 0;
        int cantidadIncompletos = 0;

        List<String> errores = new ArrayList<>();

        boolean secuenciaValida = errorSecuencia == null;

        if (!secuenciaValida) {
            errores.add("Secuencia imposible en la red: " + errorSecuencia);
        }

        for (Map.Entry<Integer, List<String>> entrada : transicionesPorIdToken.entrySet()) {
            int idToken = entrada.getKey();
            String caminoReconstruido = String.join(",", entrada.getValue());

            LineaInvarianteCompleto lineaCompletado = completosPorIdToken.get(idToken);

            if (lineaCompletado == null) {
                cantidadIncompletos++;
                errores.add("idToken=" + idToken + " no completo invariante. camino=" + caminoReconstruido);
                continue;
            }

            Modo modoPorRegex = clasificarPorRegex(caminoReconstruido);

            if (modoPorRegex == Modo.INVALIDO) {
                cantidadInvalidos++;
                errores.add("idToken=" + idToken + " tiene camino invalido por regex. camino=" + caminoReconstruido);
                continue;
            }

            if (!lineaCompletado.camino().equals(caminoReconstruido)) {
                cantidadInvalidos++;
                errores.add("idToken=" + idToken
                        + " no coincide entre camino reconstruido y camino registrado. reconstruido="
                        + caminoReconstruido
                        + ", registrado="
                        + lineaCompletado.camino());
                continue;
            }

            if (!lineaCompletado.modo().equals(modoPorRegex.name())) {
                cantidadInvalidos++;
                errores.add("idToken=" + idToken
                        + " no coincide entre modo regex y modo registrado. regex="
                        + modoPorRegex.name()
                        + ", registrado="
                        + lineaCompletado.modo());
                continue;
            }

            switch (modoPorRegex) {
                case SIMPLE -> cantidadSimple++;
                case MEDIO -> cantidadMedio++;
                case ALTO -> cantidadAlto++;
                default -> cantidadInvalidos++;
            }
        }

        for (Integer idToken : completosPorIdToken.keySet()) {
            if (!transicionesPorIdToken.containsKey(idToken)) {
                cantidadInvalidos++;
                errores.add("idToken=" + idToken + " tiene linea INVARIANTE pero no tiene disparos disparada asociados");
            }
        }

        int tokensAnalizados = cantidadSimple + cantidadMedio + cantidadAlto;

        boolean regexSecuenciaValida = regexSecuencia.residuo().isEmpty()
                && regexSecuencia.simple() == cantidadSimple
                && regexSecuencia.medio() == cantidadMedio
                && regexSecuencia.alto() == cantidadAlto;

        if (!regexSecuencia.residuo().isEmpty()) {
            String residuo = regexSecuencia.residuo();
            errores.add("La regex sobre la secuencia completa dejo disparos sin formar invariante: "
                    + (residuo.length() > 200 ? residuo.substring(0, 200) + " ..." : residuo));
        } else if (!regexSecuenciaValida) {
            errores.add("La regex sobre la secuencia completa conto simple="
                    + regexSecuencia.simple() + " medio=" + regexSecuencia.medio()
                    + " alto=" + regexSecuencia.alto()
                    + ", distinto de lo contado por dato");
        }
        boolean resumenValido = validarResumen(
                lineaResumen,
                tokensAnalizados,
                cantidadMedio,
                cantidadSimple,
                cantidadAlto,
                cantidadInvalidos,
                cantidadIncompletos,
                errores
        );

        boolean analisisRegexValido = cantidadInvalidos == 0
                && cantidadIncompletos == 0
                && tokensAnalizados == completosPorIdToken.size()
                && resumenValido
                && secuenciaValida
                && regexSecuenciaValida;

        return new ResultadoAnalisisRegex(
                tokensAnalizados,
                cantidadMedio,
                cantidadSimple,
                cantidadAlto,
                cantidadInvalidos,
                cantidadIncompletos,
                analisisRegexValido,
                resumenValido,
                secuenciaValida,
                regexSecuencia,
                regexSecuenciaValida,
                lineaResumen,
                errores
        );
    }

    private boolean validarResumen(
            LineaResumen lineaResumen,
            int tokensAnalizados,
            int cantidadMedio,
            int cantidadSimple,
            int cantidadAlto,
            int cantidadInvalidos,
            int cantidadIncompletos,
            List<String> errores
    ) {
        if (lineaResumen == null) {
            errores.add("No se encontro la linea RESUMEN en el log");
            return false;
        }

        boolean valido = true;

        if (lineaResumen.completo() != tokensAnalizados) {
            valido = false;
            errores.add("RESUMEN completo="
                    + lineaResumen.completo()
                    + " pero regex analizo "
                    + tokensAnalizados
                    + " tokens completos");
        }

        if (lineaResumen.medio() != cantidadMedio) {
            valido = false;
            errores.add("RESUMEN medio="
                    + lineaResumen.medio()
                    + " pero regex conto medio="
                    + cantidadMedio);
        }

        if (lineaResumen.simple() != cantidadSimple) {
            valido = false;
            errores.add("RESUMEN simple="
                    + lineaResumen.simple()
                    + " pero regex conto simple="
                    + cantidadSimple);
        }

        if (lineaResumen.alto() != cantidadAlto) {
            valido = false;
            errores.add("RESUMEN alto="
                    + lineaResumen.alto()
                    + " pero regex conto alto="
                    + cantidadAlto);
        }

        if (!lineaResumen.invariantesPlazaCumplidos()) {
            valido = false;
            errores.add("RESUMEN invariantesPlazaCumplidos=false");
        }

        if (cantidadInvalidos > 0 || cantidadIncompletos > 0) {
            valido = false;
        }

        return valido;
    }

    private Modo clasificarPorRegex(String camino) {
        if (PATRON_SIMPLE.matcher(camino).matches()) {
            return Modo.SIMPLE;
        }

        if (PATRON_MEDIO.matcher(camino).matches()) {
            return Modo.MEDIO;
        }

        if (PATRON_ALTO.matcher(camino).matches()) {
            return Modo.ALTO;
        }

        return Modo.INVALIDO;
    }

    private List<String> leerLineas(Path rutaLog) {
        try {
            return Files.readAllLines(rutaLog);
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo leer el archivo de log: " + rutaLog, excepcion);
        }
    }

    private enum Modo {
        SIMPLE,
        MEDIO,
        ALTO,
        INVALIDO
    }

    private record LineaInvarianteCompleto(String modo, String camino) {
    }

    private record DisparoEnLog(int numeroLinea, int idToken, String transicion) {
    }

    private record RegexSobreSecuencia(int simple, int medio, int alto, String residuo) {
    }

    private record LineaResumen(
            int completo,
            int medio,
            int simple,
            int alto,
            boolean invariantesPlazaCumplidos,
            long tiempoMillis
    ) {
    }

    public static class ResultadoAnalisisRegex {
        private final int tokensAnalizados;
        private final int cantidadMedio;
        private final int cantidadSimple;
        private final int cantidadAlto;
        private final int cantidadInvalidos;
        private final int cantidadIncompletos;
        private final boolean analisisRegexValido;
        private final boolean resumenValido;
        private final boolean secuenciaValida;
        private final RegexSobreSecuencia regexSecuencia;
        private final boolean regexSecuenciaValida;
        private final LineaResumen lineaResumen;
        private final List<String> errores;

        private ResultadoAnalisisRegex(
                int tokensAnalizados,
                int cantidadMedio,
                int cantidadSimple,
                int cantidadAlto,
                int cantidadInvalidos,
                int cantidadIncompletos,
                boolean analisisRegexValido,
                boolean resumenValido,
                boolean secuenciaValida,
                RegexSobreSecuencia regexSecuencia,
                boolean regexSecuenciaValida,
                LineaResumen lineaResumen,
                List<String> errores
        ) {
            this.secuenciaValida = secuenciaValida;
            this.regexSecuencia = regexSecuencia;
            this.regexSecuenciaValida = regexSecuenciaValida;
            this.tokensAnalizados = tokensAnalizados;
            this.cantidadMedio = cantidadMedio;
            this.cantidadSimple = cantidadSimple;
            this.cantidadAlto = cantidadAlto;
            this.cantidadInvalidos = cantidadInvalidos;
            this.cantidadIncompletos = cantidadIncompletos;
            this.analisisRegexValido = analisisRegexValido;
            this.resumenValido = resumenValido;
            this.lineaResumen = lineaResumen;
            this.errores = new ArrayList<>(errores);
        }

        public boolean esAnalisisRegexValido() {
            return analisisRegexValido;
        }

        public String construirReporte() {
            StringBuilder constructor = new StringBuilder();

            constructor.append("Analisis regex de invariantes de transicion:")
                    .append(System.lineSeparator());

            agregarLinea(constructor, "Tokens analizados", tokensAnalizados);
            agregarLinea(constructor, "Medio", cantidadMedio);
            agregarLinea(constructor, "Simple", cantidadSimple);
            agregarLinea(constructor, "Alto", cantidadAlto);
            agregarLinea(constructor, "Invalidos", cantidadInvalidos);
            agregarLinea(constructor, "Incompletos", cantidadIncompletos);
            agregarLinea(constructor, "Resumen valido", resumenValido);
            agregarLinea(constructor, "Regex sobre secuencia completa",
                    "simple=" + regexSecuencia.simple()
                            + " medio=" + regexSecuencia.medio()
                            + " alto=" + regexSecuencia.alto()
                            + " sobrante=" + (regexSecuencia.residuo().isEmpty() ? "ninguno" : "SI"));
            agregarLinea(constructor, "Regex sobre secuencia valida", regexSecuenciaValida);
            agregarLinea(constructor, "Secuencia reproducible en la red", secuenciaValida);

            if (lineaResumen != null) {
                agregarLinea(constructor, "Tiempo segun RESUMEN", lineaResumen.tiempoMillis() + " ms");
            }

            agregarLinea(constructor, "Analisis regex valido", analisisRegexValido);

            if (!errores.isEmpty()) {
                constructor.append("Errores detectados:")
                        .append(System.lineSeparator());

                int limite = Math.min(errores.size(), 20);

                for (int i = 0; i < limite; i++) {
                    constructor.append("- ")
                            .append(errores.get(i))
                            .append(System.lineSeparator());
                }

                if (errores.size() > limite) {
                    constructor.append("- ... ")
                            .append(errores.size() - limite)
                            .append(" errores mas")
                            .append(System.lineSeparator());
                }
            }

            return constructor.toString();
        }

        private static void agregarLinea(StringBuilder constructor, String etiqueta, Object valor) {
            constructor.append(etiqueta)
                    .append(": ")
                    .append(valor)
                    .append(System.lineSeparator());
        }
    }
}
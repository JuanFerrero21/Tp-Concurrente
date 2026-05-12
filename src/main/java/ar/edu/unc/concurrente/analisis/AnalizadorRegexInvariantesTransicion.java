package ar.edu.unc.concurrente.analisis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AnalizadorRegexInvariantesTransicion {
    private static final Pattern PATRON_LINEA_DISPARO =
            Pattern.compile("^idToken=(\\d+) disparada=(T\\d+)$");

    private static final Pattern PATRON_INVARIANTE_COMPLETO =
            Pattern.compile("^idToken=(\\d+) INVARIANTE modo=([A-Z]+) camino=([T0-9,]+) completo=true$");

    private static final Pattern PATRON_RESUMEN =
            Pattern.compile("^RESUMEN completo=(\\d+) medio=(\\d+) simple=(\\d+) alto=(\\d+) invarianteOk=(true|false) tiempoMillis=(\\d+)$");

    private static final Pattern PATRON_SIMPLE =
            Pattern.compile("^T0,T1,T5,T6,T11$");

    private static final Pattern PATRON_MEDIO =
            Pattern.compile("^T0,T1,T2,T3,T4,T11$");

    private static final Pattern PATRON_ALTO =
            Pattern.compile("^T0,T1,T7,T8,T9,T10,T11$");

    public ResultadoAnalisisRegex analizar(Path rutaLog) {
        List<String> lineas = leerLineas(rutaLog);

        Map<Integer, List<String>> transicionesPorIdToken = new LinkedHashMap<>();
        Map<Integer, LineaInvarianteCompleto> completosPorIdToken = new LinkedHashMap<>();

        LineaResumen lineaResumen = null;

        for (String linea : lineas) {
            java.util.regex.Matcher matcherDisparo = PATRON_LINEA_DISPARO.matcher(linea);

            if (matcherDisparo.matches()) {
                int idToken = Integer.parseInt(matcherDisparo.group(1));
                String transicion = matcherDisparo.group(2);

                transicionesPorIdToken
                        .computeIfAbsent(idToken, ignorado -> new ArrayList<>())
                        .add(transicion);

                continue;
            }

            java.util.regex.Matcher matcherCompletado = PATRON_INVARIANTE_COMPLETO.matcher(linea);

            if (matcherCompletado.matches()) {
                int idToken = Integer.parseInt(matcherCompletado.group(1));
                String modo = matcherCompletado.group(2);
                String camino = matcherCompletado.group(3);

                completosPorIdToken.put(idToken, new LineaInvarianteCompleto(modo, camino));
                continue;
            }

            java.util.regex.Matcher matcherResumen = PATRON_RESUMEN.matcher(linea);

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

        return construirResultado(transicionesPorIdToken, completosPorIdToken, lineaResumen);
    }

    private ResultadoAnalisisRegex construirResultado(
            Map<Integer, List<String>> transicionesPorIdToken,
            Map<Integer, LineaInvarianteCompleto> completosPorIdToken,
            LineaResumen lineaResumen
    ) {
        int cantidadSimple = 0;
        int cantidadMedio = 0;
        int cantidadAlto = 0;
        int cantidadInvalidos = 0;
        int cantidadIncompletos = 0;

        List<String> errores = new ArrayList<>();

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

            if (!lineaCompletado.camino.equals(caminoReconstruido)) {
                cantidadInvalidos++;
                errores.add("idToken=" + idToken
                        + " no coincide entre camino reconstruido y camino registrado. reconstruido="
                        + caminoReconstruido
                        + ", registrado="
                        + lineaCompletado.camino);
                continue;
            }

            if (!lineaCompletado.modo.equals(modoPorRegex.name())) {
                cantidadInvalidos++;
                errores.add("idToken=" + idToken
                        + " no coincide entre modo regex y modo registrado. regex="
                        + modoPorRegex.name()
                        + ", registrado="
                        + lineaCompletado.modo);
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
        boolean resumenOk = validarResumen(
                lineaResumen,
                tokensAnalizados,
                cantidadMedio,
                cantidadSimple,
                cantidadAlto,
                cantidadInvalidos,
                cantidadIncompletos,
                errores
        );

        boolean regexOk = cantidadInvalidos == 0
                && cantidadIncompletos == 0
                && tokensAnalizados == completosPorIdToken.size()
                && resumenOk;

        return new ResultadoAnalisisRegex(
                tokensAnalizados,
                cantidadMedio,
                cantidadSimple,
                cantidadAlto,
                cantidadInvalidos,
                cantidadIncompletos,
                regexOk,
                resumenOk,
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

        boolean ok = true;

        if (lineaResumen.completo != tokensAnalizados) {
            ok = false;
            errores.add("RESUMEN completo="
                    + lineaResumen.completo
                    + " pero regex analizo "
                    + tokensAnalizados
                    + " tokens completos");
        }

        if (lineaResumen.medio != cantidadMedio) {
            ok = false;
            errores.add("RESUMEN medio="
                    + lineaResumen.medio
                    + " pero regex conto medio="
                    + cantidadMedio);
        }

        if (lineaResumen.simple != cantidadSimple) {
            ok = false;
            errores.add("RESUMEN simple="
                    + lineaResumen.simple
                    + " pero regex conto simple="
                    + cantidadSimple);
        }

        if (lineaResumen.alto != cantidadAlto) {
            ok = false;
            errores.add("RESUMEN alto="
                    + lineaResumen.alto
                    + " pero regex conto alto="
                    + cantidadAlto);
        }

        if (!lineaResumen.invarianteOk) {
            ok = false;
            errores.add("RESUMEN invarianteOk=false");
        }

        if (cantidadInvalidos > 0 || cantidadIncompletos > 0) {
            ok = false;
        }

        return ok;
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

    private static class LineaInvarianteCompleto {
        private final String modo;
        private final String camino;

        private LineaInvarianteCompleto(String modo, String camino) {
            this.modo = modo;
            this.camino = camino;
        }
    }

    private static class LineaResumen {
        private final int completo;
        private final int medio;
        private final int simple;
        private final int alto;
        private final boolean invarianteOk;
        private final long tiempoMillis;

        private LineaResumen(
                int completo,
                int medio,
                int simple,
                int alto,
                boolean invarianteOk,
                long tiempoMillis
        ) {
            this.completo = completo;
            this.medio = medio;
            this.simple = simple;
            this.alto = alto;
            this.invarianteOk = invarianteOk;
            this.tiempoMillis = tiempoMillis;
        }
    }

    public static class ResultadoAnalisisRegex {
        private final int tokensAnalizados;
        private final int cantidadMedio;
        private final int cantidadSimple;
        private final int cantidadAlto;
        private final int cantidadInvalidos;
        private final int cantidadIncompletos;
        private final boolean regexOk;
        private final boolean resumenOk;
        private final LineaResumen lineaResumen;
        private final List<String> errores;

        private ResultadoAnalisisRegex(
                int tokensAnalizados,
                int cantidadMedio,
                int cantidadSimple,
                int cantidadAlto,
                int cantidadInvalidos,
                int cantidadIncompletos,
                boolean regexOk,
                boolean resumenOk,
                LineaResumen lineaResumen,
                List<String> errores
        ) {
            this.tokensAnalizados = tokensAnalizados;
            this.cantidadMedio = cantidadMedio;
            this.cantidadSimple = cantidadSimple;
            this.cantidadAlto = cantidadAlto;
            this.cantidadInvalidos = cantidadInvalidos;
            this.cantidadIncompletos = cantidadIncompletos;
            this.regexOk = regexOk;
            this.resumenOk = resumenOk;
            this.lineaResumen = lineaResumen;
            this.errores = new ArrayList<>(errores);
        }

        public boolean estaRegexOk() {
            return regexOk;
        }

        public int obtenerTokensAnalizados() {
            return tokensAnalizados;
        }

        public int obtenerCantidadMedio() {
            return cantidadMedio;
        }

        public int obtenerCantidadSimple() {
            return cantidadSimple;
        }

        public int obtenerCantidadAlto() {
            return cantidadAlto;
        }

        public int obtenerCantidadInvalidos() {
            return cantidadInvalidos;
        }

        public int obtenerCantidadIncompletos() {
            return cantidadIncompletos;
        }

        public String construirReporte() {
            StringBuilder constructor = new StringBuilder();

            constructor.append("Analisis regex de invariantes de transicion:")
                    .append(System.lineSeparator());

            constructor.append("Tokens analizados: ")
                    .append(tokensAnalizados)
                    .append(System.lineSeparator());

            constructor.append("Medio: ")
                    .append(cantidadMedio)
                    .append(System.lineSeparator());

            constructor.append("Simple: ")
                    .append(cantidadSimple)
                    .append(System.lineSeparator());

            constructor.append("Alto: ")
                    .append(cantidadAlto)
                    .append(System.lineSeparator());

            constructor.append("Invalidos: ")
                    .append(cantidadInvalidos)
                    .append(System.lineSeparator());

            constructor.append("Incompletos: ")
                    .append(cantidadIncompletos)
                    .append(System.lineSeparator());

            constructor.append("Resumen OK: ")
                    .append(resumenOk)
                    .append(System.lineSeparator());

            if (lineaResumen != null) {
                constructor.append("Tiempo segun RESUMEN: ")
                        .append(lineaResumen.tiempoMillis)
                        .append(" ms")
                        .append(System.lineSeparator());
            }

            constructor.append("Regex OK: ")
                    .append(regexOk)
                    .append(System.lineSeparator());

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
    }
}

package ar.edu.unc.concurrente.analisis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Analiza la traza global, sin ID de tokens, extrayendo T-invariantes. */
public class AnalizadorRegexInvariantesTransicion {
    private static final Pattern DISPARO = Pattern.compile("^disparada=(T(?:[0-9]|10|11))$");
    private static final Pattern RESUMEN = Pattern.compile(
            "^RESUMEN completo=(\\d+) medio=(\\d+) simple=(\\d+) alto=(\\d+) "
            + "invariantesPlazaCumplidos=(true|false) tiempoMillis=(\\d+)$");

    /*
     * UNA sola expresion para los tres caminos, permitiendo intercalaciones.
     * Cada grupo 'gN' preserva las transiciones intercaladas cuando se elimina
     * el recorrido reconocido. Las comas delimitan tokens (T1 != T10).
     */
    private static final Pattern INVARIANTES = Pattern.compile(
            "T0,(?<g1>.*?)T1,(?<g2>.*?)"
            + "(?:"
            + "(?<medio>T2,)(?<g3>.*?)T3,(?<g4>.*?)T4,(?<g5>.*?)"
            + "|(?<simple>T5,)(?<g6>.*?)T6,(?<g7>.*?)"
            + "|(?<alto>T7,)(?<g8>.*?)T8,(?<g9>.*?)T9,(?<g10>.*?)T10,(?<g11>.*?)"
            + ")T11,");
    private static final String[] HUECOS = {
            "g1", "g2", "g3", "g4", "g5", "g6", "g7", "g8", "g9", "g10", "g11"
    };

    public ResultadoAnalisisRegex analizar(Path rutaLog) {
        StringBuilder traza = new StringBuilder();
        List<String> errores = new ArrayList<>();
        LineaResumen resumen = null;
        int cantidadResumen = 0;
        int numeroLinea = 0;
        boolean terminoResumen = false;

        try {
            for (String linea : Files.readAllLines(rutaLog)) {
                numeroLinea++;
                Matcher disparo = DISPARO.matcher(linea);
                if (disparo.matches()) {
                    if (terminoResumen) errores.add("Disparo luego del RESUMEN en linea " + numeroLinea);
                    traza.append(disparo.group(1)).append(',');
                } else if (linea.startsWith("disparada=")) {
                    errores.add("Disparo mal formado en linea " + numeroLinea);
                } else if (linea.startsWith("RESUMEN")) {
                    terminoResumen = true;
                    cantidadResumen++;
                    Matcher datos = RESUMEN.matcher(linea);
                    if (!datos.matches()) {
                        errores.add("RESUMEN mal formado en linea " + numeroLinea);
                    } else {
                        try {
                            resumen = new LineaResumen(
                                    Integer.parseInt(datos.group(1)),
                                    Integer.parseInt(datos.group(2)),
                                    Integer.parseInt(datos.group(3)),
                                    Integer.parseInt(datos.group(4)),
                                    Boolean.parseBoolean(datos.group(5)),
                                    Long.parseLong(datos.group(6)));
                        } catch (NumberFormatException excepcion) {
                            errores.add("Numero fuera de rango en RESUMEN");
                        }
                    }
                }
            }
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo leer el log: " + rutaLog, excepcion);
        }
        if (cantidadResumen != 1) errores.add("El log debe tener exactamente un RESUMEN");
        if (traza.length() == 0) errores.add("El log no contiene disparos");

        int medio = 0, simple = 0, alto = 0;
        String restante = traza.toString();
        // Equivalente al re.subn del ejemplo en Python: extraer y conservar huecos.
        while (true) {
            Matcher encontrados = INVARIANTES.matcher(restante);
            StringBuffer nueva = new StringBuffer();
            int coincidencias = 0;
            while (encontrados.find()) {
                coincidencias++;
                if (encontrados.group("medio") != null) medio++;
                else if (encontrados.group("simple") != null) simple++;
                else alto++;
                StringBuilder huecos = new StringBuilder();
                for (String nombre : HUECOS) {
                    String grupo = encontrados.group(nombre);
                    if (grupo != null) huecos.append(grupo);
                }
                encontrados.appendReplacement(nueva, Matcher.quoteReplacement(huecos.toString()));
            }
            encontrados.appendTail(nueva);
            if (coincidencias == 0) break;
            restante = nueva.toString();
        }

        int completos = medio + simple + alto;
        int incompletos = restante.isEmpty() ? 0 : 1;
        if (incompletos > 0) errores.add("Quedaron disparos sin asociar a un invariante: "
                + (restante.length() > 160 ? restante.substring(0, 160) + "..." : restante));
        boolean resumenValido = resumen != null;
        if (resumen != null) {
            if (resumen.completo() != completos || resumen.medio() != medio
                    || resumen.simple() != simple || resumen.alto() != alto) {
                errores.add("Los conteos de regex no coinciden con RESUMEN");
                resumenValido = false;
            }
            if (!resumen.invariantesPlazaCumplidos()) {
                errores.add("El RESUMEN indica invariantes de plaza incumplidos");
                resumenValido = false;
            }
        }
        int invalidos = Math.max(0, errores.size() - incompletos);
        boolean valido = errores.isEmpty() && resumenValido;
        return new ResultadoAnalisisRegex(completos, medio, simple, alto,
                invalidos, incompletos, valido, resumenValido, resumen, errores);
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
                LineaResumen lineaResumen,
                List<String> errores
        ) {
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
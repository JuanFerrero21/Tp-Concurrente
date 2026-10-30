package ar.edu.unc.concurrente.analisis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Analiza la traza global de disparos con UNA sola regex, como en la filmina de expresiones regulares. */
public class AnalizadorRegexInvariantesTransicion {
    private static final Pattern DISPARO = Pattern.compile("^disparada=(T(?:[0-9]|10|11))$");
    private static final Pattern RESUMEN = Pattern.compile(
            "^RESUMEN completo=(\\d+) medio=(\\d+) simple=(\\d+) alto=(\\d+) "
            + "invariantesPlazaCumplidos=(true|false) tiempoMillis=(\\d+)$");

    /*
     * UNA sola expresion para los tres caminos, con grupos numerados como en
     * las filminas: (T0)(.*?)(T1)(.*?)(...). Los grupos pares son los huecos
     * donde pueden aparecer transiciones de otros invariantes intercaladas.
     * Las comas delimitan tokens (T1 != T10).
     *   medio : grupos 5 a 10    simple: grupos 11 a 14    alto: grupos 15 a 22
     */
    private static final Pattern INVARIANTES = Pattern.compile(
            "(T0,)(.*?)(T1,)(.*?)"
            + "(?:"
            + "(T2,)(.*?)(T3,)(.*?)(T4,)(.*?)"
            + "|(T5,)(.*?)(T6,)(.*?)"
            + "|(T7,)(.*?)(T8,)(.*?)(T9,)(.*?)(T10,)(.*?)"
            + ")(T11,)");
    private static final int GRUPO_T2 = 5;
    private static final int GRUPO_T5 = 11;
    private static final int[] HUECOS = {2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22};

    public ResultadoAnalisisRegex analizar(Path rutaLog) {
        List<String> errores = new ArrayList<>();
        StringBuilder traza = new StringBuilder();
        Matcher resumen = null;
        int cantidadResumen = 0;

        // 1) Leer el log: cada disparo se agrega a la traza, el RESUMEN se guarda aparte.
        try {
            for (String linea : Files.readAllLines(rutaLog)) {
                Matcher disparo = DISPARO.matcher(linea);
                if (disparo.matches()) {
                    traza.append(disparo.group(1)).append(',');
                } else if (linea.startsWith("disparada=")) {
                    errores.add("Disparo mal formado: " + linea);
                } else if (linea.startsWith("RESUMEN")) {
                    cantidadResumen++;
                    Matcher datos = RESUMEN.matcher(linea);
                    if (datos.matches()) resumen = datos;
                    else errores.add("RESUMEN mal formado: " + linea);
                }
            }
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo leer el log: " + rutaLog, excepcion);
        }
        if (cantidadResumen != 1) errores.add("El log debe tener exactamente un RESUMEN");
        if (traza.length() == 0) errores.add("El log no contiene disparos");

        // 2) Equivalente al re.subn del ejemplo en Python: reemplazar cada coincidencia
        //    por sus huecos y repetir hasta que no haya mas coincidencias.
        int medio = 0, simple = 0, alto = 0;
        String restante = traza.toString();
        while (true) {
            Matcher encontrados = INVARIANTES.matcher(restante);
            StringBuffer nueva = new StringBuffer();
            int coincidencias = 0;
            while (encontrados.find()) {
                coincidencias++;
                if (encontrados.group(GRUPO_T2) != null) medio++;
                else if (encontrados.group(GRUPO_T5) != null) simple++;
                else alto++;
                StringBuilder huecos = new StringBuilder();
                for (int numero : HUECOS) {
                    String hueco = encontrados.group(numero);
                    if (hueco != null) huecos.append(hueco);
                }
                encontrados.appendReplacement(nueva, Matcher.quoteReplacement(huecos.toString()));
            }
            encontrados.appendTail(nueva);
            if (coincidencias == 0) break;
            restante = nueva.toString();
        }

        // 3) Lo que no coincidio con ningun invariante es un error; despues se contrasta con el RESUMEN.
        if (!restante.isEmpty()) errores.add("Disparos sin asociar a un invariante: " + restante);
        long tiempoMillis = -1;
        if (resumen != null) {
            tiempoMillis = Long.parseLong(resumen.group(6));
            if (!resumen.group(1).equals(String.valueOf(medio + simple + alto))
                    || !resumen.group(2).equals(String.valueOf(medio))
                    || !resumen.group(3).equals(String.valueOf(simple))
                    || !resumen.group(4).equals(String.valueOf(alto))) {
                errores.add("Los conteos de regex no coinciden con RESUMEN");
            }
            if (resumen.group(5).equals("false")) {
                errores.add("El RESUMEN indica invariantes de plaza incumplidos");
            }
        }
        return new ResultadoAnalisisRegex(medio, simple, alto, tiempoMillis, errores);
    }

    public record ResultadoAnalisisRegex(int medio, int simple, int alto, long tiempoMillis, List<String> errores) {
        public boolean esAnalisisRegexValido() {
            return errores.isEmpty();
        }

        public String construirReporte() {
            StringBuilder reporte = new StringBuilder(String.format(
                    "Analisis regex de invariantes de transicion:%n"
                    + "Invariantes completos: %d%n"
                    + "Medio: %d%nSimple: %d%nAlto: %d%n",
                    medio + simple + alto, medio, simple, alto));
            if (tiempoMillis >= 0) reporte.append(String.format("Tiempo segun RESUMEN: %d ms%n", tiempoMillis));
            reporte.append(String.format("Analisis regex valido: %b%n", esAnalisisRegexValido()));
            if (!errores.isEmpty()) {
                reporte.append(String.format("Errores detectados:%n"));
                for (String error : errores) {
                    String corto = error.length() > 200 ? error.substring(0, 200) + "..." : error;
                    reporte.append(String.format("- %s%n", corto));
                }
            }
            return reporte.toString();
        }
    }
}

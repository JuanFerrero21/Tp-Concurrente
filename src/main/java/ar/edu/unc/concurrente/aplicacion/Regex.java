package ar.edu.unc.concurrente.aplicacion;

import ar.edu.unc.concurrente.analisis.AnalizadorRegexInvariantesTransicion;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Regex {
    private static final Path RUTA_LOG = Paths.get("logs", "simulacion.log");

    public static void main(String[] args) {
        Path rutaLog = obtenerRutaLog(args);

        AnalizadorRegexInvariantesTransicion analizadorRegex = new AnalizadorRegexInvariantesTransicion();
        AnalizadorRegexInvariantesTransicion.ResultadoAnalisisRegex resultadoRegex = analizadorRegex.analizar(rutaLog);

        System.out.println("Log analizado: " + rutaLog);
        System.out.print(resultadoRegex.construirReporte());

        if (!resultadoRegex.esAnalisisRegexValido()) {
            System.err.println("El analisis regex de invariantes de transicion fallo");
            System.exit(1);
        }
    }

    private static Path obtenerRutaLog(String[] args) {
        if (args.length == 0) {
            return RUTA_LOG;
        }

        if (args.length == 1) {
            return Paths.get(args[0]);
        }

        throw new IllegalArgumentException("Uso: java ar.edu.unc.concurrente.aplicacion.Regex [ruta-log]");
    }
}

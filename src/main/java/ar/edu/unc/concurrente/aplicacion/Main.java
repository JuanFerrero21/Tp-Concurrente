package ar.edu.unc.concurrente.aplicacion;

import ar.edu.unc.concurrente.analisis.VerificadorInvariantes;
import ar.edu.unc.concurrente.analisis.ResultadoSimulacion;
import ar.edu.unc.concurrente.analisis.AnalizadorRegexInvariantesTransicion;
import ar.edu.unc.concurrente.analisis.EstadoSimulacion;
import ar.edu.unc.concurrente.configuracion.ConfiguracionSimulacion;
import ar.edu.unc.concurrente.registro.RegistradorTransiciones;
import ar.edu.unc.concurrente.monitor.Monitor;
import ar.edu.unc.concurrente.monitor.MonitorInterface;
import ar.edu.unc.concurrente.redpetri.RedDePetri;
import ar.edu.unc.concurrente.politica.Politica;
import ar.edu.unc.concurrente.politica.PoliticaPrioridadSimple;
import ar.edu.unc.concurrente.politica.PoliticaAleatoria;
import ar.edu.unc.concurrente.hilo.Hilo;
import ar.edu.unc.concurrente.tiempo.SensibilizadoConTiempo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {
    /*
     * Configuracion principal de la ejecucion.
     *
     * Cambiar estos valores para probar distintos casos:
     *
     * NOMBRE_POLITICA:
     * - "prioridad" para priorizar el modo simple.
     * - "aleatorio" para politica aleatoria.
     *
     * OBJETIVO_INVARIANTES_COMPLETOS:
     * - cantidad de invariantes completos que debe ejecutar el sistema.
     *
     * DETALLADO:
     * - true para ver detalle de disparos y esperas por consola.
     * - false para ejecucion normal.
     *
     * RUTA_LOG:
     * - archivo donde se registran las transiciones disparadas y los invariantes.
     */
    private static final String NOMBRE_POLITICA = "prioridad";
    private static final int OBJETIVO_INVARIANTES_COMPLETOS = 200;
    private static final boolean DETALLADO = false;
    private static final Path RUTA_LOG = Paths.get("logs", "simulation.log");

    public static void main(String[] args) throws InterruptedException {
        long inicioMillis = System.currentTimeMillis();

        ConfiguracionSimulacion config = ConfiguracionSimulacion.configuracionPorDefecto(OBJETIVO_INVARIANTES_COMPLETOS);
        RedDePetri redDePetri = new RedDePetri(config.obtenerMarcadoInicial(), config.obtenerMatrizIncidencia());

        EstadoSimulacion estadoSimulacion = new EstadoSimulacion(
                config.obtenerObjetivoInvariantesCompletos(),
                config.obtenerTransicionIngreso(),
                config.obtenerTransicionFinalizacion(),
                config.obtenerTransicionesConflicto()
        );

        Politica politica = crearPolitica(NOMBRE_POLITICA, config);

        SensibilizadoConTiempo sensibilizadoConTiempo = new SensibilizadoConTiempo(
                config.obtenerTransicionesTemporales(),
                config.obtenerAlfaMillis(),
                config.obtenerBetaMillis()
        );

        RegistradorTransiciones registradorTransiciones = new RegistradorTransiciones(
                redDePetri.obtenerCantidadTransiciones(),
                RUTA_LOG,
                NOMBRE_POLITICA,
                config.obtenerObjetivoInvariantesCompletos(),
                config.obtenerTransicionesTemporales(),
                config.obtenerAlfaMillis(),
                config.obtenerBetaMillis()
        );

        VerificadorInvariantes verificadorInvariantes = new VerificadorInvariantes(config.obtenerMarcadoInicial());

        MonitorInterface monitor = new Monitor(
                redDePetri,
                politica,
                estadoSimulacion,
                sensibilizadoConTiempo,
                DETALLADO,
                registradorTransiciones,
                verificadorInvariantes
        );

        System.out.println("Politica: " + NOMBRE_POLITICA);
        System.out.println("Marcado inicial: " + config.obtenerMarcadoInicial());
        System.out.println("Transiciones sensibilizadas: " + redDePetri.obtenerTransicionesSensibilizadas());
        System.out.println("Objetivo de invariantes completos: " + config.obtenerObjetivoInvariantesCompletos());
        System.out.println("Transiciones temporales: " + Arrays.toString(config.obtenerTransicionesTemporales()));
        System.out.println("Alfa ms: " + Arrays.toString(config.obtenerAlfaMillis()));
        System.out.println("Beta ms: " + Arrays.toString(config.obtenerBetaMillis()));

        Hilo[] hilos = crearHilos(config, monitor, estadoSimulacion, registradorTransiciones);

        for (Hilo hilo : hilos) {
            hilo.start();
        }

        for (Hilo hilo : hilos) {
            hilo.join();
        }

        long tiempoTranscurridoMillis = System.currentTimeMillis() - inicioMillis;

        boolean invariantesPlazaOk = verificadorInvariantes.seCumplenInvariantesPlaza(redDePetri.obtenerMarcado());

        ResultadoSimulacion resultado = new ResultadoSimulacion(
                redDePetri.obtenerMarcado(),
                registradorTransiciones.obtenerIntentosTotales(),
                registradorTransiciones.obtenerDisparosTotales(),
                registradorTransiciones.obtenerDisparosPorTransicion(),
                invariantesPlazaOk
        );

        registradorTransiciones.escribirResumen(
                estadoSimulacion.obtenerInvariantesCompletos(),
                estadoSimulacion.obtenerCompletosPorModo(),
                resultado.invarianteOk(),
                tiempoTranscurridoMillis
        );

        registradorTransiciones.close();

        AnalizadorRegexInvariantesTransicion analizadorRegex = new AnalizadorRegexInvariantesTransicion();
        AnalizadorRegexInvariantesTransicion.ResultadoAnalisisRegex resultadoRegex = analizadorRegex.analizar(RUTA_LOG);

        if (!resultadoRegex.estaRegexOk()) {
            throw new IllegalStateException("El analisis regex de invariantes de transicion fallo");
        }

        System.out.println("Resumen: " + resultado);
        System.out.println("Reporte de invariantes de plaza:");
        System.out.print(verificadorInvariantes.construirReporte(redDePetri.obtenerMarcado()));
        System.out.print(resultadoRegex.construirReporte());
        System.out.println("Invariantes completos: "
                + estadoSimulacion.obtenerInvariantesCompletos()
                + "/"
                + estadoSimulacion.obtenerObjetivoInvariantesCompletos());

        System.out.println("Invariantes iniciados: " + estadoSimulacion.obtenerInvariantesIniciados());

        System.out.println("Procesados por modo [medio, simple, alto]: "
                + Arrays.toString(estadoSimulacion.obtenerCompletosPorModo()));

        System.out.println("Tiempo total de ejecucion: " + tiempoTranscurridoMillis + " ms");
        System.out.println("Log generado en: " + RUTA_LOG);
    }

    private static Hilo[] crearHilos(
            ConfiguracionSimulacion config,
            MonitorInterface monitor,
            EstadoSimulacion estadoSimulacion,
            RegistradorTransiciones registradorTransiciones
    ) {
        int[][] transicionesHilos = config.obtenerTransicionesHilos();
        Hilo[] hilos = new Hilo[config.obtenerCantidadHilos()];

        for (int i = 0; i < hilos.length; i++) {
            hilos[i] = new Hilo(
                    "Hilo-" + (i + 1),
                    monitor,
                    transicionesHilos[i],
                    estadoSimulacion,
                    registradorTransiciones
            );
        }

        return hilos;
    }

    private static Politica crearPolitica(String nombrePolitica, ConfiguracionSimulacion config) {
        if ("prioridad".equalsIgnoreCase(nombrePolitica)) {
            return new PoliticaPrioridadSimple(config.obtenerTransicionModoSimple(), config.obtenerTransicionesConflicto());
        }

        if ("aleatorio".equalsIgnoreCase(nombrePolitica) || "aleatoria".equalsIgnoreCase(nombrePolitica)) {
            return new PoliticaAleatoria(config.obtenerTransicionesConflicto());
        }

        throw new IllegalArgumentException("Politica desconocida: " + nombrePolitica);
    }
}

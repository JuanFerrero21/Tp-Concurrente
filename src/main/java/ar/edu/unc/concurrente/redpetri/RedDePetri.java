package ar.edu.unc.concurrente.redpetri;

import ar.edu.unc.concurrente.analisis.EstadoSimulacion;
import ar.edu.unc.concurrente.analisis.VerificadorInvariantes;
import ar.edu.unc.concurrente.registro.RegistradorTransiciones;
import ar.edu.unc.concurrente.tiempo.SensibilizadoConTiempo;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class RedDePetri {
    private Marcado marcado;
    private final int[][] matrizIncidencia;
    private SensibilizadoConTiempo sensibilizadoConTiempo;
    private VerificadorInvariantes verificadorInvariantes;
    private EstadoSimulacion estadoSimulacion;
    private RegistradorTransiciones registradorTransiciones;

    public RedDePetri(Marcado marcadoInicial, int[][] matrizIncidencia) {
        validarMatriz(marcadoInicial, matrizIncidencia);
        this.marcado = marcadoInicial;
        this.matrizIncidencia = copiarMatriz(matrizIncidencia);
    }

    public void configurarSemanticaTemporal(SensibilizadoConTiempo sensibilizadoConTiempo) {
        this.sensibilizadoConTiempo = sensibilizadoConTiempo;
        actualizarSensibilizadasTemporales();
    }

    public void configurarVerificadorInvariantes(VerificadorInvariantes verificadorInvariantes) {
        this.verificadorInvariantes = verificadorInvariantes;
        verificarInvariantesPlaza();
    }

    public void configurarEstadoSimulacion(EstadoSimulacion estadoSimulacion) {
        this.estadoSimulacion = estadoSimulacion;
    }

    public void configurarRegistradorTransiciones(RegistradorTransiciones registradorTransiciones) {
        this.registradorTransiciones = registradorTransiciones;
    }

    public boolean estaSensibilizada(int transicion) {
        validarTransicion(transicion);
        Marcado siguienteMarcado = calcularSiguienteMarcado(transicion);
        return !siguienteMarcado.tieneTokensNegativos();
    }

    public boolean disparar(int transicion) {
        if (!puedeDisparar(transicion)) {
            return false;
        }

        marcado = calcularSiguienteMarcado(transicion);
        verificarInvariantesPlaza();
        actualizarSensibilizadasTemporales();
        registrarDisparo(transicion);
        return true;
    }

    public boolean puedeDisparar(int transicion) {
        return puedeDispararPorEstadoSimulacion(transicion)
                && estaSensibilizada(transicion)
                && cumpleRestriccionTemporal(transicion);
    }

    public boolean estaAntesDeLaVentanaTemporal(int transicion) {
        validarTransicion(transicion);

        if (!estaSensibilizada(transicion)) {
            return false;
        }

        if (sensibilizadoConTiempo == null || !sensibilizadoConTiempo.esTemporal(transicion)) {
            return false;
        }

        return sensibilizadoConTiempo.estaAntesDeLaVentana(transicion);
    }

    public long milisegundosHastaVentanaTemporal(int transicion) {
        validarTransicion(transicion);

        if (!estaAntesDeLaVentanaTemporal(transicion)) {
            return 0L;
        }

        return sensibilizadoConTiempo.milisegundosHastaAlfa(transicion);
    }

    public String describirMotivoEspera(int transicion) {
        validarTransicion(transicion);

        if (!estaSensibilizada(transicion)) {
            return "sin tokens suficientes";
        }

        if (sensibilizadoConTiempo == null || !sensibilizadoConTiempo.esTemporal(transicion)) {
            return "la transicion esta lista para disparar";
        }

        return sensibilizadoConTiempo.describirEstado(transicion);
    }

    public Set<Integer> obtenerTransicionesSensibilizadas() {
        Set<Integer> transicionesSensibilizadas = new LinkedHashSet<>();
        for (int transicion = 0; transicion < obtenerCantidadTransiciones(); transicion++) {
            if (estaSensibilizada(transicion)) {
                transicionesSensibilizadas.add(transicion);
            }
        }

        return transicionesSensibilizadas;
    }

    public Set<Integer> obtenerTransicionesDisparables() {
        Set<Integer> transicionesDisparables = new LinkedHashSet<>();

        for (int transicion = 0; transicion < obtenerCantidadTransiciones(); transicion++) {
            if (puedeDisparar(transicion)) {
                transicionesDisparables.add(transicion);
            }
        }

        return transicionesDisparables;
    }

    public Marcado obtenerMarcado() {
        return marcado.copia();
    }

    public int obtenerCantidadPlazas() {
        return matrizIncidencia.length;
    }

    public int obtenerCantidadTransiciones() {
        return matrizIncidencia[0].length;
    }

    public boolean seCumplenInvariantesPlaza() {
        return verificadorInvariantes == null || verificadorInvariantes.seCumplenInvariantesPlaza(obtenerMarcado());
    }

    public String construirReporteInvariantesPlaza() {
        return verificadorInvariantes == null ? "" : verificadorInvariantes.construirReporte(obtenerMarcado());
    }

    public boolean simulacionFinalizada() {
        return estadoSimulacion != null && estadoSimulacion.estaFinalizada();
    }

    public boolean puedeDispararPorEstadoSimulacion(int transicion) {
        return estadoSimulacion == null || estadoSimulacion.puedeDispararTransicion(transicion);
    }

    private void registrarDisparo(int transicion) {
        if (estadoSimulacion != null) {
            estadoSimulacion.registrarTransicionDisparada(transicion);
        }

        if (registradorTransiciones != null) {
            registradorTransiciones.registrarEventoDisparo(transicion);
        }
    }

    private void verificarInvariantesPlaza() {
        if (verificadorInvariantes != null) {
            verificadorInvariantes.verificarInvariantesPlazaOLanzar(obtenerMarcado());
        }
    }

    private void actualizarSensibilizadasTemporales() {
        if (sensibilizadoConTiempo != null) {
            sensibilizadoConTiempo.actualizarSensibilizadas(obtenerTransicionesSensibilizadas());
        }
    }

    private boolean cumpleRestriccionTemporal(int transicion) {
        if (sensibilizadoConTiempo == null || !sensibilizadoConTiempo.esTemporal(transicion)) {
            return true;
        }

        return sensibilizadoConTiempo.puedeDispararAhora(transicion);
    }

    private Marcado calcularSiguienteMarcado(int transicion) {
        return marcado.sumar(obtenerColumna(transicion));
    }

    private int[] obtenerColumna(int transicion) {
        int[] columna = new int[obtenerCantidadPlazas()];
        for (int plaza = 0; plaza < obtenerCantidadPlazas(); plaza++) {
            columna[plaza] = matrizIncidencia[plaza][transicion];
        }

        return columna;
    }

    private void validarTransicion(int transicion) {
        if (transicion < 0 || transicion >= obtenerCantidadTransiciones()) {
            throw new IllegalArgumentException("Transicion fuera de rango: " + transicion);
        }
    }

    private static void validarMatriz(Marcado marcadoInicial, int[][] matrizIncidencia) {
        if (matrizIncidencia.length == 0) {
            throw new IllegalArgumentException("La matriz de incidencia no puede estar vacia");
        }

        int cantidadTransiciones = matrizIncidencia[0].length;
        if (cantidadTransiciones == 0) {
            throw new IllegalArgumentException("La matriz debe tener al menos una transicion");
        }

        if (marcadoInicial.cantidad() != matrizIncidencia.length) {
            throw new IllegalArgumentException("El marcado inicial debe tener una componente por cada plaza");
        }

        for (int[] fila : matrizIncidencia) {
            if (fila.length != cantidadTransiciones) {
                throw new IllegalArgumentException("Todas las filas de la matriz deben tener la misma longitud");
            }
        }
    }

    private static int[][] copiarMatriz(int[][] matriz) {
        int[][] copia = new int[matriz.length][];
        for (int i = 0; i < matriz.length; i++) {
            copia[i] = Arrays.copyOf(matriz[i], matriz[i].length);
        }

        return copia;
    }

}

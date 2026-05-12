package ar.edu.unc.concurrente.redpetri;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class RedDePetri {
    private Marcado marcado;
    private final int[][] matrizIncidencia;

    public RedDePetri(Marcado marcadoInicial, int[][] matrizIncidencia) {
        validarMatriz(marcadoInicial, matrizIncidencia);
        this.marcado = marcadoInicial;
        this.matrizIncidencia = copiarMatriz(matrizIncidencia);
    }

    public boolean estaSensibilizada(int transicion) {
        validarTransicion(transicion);
        Marcado siguienteMarcado = calcularSiguienteMarcado(transicion);
        return !siguienteMarcado.tieneTokensNegativos();
    }

    public boolean disparar(int transicion) {
        if (!estaSensibilizada(transicion)) {
            return false;
        }

        marcado = calcularSiguienteMarcado(transicion);
        return true;
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

    public Marcado obtenerMarcado() {
        return marcado.copia();
    }

    public int obtenerCantidadPlazas() {
        return matrizIncidencia.length;
    }

    public int obtenerCantidadTransiciones() {
        return matrizIncidencia[0].length;
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

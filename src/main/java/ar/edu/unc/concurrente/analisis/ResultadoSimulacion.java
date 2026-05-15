package ar.edu.unc.concurrente.analisis;

import ar.edu.unc.concurrente.redpetri.Marcado;

import java.util.Arrays;

public class ResultadoSimulacion {
    private final Marcado marcadoFinal;
    private final int intentosTotales;
    private final int disparosTotales;
    private final int[] disparosPorTransicion;
    private final boolean invariantesPlazaCumplidos;

    public ResultadoSimulacion(Marcado marcadoFinal, int intentosTotales, int disparosTotales, int[] disparosPorTransicion, boolean invariantesPlazaCumplidos) {
        this.marcadoFinal = marcadoFinal.copia();
        this.intentosTotales = intentosTotales;
        this.disparosTotales = disparosTotales;
        this.disparosPorTransicion = Arrays.copyOf(disparosPorTransicion, disparosPorTransicion.length);
        this.invariantesPlazaCumplidos = invariantesPlazaCumplidos;
    }

    public Marcado obtenerMarcadoFinal() {
        return marcadoFinal.copia();
    }

    public int obtenerIntentosTotales() {
        return intentosTotales;
    }

    public int obtenerDisparosTotales() {
        return disparosTotales;
    }

    public int[] obtenerDisparosPorTransicion() {
        return Arrays.copyOf(disparosPorTransicion, disparosPorTransicion.length);
    }

    public boolean seCumplenInvariantesPlaza() {
        return invariantesPlazaCumplidos;
    }

    @Override
    public String toString() {
        return "ResultadoSimulacion{" +
                "marcadoFinal=" + marcadoFinal +
                ", intentosTotales=" + intentosTotales +
                ", disparosTotales=" + disparosTotales +
                ", disparosPorTransicion=" + Arrays.toString(disparosPorTransicion) +
                ", invariantesPlazaCumplidos=" + invariantesPlazaCumplidos +
                '}';
    }
}
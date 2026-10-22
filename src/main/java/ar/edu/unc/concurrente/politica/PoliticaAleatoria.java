package ar.edu.unc.concurrente.politica;

import java.util.Random;
import java.util.Set;

public class PoliticaAleatoria implements Politica {
    private final Random aleatorio;

    public PoliticaAleatoria() {
        this.aleatorio = new Random();
    }

    @Override
    public int elegirTransicion(Set<Integer> transicionesSensibilizadas) {
        return Politica.elegirAlAzar(transicionesSensibilizadas, aleatorio);
    }
}

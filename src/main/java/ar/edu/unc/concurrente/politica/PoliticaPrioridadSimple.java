package ar.edu.unc.concurrente.politica;

import java.util.Random;
import java.util.Set;

public class PoliticaPrioridadSimple implements Politica {
    private final Integer transicionModoSimple;
    private final Random aleatorio;

    public PoliticaPrioridadSimple(int transicionModoSimple) {
        this.transicionModoSimple = transicionModoSimple;
        this.aleatorio = new Random();
    }

    @Override
    public int elegirTransicion(Set<Integer> transicionesSensibilizadas) {
        /*
         * Politica priorizada:
         * si la transicion del modo simple esta disponible como candidata,
         * se la elige siempre.
         */
        if (transicionModoSimple != null && transicionesSensibilizadas.contains(transicionModoSimple)) {
            return transicionModoSimple;
        }

        /*
         * Si no esta disponible el modo simple, se elige aleatoriamente
         * entre todas las transiciones disponibles, sin distinguir si
         * pertenecen o no al conflicto.
         */
        return Politica.elegirAlAzar(transicionesSensibilizadas, aleatorio);
    }
}

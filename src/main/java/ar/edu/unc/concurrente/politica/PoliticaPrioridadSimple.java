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
        if (transicionesSensibilizadas.isEmpty()) {
            throw new IllegalArgumentException("No hay transiciones habilitadas para elegir");
        }

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
        int indiceSeleccionado = aleatorio.nextInt(transicionesSensibilizadas.size());
        int indice = 0;
        for (int transicion : transicionesSensibilizadas) {
            if (indice == indiceSeleccionado) {
                return transicion;
            }
            indice++;
        }

        throw new IllegalStateException("No se pudo seleccionar una transicion");
    }
}

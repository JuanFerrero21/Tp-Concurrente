package ar.edu.unc.concurrente.politica;

import java.util.Set;

public interface Politica {
    int elegirTransicion(Set<Integer> transicionesSensibilizadas);
}

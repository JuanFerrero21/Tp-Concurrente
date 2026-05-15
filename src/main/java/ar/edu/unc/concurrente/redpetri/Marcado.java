package ar.edu.unc.concurrente.redpetri;

import java.util.Arrays;

public class Marcado {
    private final int[] tokens;

    public Marcado(int[] tokens) {
        this.tokens = Arrays.copyOf(tokens, tokens.length);
    }

    public int obtenerTokens(int plaza) {
        return tokens[plaza];
    }

    public Marcado sumar(int[] valores) {
        if (valores.length != tokens.length) {
            throw new IllegalArgumentException("El vector debe tener la misma cantidad de plazas que el marcado");
        }

        int[] resultado = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            resultado[i] = tokens[i] + valores[i];
        }

        return new Marcado(resultado);
    }

    public boolean tieneTokensNegativos() {
        for (int token : tokens) {
            if (token < 0) {
                return true;
            }
        }

        return false;
    }

    public int cantidad() {
        return tokens.length;
    }

    public Marcado copia() {
        return new Marcado(tokens);
    }

    public int[] obtenerTokensComoArreglo() {
        return Arrays.copyOf(tokens, tokens.length);
    }

    @Override
    public String toString() {
        return Arrays.toString(tokens);
    }
}
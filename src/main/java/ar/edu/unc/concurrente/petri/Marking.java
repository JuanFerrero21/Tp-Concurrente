package ar.edu.unc.concurrente.petri;

import java.util.Arrays;

public class Marking {
    private final int[] tokens;

    public Marking(int[] tokens) {
        this.tokens = Arrays.copyOf(tokens, tokens.length);
    }

    public int getTokens(int place) {
        return tokens[place];
    }

    public Marking add(int[] values) {
        if (values.length != tokens.length) {
            throw new IllegalArgumentException("El vector debe tener la misma cantidad de plazas que el marcado");
        }

        int[] result = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            result[i] = tokens[i] + values[i];
        }

        return new Marking(result);
    }

    public boolean hasNegativeTokens() {
        for (int token : tokens) {
            if (token < 0) {
                return true;
            }
        }

        return false;
    }

    public int size() {
        return tokens.length;
    }

    public Marking copy() {
        return new Marking(tokens);
    }

    public int[] toArray() {
        return Arrays.copyOf(tokens, tokens.length);
    }

    @Override
    public String toString() {
        return Arrays.toString(tokens);
    }
}

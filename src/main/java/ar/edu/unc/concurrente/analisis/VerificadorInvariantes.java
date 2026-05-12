package ar.edu.unc.concurrente.analisis;

import ar.edu.unc.concurrente.redpetri.Marcado;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VerificadorInvariantes {
    private final List<InvariantePlaza> invariantesPlaza;

    public VerificadorInvariantes(Marcado marcadoInicial) {
        this.invariantesPlaza = crearInvariantesPlazaPorDefecto(marcadoInicial);
    }

    /*
     * Constructor auxiliar para pruebas o casos donde se quieran definir
     * invariantes manualmente.
     */
    public VerificadorInvariantes(List<InvariantePlaza> invariantesPlaza) {
        if (invariantesPlaza == null || invariantesPlaza.isEmpty()) {
            throw new IllegalArgumentException("Debe existir al menos un invariante de plaza");
        }

        this.invariantesPlaza = Collections.unmodifiableList(new ArrayList<>(invariantesPlaza));
    }

    /*
     * Invariantes de plaza obtenidos de la red del TP.
     *
     * I1: conserva la cantidad total de datos dentro del sistema.
     *     No incluye las plazas de recursos P2 y P6.
     *
     * I2: representa el bus de acceso al buffer.
     *     Si P2 tiene token, el bus esta libre.
     *     Si P1 tiene token, hay un dato usando el bus.
     *
     * I3: representa la unidad de procesamiento.
     *     Si P6 tiene token, la unidad esta libre.
     *     Si el token esta en P4, P5, P7, P8, P9 o P10,
     *     la unidad esta ocupada procesando algun dato.
     */
    private static List<InvariantePlaza> crearInvariantesPlazaPorDefecto(Marcado marcadoInicial) {
        validarCantidadPlazas(marcadoInicial, 12);

        List<InvariantePlaza> invariantes = new ArrayList<>();

        invariantes.add(InvariantePlaza.desdeMarcadoInicial(
                "CONSERVACION_DATOS",
                new int[] {0, 1, 3, 4, 5, 7, 8, 9, 10, 11},
                marcadoInicial
        ));

        invariantes.add(InvariantePlaza.desdeMarcadoInicial(
                "BUS_BUFFER",
                new int[] {1, 2},
                marcadoInicial
        ));

        invariantes.add(InvariantePlaza.desdeMarcadoInicial(
                "UNIDAD_PROCESAMIENTO",
                new int[] {4, 5, 6, 7, 8, 9, 10},
                marcadoInicial
        ));

        return invariantes;
    }

    public boolean seCumplenInvariantesPlaza(Marcado marcado) {
        for (InvariantePlaza invariante : invariantesPlaza) {
            if (!invariante.seCumple(marcado)) {
                return false;
            }
        }

        return true;
    }

    public void verificarInvariantesPlazaOLanzar(Marcado marcado) {
        for (InvariantePlaza invariante : invariantesPlaza) {
            int valorActual = invariante.calcular(marcado);

            if (valorActual != invariante.obtenerValorEsperado()) {
                throw new IllegalStateException(
                        "Se rompio el invariante de plaza "
                                + invariante.obtenerNombre()
                                + ". Plazas="
                                + Arrays.toString(invariante.obtenerPlazas())
                                + ", esperado="
                                + invariante.obtenerValorEsperado()
                                + ", obtenido="
                                + valorActual
                                + ", marcado="
                                + marcado
                );
            }
        }
    }

    public String construirReporte(Marcado marcado) {
        StringBuilder constructor = new StringBuilder();

        for (InvariantePlaza invariante : invariantesPlaza) {
            int valorActual = invariante.calcular(marcado);

            constructor.append(invariante.obtenerNombre())
                    .append(": esperado=")
                    .append(invariante.obtenerValorEsperado())
                    .append(", obtenido=")
                    .append(valorActual)
                    .append(", ok=")
                    .append(valorActual == invariante.obtenerValorEsperado())
                    .append(System.lineSeparator());
        }

        return constructor.toString();
    }

    /*
     * Se deja este metodo como utilidad, pero ya no es la verificacion principal.
     * La verificacion importante ahora son los invariantes de plaza reales.
     */
    public boolean conservaTotalTokens(Marcado marcadoInicial, Marcado marcadoFinal) {
        return sumarValores(marcadoInicial) == sumarValores(marcadoFinal);
    }

    public int sumarValores(Marcado marcado) {
        int total = 0;

        for (int token : marcado.aArreglo()) {
            total += token;
        }

        return total;
    }

    private static void validarCantidadPlazas(Marcado marcado, int plazasEsperadas) {
        if (marcado.cantidad() != plazasEsperadas) {
            throw new IllegalArgumentException(
                    "El marcado debe tener "
                            + plazasEsperadas
                            + " plazas, pero tiene "
                            + marcado.cantidad()
            );
        }
    }

    public static class InvariantePlaza {
        private final String nombre;
        private final int[] plazas;
        private final int valorEsperado;

        public InvariantePlaza(String nombre, int[] plazas, int valorEsperado) {
            if (nombre == null || nombre.isBlank()) {
                throw new IllegalArgumentException("El nombre del invariante no puede estar vacio");
            }

            if (plazas == null || plazas.length == 0) {
                throw new IllegalArgumentException("El invariante debe tener al menos una plaza");
            }

            this.nombre = nombre;
            this.plazas = Arrays.copyOf(plazas, plazas.length);
            this.valorEsperado = valorEsperado;
        }

        public static InvariantePlaza desdeMarcadoInicial(String nombre, int[] plazas, Marcado marcadoInicial) {
            InvariantePlaza invariante = new InvariantePlaza(nombre, plazas, 0);
            return new InvariantePlaza(nombre, plazas, invariante.calcular(marcadoInicial));
        }

        public boolean seCumple(Marcado marcado) {
            return calcular(marcado) == valorEsperado;
        }

        public int calcular(Marcado marcado) {
            int total = 0;

            for (int plaza : plazas) {
                if (plaza < 0 || plaza >= marcado.cantidad()) {
                    throw new IllegalArgumentException(
                            "Plaza fuera de rango en invariante "
                                    + nombre
                                    + ": P"
                                    + plaza
                    );
                }

                total += marcado.obtenerTokens(plaza);
            }

            return total;
        }

        public String obtenerNombre() {
            return nombre;
        }

        public int[] obtenerPlazas() {
            return Arrays.copyOf(plazas, plazas.length);
        }

        public int obtenerValorEsperado() {
            return valorEsperado;
        }
    }
}
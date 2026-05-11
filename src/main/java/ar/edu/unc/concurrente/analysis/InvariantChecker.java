package ar.edu.unc.concurrente.analysis;

import ar.edu.unc.concurrente.petri.Marking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InvariantChecker {
    private final List<PlaceInvariant> placeInvariants;

    public InvariantChecker(Marking initialMarking) {
        this.placeInvariants = createDefaultPlaceInvariants(initialMarking);
    }

    /*
     * Constructor auxiliar para pruebas o casos donde se quieran definir
     * invariantes manualmente.
     */
    public InvariantChecker(List<PlaceInvariant> placeInvariants) {
        if (placeInvariants == null || placeInvariants.isEmpty()) {
            throw new IllegalArgumentException("Debe existir al menos un invariante de plaza");
        }

        this.placeInvariants = Collections.unmodifiableList(new ArrayList<>(placeInvariants));
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
    private static List<PlaceInvariant> createDefaultPlaceInvariants(Marking initialMarking) {
        validatePlaceCount(initialMarking, 12);

        List<PlaceInvariant> invariants = new ArrayList<>();

        invariants.add(PlaceInvariant.fromInitialMarking(
                "CONSERVACION_DATOS",
                new int[] {0, 1, 3, 4, 5, 7, 8, 9, 10, 11},
                initialMarking
        ));

        invariants.add(PlaceInvariant.fromInitialMarking(
                "BUS_BUFFER",
                new int[] {1, 2},
                initialMarking
        ));

        invariants.add(PlaceInvariant.fromInitialMarking(
                "UNIDAD_PROCESAMIENTO",
                new int[] {4, 5, 6, 7, 8, 9, 10},
                initialMarking
        ));

        return invariants;
    }

    public boolean arePlaceInvariantsSatisfied(Marking marking) {
        for (PlaceInvariant invariant : placeInvariants) {
            if (!invariant.isSatisfied(marking)) {
                return false;
            }
        }

        return true;
    }

    public void verifyPlaceInvariantsOrThrow(Marking marking) {
        for (PlaceInvariant invariant : placeInvariants) {
            int actualValue = invariant.calculate(marking);

            if (actualValue != invariant.getExpectedValue()) {
                throw new IllegalStateException(
                        "Se rompio el invariante de plaza "
                                + invariant.getName()
                                + ". Plazas="
                                + Arrays.toString(invariant.getPlaces())
                                + ", esperado="
                                + invariant.getExpectedValue()
                                + ", obtenido="
                                + actualValue
                                + ", marcado="
                                + marking
                );
            }
        }
    }

    public String buildReport(Marking marking) {
        StringBuilder builder = new StringBuilder();

        for (PlaceInvariant invariant : placeInvariants) {
            int actualValue = invariant.calculate(marking);

            builder.append(invariant.getName())
                    .append(": esperado=")
                    .append(invariant.getExpectedValue())
                    .append(", obtenido=")
                    .append(actualValue)
                    .append(", ok=")
                    .append(actualValue == invariant.getExpectedValue())
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    /*
     * Se deja este metodo como utilidad, pero ya no es la verificacion principal.
     * La verificacion importante ahora son los invariantes de plaza reales.
     */
    public boolean keepsTokenTotal(Marking initialMarking, Marking finalMarking) {
        return sum(initialMarking) == sum(finalMarking);
    }

    public int sum(Marking marking) {
        int total = 0;

        for (int token : marking.toArray()) {
            total += token;
        }

        return total;
    }

    private static void validatePlaceCount(Marking marking, int expectedPlaces) {
        if (marking.size() != expectedPlaces) {
            throw new IllegalArgumentException(
                    "El marcado debe tener "
                            + expectedPlaces
                            + " plazas, pero tiene "
                            + marking.size()
            );
        }
    }

    public static class PlaceInvariant {
        private final String name;
        private final int[] places;
        private final int expectedValue;

        public PlaceInvariant(String name, int[] places, int expectedValue) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("El nombre del invariante no puede estar vacio");
            }

            if (places == null || places.length == 0) {
                throw new IllegalArgumentException("El invariante debe tener al menos una plaza");
            }

            this.name = name;
            this.places = Arrays.copyOf(places, places.length);
            this.expectedValue = expectedValue;
        }

        public static PlaceInvariant fromInitialMarking(String name, int[] places, Marking initialMarking) {
            PlaceInvariant invariant = new PlaceInvariant(name, places, 0);
            return new PlaceInvariant(name, places, invariant.calculate(initialMarking));
        }

        public boolean isSatisfied(Marking marking) {
            return calculate(marking) == expectedValue;
        }

        public int calculate(Marking marking) {
            int total = 0;

            for (int place : places) {
                if (place < 0 || place >= marking.size()) {
                    throw new IllegalArgumentException(
                            "Plaza fuera de rango en invariante "
                                    + name
                                    + ": P"
                                    + place
                    );
                }

                total += marking.getTokens(place);
            }

            return total;
        }

        public String getName() {
            return name;
        }

        public int[] getPlaces() {
            return Arrays.copyOf(places, places.length);
        }

        public int getExpectedValue() {
            return expectedValue;
        }
    }
}
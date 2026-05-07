package ar.edu.unc.concurrente.log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class TransitionLogger implements AutoCloseable {
    private static final int MEDIUM_MODE_INDEX = 0;
    private static final int SIMPLE_MODE_INDEX = 1;
    private static final int HIGH_MODE_INDEX = 2;

    private final int[] attemptsByTransition;
    private final int[] firedByTransition;
    private final PrintWriter writer;

    /*
     * Estas colas NO modifican la Red de Petri.
     * Solo sirven para reconstruir en el archivo log que recorrido hizo cada tokenId.
     */
    private final Deque<Integer> p1Tokens = new ArrayDeque<>();
    private final Deque<Integer> p3Tokens = new ArrayDeque<>();
    private final Deque<Integer> p4Tokens = new ArrayDeque<>();
    private final Deque<Integer> p5Tokens = new ArrayDeque<>();
    private final Deque<Integer> p7Tokens = new ArrayDeque<>();
    private final Deque<Integer> p8Tokens = new ArrayDeque<>();
    private final Deque<Integer> p9Tokens = new ArrayDeque<>();
    private final Deque<Integer> p10Tokens = new ArrayDeque<>();
    private final Deque<Integer> p11Tokens = new ArrayDeque<>();

    private final Map<Integer, String> modeByTokenId = new HashMap<>();

    private int nextTokenId = 1;

    public TransitionLogger(int transitionCount) {
        this(transitionCount, null, null, 0, null, null, null);
    }

    public TransitionLogger(
            int transitionCount,
            Path logPath,
            String policyName,
            int targetCompletedInvariants,
            boolean[] temporalTransitions,
            long[] alfaMillis,
            long[] betaMillis
    ) {
        if (transitionCount <= 0) {
            throw new IllegalArgumentException("Debe existir al menos una transicion");
        }

        this.attemptsByTransition = new int[transitionCount];
        this.firedByTransition = new int[transitionCount];
        this.writer = createWriter(logPath);

        if (writer != null) {
            writer.println("RUN policy=" + policyName + " target=" + targetCompletedInvariants);
            writer.println("TEMPORAL transitions=" + Arrays.toString(temporalTransitions));
            writer.println("ALFA millis=" + Arrays.toString(alfaMillis));
            writer.println("BETA millis=" + Arrays.toString(betaMillis));
            writer.println("FORMAT tokenId=<id> fired=T<transition>");
            writer.println("FORMAT tokenId=<id> INVARIANT mode=<mode> path=<path> completed=true");
            writer.println();
            writer.flush();
        }
    }

    /*
     * Este metodo lo usa WorkerThread para contar intentos y disparos.
     */
    public synchronized void record(int transition, boolean fired) {
        attemptsByTransition[transition]++;

        if (fired) {
            firedByTransition[transition]++;
        }
    }

    /*
     * Este metodo lo llama el Monitor solamente cuando una transicion
     * realmente se disparo.
     */
    public synchronized void recordFiredEvent(int transition) {
        if (writer == null) {
            return;
        }

        switch (transition) {
            case 0 -> recordT0();
            case 1 -> moveAndLog(transition, p1Tokens, p3Tokens, null);
            case 2 -> moveAndLog(transition, p3Tokens, p4Tokens, "MEDIUM");
            case 3 -> moveAndLog(transition, p4Tokens, p5Tokens, null);
            case 4 -> moveAndLog(transition, p5Tokens, p11Tokens, null);
            case 5 -> moveAndLog(transition, p3Tokens, p7Tokens, "SIMPLE");
            case 6 -> moveAndLog(transition, p7Tokens, p11Tokens, null);
            case 7 -> moveAndLog(transition, p3Tokens, p8Tokens, "HIGH");
            case 8 -> moveAndLog(transition, p8Tokens, p9Tokens, null);
            case 9 -> moveAndLog(transition, p9Tokens, p10Tokens, null);
            case 10 -> moveAndLog(transition, p10Tokens, p11Tokens, null);
            case 11 -> recordT11();
            default -> throw new IllegalArgumentException("Transicion sin trazabilidad definida: T" + transition);
        }

        writer.flush();
    }

    public synchronized int getTotalAttempts() {
        return sum(attemptsByTransition);
    }

    public synchronized int getTotalFired() {
        return sum(firedByTransition);
    }

    public synchronized int[] getFiredByTransition() {
        return Arrays.copyOf(firedByTransition, firedByTransition.length);
    }

    public synchronized void writeSummary(
            int completedInvariants,
            int[] completedByMode,
            boolean invariantOk,
            long elapsedMillis
    ) {
        if (writer == null) {
            return;
        }

        int medium = completedByMode.length > MEDIUM_MODE_INDEX ? completedByMode[MEDIUM_MODE_INDEX] : 0;
        int simple = completedByMode.length > SIMPLE_MODE_INDEX ? completedByMode[SIMPLE_MODE_INDEX] : 0;
        int high = completedByMode.length > HIGH_MODE_INDEX ? completedByMode[HIGH_MODE_INDEX] : 0;

        writer.println();
        writer.println("SUMMARY completed=" + completedInvariants
                + " medium=" + medium
                + " simple=" + simple
                + " high=" + high
                + " invariantOk=" + invariantOk
                + " timeMillis=" + elapsedMillis);

        writer.flush();
    }

    @Override
    public synchronized void close() {
        if (writer != null) {
            writer.close();
        }
    }

    private void recordT0() {
        int tokenId = nextTokenId++;
        p1Tokens.addLast(tokenId);
        writeFired(tokenId, 0);
    }

    private void recordT11() {
        int tokenId = removeFirstToken(p11Tokens, 11);
        writeFired(tokenId, 11);

        String mode = modeByTokenId.remove(tokenId);

        if (mode == null) {
            mode = "UNKNOWN";
        }

        writer.println("tokenId=" + tokenId
                + " INVARIANT mode=" + mode
                + " path=" + pathForMode(mode)
                + " completed=true");
    }

    private void moveAndLog(int transition, Deque<Integer> source, Deque<Integer> destination, String mode) {
        int tokenId = removeFirstToken(source, transition);
        destination.addLast(tokenId);

        if (mode != null) {
            modeByTokenId.put(tokenId, mode);
        }

        writeFired(tokenId, transition);
    }

    private int removeFirstToken(Deque<Integer> source, int transition) {
        Integer tokenId = source.pollFirst();

        if (tokenId == null) {
            throw new IllegalStateException("No hay tokenId disponible para registrar T" + transition);
        }

        return tokenId;
    }

    private void writeFired(int tokenId, int transition) {
        writer.println("tokenId=" + tokenId + " fired=T" + transition);
    }

    private String pathForMode(String mode) {
        return switch (mode) {
            case "MEDIUM" -> "T0,T1,T2,T3,T4,T11";
            case "SIMPLE" -> "T0,T1,T5,T6,T11";
            case "HIGH" -> "T0,T1,T7,T8,T9,T10,T11";
            default -> "UNKNOWN";
        };
    }

    private PrintWriter createWriter(Path logPath) {
        if (logPath == null) {
            return null;
        }

        try {
            Path parent = logPath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            BufferedWriter bufferedWriter = Files.newBufferedWriter(logPath);
            return new PrintWriter(bufferedWriter);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear el archivo de log: " + logPath, exception);
        }
    }

    private static int sum(int[] values) {
        int total = 0;

        for (int value : values) {
            total += value;
        }

        return total;
    }
}
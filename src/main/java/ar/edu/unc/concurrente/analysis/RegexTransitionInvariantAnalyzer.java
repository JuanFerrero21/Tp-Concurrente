package ar.edu.unc.concurrente.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RegexTransitionInvariantAnalyzer {
    private static final Pattern FIRED_LINE_PATTERN =
            Pattern.compile("^tokenId=(\\d+) fired=(T\\d+)$");

    private static final Pattern COMPLETED_INVARIANT_PATTERN =
            Pattern.compile("^tokenId=(\\d+) INVARIANT mode=([A-Z]+) path=([T0-9,]+) completed=true$");

    private static final Pattern SUMMARY_PATTERN =
            Pattern.compile("^SUMMARY completed=(\\d+) medium=(\\d+) simple=(\\d+) high=(\\d+) invariantOk=(true|false) timeMillis=(\\d+)$");

    private static final Pattern SIMPLE_PATTERN =
            Pattern.compile("^T0,T1,T5,T6,T11$");

    private static final Pattern MEDIUM_PATTERN =
            Pattern.compile("^T0,T1,T2,T3,T4,T11$");

    private static final Pattern HIGH_PATTERN =
            Pattern.compile("^T0,T1,T7,T8,T9,T10,T11$");

    public RegexAnalysisResult analyze(Path logPath) {
        List<String> lines = readLines(logPath);

        Map<Integer, List<String>> transitionsByTokenId = new LinkedHashMap<>();
        Map<Integer, CompletedInvariantLine> completedByTokenId = new LinkedHashMap<>();

        SummaryLine summaryLine = null;

        for (String line : lines) {
            java.util.regex.Matcher firedMatcher = FIRED_LINE_PATTERN.matcher(line);

            if (firedMatcher.matches()) {
                int tokenId = Integer.parseInt(firedMatcher.group(1));
                String transition = firedMatcher.group(2);

                transitionsByTokenId
                        .computeIfAbsent(tokenId, ignored -> new ArrayList<>())
                        .add(transition);

                continue;
            }

            java.util.regex.Matcher completedMatcher = COMPLETED_INVARIANT_PATTERN.matcher(line);

            if (completedMatcher.matches()) {
                int tokenId = Integer.parseInt(completedMatcher.group(1));
                String mode = completedMatcher.group(2);
                String path = completedMatcher.group(3);

                completedByTokenId.put(tokenId, new CompletedInvariantLine(mode, path));
                continue;
            }

            java.util.regex.Matcher summaryMatcher = SUMMARY_PATTERN.matcher(line);

            if (summaryMatcher.matches()) {
                summaryLine = new SummaryLine(
                        Integer.parseInt(summaryMatcher.group(1)),
                        Integer.parseInt(summaryMatcher.group(2)),
                        Integer.parseInt(summaryMatcher.group(3)),
                        Integer.parseInt(summaryMatcher.group(4)),
                        Boolean.parseBoolean(summaryMatcher.group(5)),
                        Long.parseLong(summaryMatcher.group(6))
                );
            }
        }

        return buildResult(transitionsByTokenId, completedByTokenId, summaryLine);
    }

    private RegexAnalysisResult buildResult(
            Map<Integer, List<String>> transitionsByTokenId,
            Map<Integer, CompletedInvariantLine> completedByTokenId,
            SummaryLine summaryLine
    ) {
        int simpleCount = 0;
        int mediumCount = 0;
        int highCount = 0;
        int invalidCount = 0;
        int incompleteCount = 0;

        List<String> errors = new ArrayList<>();

        for (Map.Entry<Integer, List<String>> entry : transitionsByTokenId.entrySet()) {
            int tokenId = entry.getKey();
            String reconstructedPath = String.join(",", entry.getValue());

            CompletedInvariantLine completedLine = completedByTokenId.get(tokenId);

            if (completedLine == null) {
                incompleteCount++;
                errors.add("tokenId=" + tokenId + " no completo invariante. path=" + reconstructedPath);
                continue;
            }

            Mode modeByRegex = classifyByRegex(reconstructedPath);

            if (modeByRegex == Mode.INVALID) {
                invalidCount++;
                errors.add("tokenId=" + tokenId + " tiene path invalido por regex. path=" + reconstructedPath);
                continue;
            }

            if (!completedLine.path.equals(reconstructedPath)) {
                invalidCount++;
                errors.add("tokenId=" + tokenId
                        + " no coincide entre path reconstruido y path registrado. reconstruido="
                        + reconstructedPath
                        + ", registrado="
                        + completedLine.path);
                continue;
            }

            if (!completedLine.mode.equals(modeByRegex.name())) {
                invalidCount++;
                errors.add("tokenId=" + tokenId
                        + " no coincide entre modo regex y modo registrado. regex="
                        + modeByRegex.name()
                        + ", registrado="
                        + completedLine.mode);
                continue;
            }

            switch (modeByRegex) {
                case SIMPLE -> simpleCount++;
                case MEDIUM -> mediumCount++;
                case HIGH -> highCount++;
                default -> invalidCount++;
            }
        }

        for (Integer tokenId : completedByTokenId.keySet()) {
            if (!transitionsByTokenId.containsKey(tokenId)) {
                invalidCount++;
                errors.add("tokenId=" + tokenId + " tiene linea INVARIANT pero no tiene disparos fired asociados");
            }
        }

        int analyzedTokens = simpleCount + mediumCount + highCount;
        boolean summaryOk = validateSummary(
                summaryLine,
                analyzedTokens,
                mediumCount,
                simpleCount,
                highCount,
                invalidCount,
                incompleteCount,
                errors
        );

        boolean regexOk = invalidCount == 0
                && incompleteCount == 0
                && analyzedTokens == completedByTokenId.size()
                && summaryOk;

        return new RegexAnalysisResult(
                analyzedTokens,
                mediumCount,
                simpleCount,
                highCount,
                invalidCount,
                incompleteCount,
                regexOk,
                summaryOk,
                summaryLine,
                errors
        );
    }

    private boolean validateSummary(
            SummaryLine summaryLine,
            int analyzedTokens,
            int mediumCount,
            int simpleCount,
            int highCount,
            int invalidCount,
            int incompleteCount,
            List<String> errors
    ) {
        if (summaryLine == null) {
            errors.add("No se encontro la linea SUMMARY en el log");
            return false;
        }

        boolean ok = true;

        if (summaryLine.completed != analyzedTokens) {
            ok = false;
            errors.add("SUMMARY completed="
                    + summaryLine.completed
                    + " pero regex analizo "
                    + analyzedTokens
                    + " tokens completos");
        }

        if (summaryLine.medium != mediumCount) {
            ok = false;
            errors.add("SUMMARY medium="
                    + summaryLine.medium
                    + " pero regex conto medium="
                    + mediumCount);
        }

        if (summaryLine.simple != simpleCount) {
            ok = false;
            errors.add("SUMMARY simple="
                    + summaryLine.simple
                    + " pero regex conto simple="
                    + simpleCount);
        }

        if (summaryLine.high != highCount) {
            ok = false;
            errors.add("SUMMARY high="
                    + summaryLine.high
                    + " pero regex conto high="
                    + highCount);
        }

        if (!summaryLine.invariantOk) {
            ok = false;
            errors.add("SUMMARY invariantOk=false");
        }

        if (invalidCount > 0 || incompleteCount > 0) {
            ok = false;
        }

        return ok;
    }

    private Mode classifyByRegex(String path) {
        if (SIMPLE_PATTERN.matcher(path).matches()) {
            return Mode.SIMPLE;
        }

        if (MEDIUM_PATTERN.matcher(path).matches()) {
            return Mode.MEDIUM;
        }

        if (HIGH_PATTERN.matcher(path).matches()) {
            return Mode.HIGH;
        }

        return Mode.INVALID;
    }

    private List<String> readLines(Path logPath) {
        try {
            return Files.readAllLines(logPath);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el archivo de log: " + logPath, exception);
        }
    }

    private enum Mode {
        SIMPLE,
        MEDIUM,
        HIGH,
        INVALID
    }

    private static class CompletedInvariantLine {
        private final String mode;
        private final String path;

        private CompletedInvariantLine(String mode, String path) {
            this.mode = mode;
            this.path = path;
        }
    }

    private static class SummaryLine {
        private final int completed;
        private final int medium;
        private final int simple;
        private final int high;
        private final boolean invariantOk;
        private final long timeMillis;

        private SummaryLine(
                int completed,
                int medium,
                int simple,
                int high,
                boolean invariantOk,
                long timeMillis
        ) {
            this.completed = completed;
            this.medium = medium;
            this.simple = simple;
            this.high = high;
            this.invariantOk = invariantOk;
            this.timeMillis = timeMillis;
        }
    }

    public static class RegexAnalysisResult {
        private final int analyzedTokens;
        private final int mediumCount;
        private final int simpleCount;
        private final int highCount;
        private final int invalidCount;
        private final int incompleteCount;
        private final boolean regexOk;
        private final boolean summaryOk;
        private final SummaryLine summaryLine;
        private final List<String> errors;

        private RegexAnalysisResult(
                int analyzedTokens,
                int mediumCount,
                int simpleCount,
                int highCount,
                int invalidCount,
                int incompleteCount,
                boolean regexOk,
                boolean summaryOk,
                SummaryLine summaryLine,
                List<String> errors
        ) {
            this.analyzedTokens = analyzedTokens;
            this.mediumCount = mediumCount;
            this.simpleCount = simpleCount;
            this.highCount = highCount;
            this.invalidCount = invalidCount;
            this.incompleteCount = incompleteCount;
            this.regexOk = regexOk;
            this.summaryOk = summaryOk;
            this.summaryLine = summaryLine;
            this.errors = new ArrayList<>(errors);
        }

        public boolean isRegexOk() {
            return regexOk;
        }

        public int getAnalyzedTokens() {
            return analyzedTokens;
        }

        public int getMediumCount() {
            return mediumCount;
        }

        public int getSimpleCount() {
            return simpleCount;
        }

        public int getHighCount() {
            return highCount;
        }

        public int getInvalidCount() {
            return invalidCount;
        }

        public int getIncompleteCount() {
            return incompleteCount;
        }

        public String buildReport() {
            StringBuilder builder = new StringBuilder();

            builder.append("Analisis regex de invariantes de transicion:")
                    .append(System.lineSeparator());

            builder.append("Tokens analizados: ")
                    .append(analyzedTokens)
                    .append(System.lineSeparator());

            builder.append("Medium: ")
                    .append(mediumCount)
                    .append(System.lineSeparator());

            builder.append("Simple: ")
                    .append(simpleCount)
                    .append(System.lineSeparator());

            builder.append("High: ")
                    .append(highCount)
                    .append(System.lineSeparator());

            builder.append("Invalidos: ")
                    .append(invalidCount)
                    .append(System.lineSeparator());

            builder.append("Incompletos: ")
                    .append(incompleteCount)
                    .append(System.lineSeparator());

            builder.append("Summary OK: ")
                    .append(summaryOk)
                    .append(System.lineSeparator());

            if (summaryLine != null) {
                builder.append("Tiempo segun SUMMARY: ")
                        .append(summaryLine.timeMillis)
                        .append(" ms")
                        .append(System.lineSeparator());
            }

            builder.append("Regex OK: ")
                    .append(regexOk)
                    .append(System.lineSeparator());

            if (!errors.isEmpty()) {
                builder.append("Errores detectados:")
                        .append(System.lineSeparator());

                int limit = Math.min(errors.size(), 20);

                for (int i = 0; i < limit; i++) {
                    builder.append("- ")
                            .append(errors.get(i))
                            .append(System.lineSeparator());
                }

                if (errors.size() > limit) {
                    builder.append("- ... ")
                            .append(errors.size() - limit)
                            .append(" errores mas")
                            .append(System.lineSeparator());
                }
            }

            return builder.toString();
        }
    }
}
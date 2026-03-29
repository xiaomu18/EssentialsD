package cn.lunadeer.essentialsd.utils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MuteDuration {
    private static final Pattern SEGMENT_PATTERN = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);

    public static ParseResult parse(String input) {
        if (input == null) {
            return ParseResult.invalid();
        }

        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return ParseResult.invalid();
        }
        if (normalized.equals("perm") || normalized.equals("permanent") || normalized.equals("forever")
                || normalized.equals("永久") || normalized.equals("永久禁言") || normalized.equals("0")) {
            return ParseResult.permanent();
        }
        if (normalized.chars().allMatch(Character::isDigit)) {
            long seconds = Long.parseLong(normalized);
            return seconds <= 0 ? ParseResult.permanent() : new ParseResult(true, seconds * 1000L);
        }

        Matcher matcher = SEGMENT_PATTERN.matcher(normalized);
        int lastEnd = 0;
        long totalMillis = 0L;
        boolean matched = false;
        while (matcher.find()) {
            if (matcher.start() != lastEnd) {
                return ParseResult.invalid();
            }
            matched = true;
            lastEnd = matcher.end();

            long value = Long.parseLong(matcher.group(1));
            char unit = Character.toLowerCase(matcher.group(2).charAt(0));
            totalMillis += switch (unit) {
                case 's' -> value * 1000L;
                case 'm' -> value * 60_000L;
                case 'h' -> value * 3_600_000L;
                case 'd' -> value * 86_400_000L;
                case 'w' -> value * 604_800_000L;
                default -> 0L;
            };
        }

        if (!matched || lastEnd != normalized.length()) {
            return ParseResult.invalid();
        }
        return totalMillis <= 0 ? ParseResult.permanent() : new ParseResult(true, totalMillis);
    }

    public static String formatRemaining(Long expiresAtMillis) {
        if (expiresAtMillis == null) {
            return "永久";
        }
        return formatDuration(Math.max(0L, expiresAtMillis - System.currentTimeMillis()));
    }

    public static String formatDuration(Long durationMillis) {
        if (durationMillis == null) {
            return "永久";
        }

        long totalSeconds = Math.max(1L, durationMillis / 1000L);
        long weeks = totalSeconds / 604800L;
        totalSeconds %= 604800L;
        long days = totalSeconds / 86400L;
        totalSeconds %= 86400L;
        long hours = totalSeconds / 3600L;
        totalSeconds %= 3600L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        StringBuilder builder = new StringBuilder();
        appendUnit(builder, weeks, "周");
        appendUnit(builder, days, "天");
        appendUnit(builder, hours, "小时");
        appendUnit(builder, minutes, "分钟");
        if (builder.length() == 0 || seconds > 0) {
            appendUnit(builder, seconds, "秒");
        }
        return builder.toString();
    }

    private static void appendUnit(StringBuilder builder, long value, String unit) {
        if (value > 0) {
            builder.append(value).append(unit);
        }
    }

    public static class ParseResult {
        private final boolean valid;
        private final Long durationMillis;

        public ParseResult(boolean valid, Long durationMillis) {
            this.valid = valid;
            this.durationMillis = durationMillis;
        }

        public static ParseResult invalid() {
            return new ParseResult(false, null);
        }

        public static ParseResult permanent() {
            return new ParseResult(true, null);
        }

        public boolean isValid() {
            return valid;
        }

        public Long getDurationMillis() {
            return durationMillis;
        }
    }
}

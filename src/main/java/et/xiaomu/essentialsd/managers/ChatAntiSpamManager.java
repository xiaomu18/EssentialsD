package et.xiaomu.essentialsd.managers;

import et.xiaomu.essentialsd.EssentialsD;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ChatAntiSpamManager {
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("(?i)§x(§[0-9a-f]){6}");
    private static final Pattern SHARP_HEX_COLOR_PATTERN = Pattern.compile("(?i)§#[0-9a-f]{6}");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)§[0-9a-fk-or]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final Map<String, ChatState> states = new ConcurrentHashMap<>();

    public CheckResult checkAttempt(Player player, String normalizedMessage, long currentTime) {
        String identityKey = resolveIdentityKey(player);
        ChatState state = states.computeIfAbsent(identityKey, ignored -> new ChatState());
        synchronized (state) {
            if (EssentialsD.config.chat_rate_limit_value > 0 && EssentialsD.config.chat_rate_limit_duration_ms > 0) {
                pruneAttempts(state.attemptTimestamps, currentTime, EssentialsD.config.chat_rate_limit_duration_ms);
                if (state.attemptTimestamps.size() >= EssentialsD.config.chat_rate_limit_value) {
                    return new CheckResult(identityKey, CheckType.RATE_LIMIT);
                }
            }

            if (EssentialsD.config.chat_anti_spam_cooldown_ms > 0
                    && state.lastChatTime > 0
                    && currentTime - state.lastChatTime < EssentialsD.config.chat_anti_spam_cooldown_ms) {
                state.lastChatTime = currentTime;
                return new CheckResult(identityKey, CheckType.COOLDOWN);
            }
            state.lastChatTime = currentTime;

            if (EssentialsD.config.chat_repeat_interceptor_enable) {
                pruneSamples(state.samples, currentTime, EssentialsD.config.chat_repeat_interceptor_sample_expiration_ms);
                for (MessageSample sample : state.samples) {
                    double similarity = similarity(normalizedMessage, sample.normalizedMessage());
                    if (similarity >= EssentialsD.config.chat_repeat_interceptor_similarity_threshold) {
                        return new CheckResult(identityKey, CheckType.REPEAT);
                    }
                }
            }

            return new CheckResult(identityKey, CheckType.PASS);
        }
    }

    public void recordRateLimitAttempt(String identityKey, long currentTime) {
        if (EssentialsD.config.chat_rate_limit_value <= 0 || EssentialsD.config.chat_rate_limit_duration_ms <= 0) {
            return;
        }
        ChatState state = states.computeIfAbsent(identityKey, ignored -> new ChatState());
        synchronized (state) {
            pruneAttempts(state.attemptTimestamps, currentTime, EssentialsD.config.chat_rate_limit_duration_ms);
            state.attemptTimestamps.addLast(currentTime);
        }
    }

    public void recordVisibleMessage(String identityKey, String normalizedMessage, long currentTime) {
        ChatState state = states.computeIfAbsent(identityKey, ignored -> new ChatState());
        synchronized (state) {
            if (EssentialsD.config.chat_repeat_interceptor_enable) {
                pruneSamples(state.samples, currentTime, EssentialsD.config.chat_repeat_interceptor_sample_expiration_ms);
                state.samples.addLast(new MessageSample(normalizedMessage, currentTime));
                int maxSamples = Math.max(1, EssentialsD.config.chat_repeat_interceptor_sample_hits);
                while (state.samples.size() > maxSamples) {
                    state.samples.removeFirst();
                }
            }
        }
    }

    public void clearPlayer(Player player) {
        String identityKey = resolveIdentityKey(player);
        if (!identityKey.startsWith("ip:")) {
            states.remove(identityKey);
            return;
        }

        String playerIp = MuteManager.normalizeIp(MuteManager.getPlayerIp(player));
        if (playerIp == null || playerIp.isBlank()) {
            states.remove(identityKey);
            return;
        }

        boolean hasOtherOnlinePlayerWithSameIp = Bukkit.getOnlinePlayers().stream()
                .filter(online -> !online.getUniqueId().equals(player.getUniqueId()))
                .map(MuteManager::getPlayerIp)
                .map(MuteManager::normalizeIp)
                .anyMatch(playerIp::equals);
        if (!hasOtherOnlinePlayerWithSameIp) {
            states.remove(identityKey);
        }
    }

    public void reset() {
        states.clear();
    }

    public String resolveIdentityKey(Player player) {
        if (!EssentialsD.config.chat_anti_spam_base_on_ip) {
            return player.getUniqueId().toString();
        }
        String ip = MuteManager.normalizeIp(MuteManager.getPlayerIp(player));
        if (ip == null || ip.isBlank()) {
            return player.getUniqueId().toString();
        }
        return "ip:" + ip;
    }

    public static String normalizeForDetection(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        String stripped = HEX_COLOR_PATTERN.matcher(message).replaceAll("");
        stripped = SHARP_HEX_COLOR_PATTERN.matcher(stripped).replaceAll("");
        stripped = LEGACY_COLOR_PATTERN.matcher(stripped).replaceAll("");
        stripped = stripped.toLowerCase(Locale.ROOT).trim();
        return WHITESPACE_PATTERN.matcher(stripped).replaceAll(" ");
    }

    public static int getVisibleLength(String message) {
        return removeColorSymbol(message).length();
    }

    public static String removeColorSymbol(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        String stripped = HEX_COLOR_PATTERN.matcher(message).replaceAll("");
        stripped = SHARP_HEX_COLOR_PATTERN.matcher(stripped).replaceAll("");
        return LEGACY_COLOR_PATTERN.matcher(stripped).replaceAll("");
    }

    private void pruneAttempts(Deque<Long> attempts, long currentTime, long durationMs) {
        long minTime = currentTime - durationMs;
        while (!attempts.isEmpty() && attempts.peekFirst() < minTime) {
            attempts.removeFirst();
        }
    }

    private void pruneSamples(Deque<MessageSample> samples, long currentTime, int expirationMs) {
        if (expirationMs <= 0) {
            samples.clear();
            return;
        }
        long minTime = currentTime - expirationMs;
        while (!samples.isEmpty() && samples.peekFirst().timestamp() < minTime) {
            samples.removeFirst();
        }
    }

    private static double similarity(String left, String right) {
        if (left.equals(right)) {
            return 1.0D;
        }
        int maxLength = Math.max(left.length(), right.length());
        if (maxLength == 0) {
            return 1.0D;
        }
        int distance = levenshtein(left, right);
        return 1.0D - ((double) distance / (double) maxLength);
    }

    private static int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];

        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            char leftChar = left.charAt(i - 1);
            for (int j = 1; j <= right.length(); j++) {
                int cost = leftChar == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[right.length()];
    }

    public enum CheckType {
        PASS,
        RATE_LIMIT,
        COOLDOWN,
        REPEAT
    }

    public record CheckResult(String identityKey, CheckType type) {
    }

    private record MessageSample(String normalizedMessage, long timestamp) {
    }

    private static final class ChatState {
        private long lastChatTime;
        private final Deque<Long> attemptTimestamps = new ArrayDeque<>();
        private final Deque<MessageSample> samples = new ArrayDeque<>();
    }
}

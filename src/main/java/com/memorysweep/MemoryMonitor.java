package com.memorysweep;

import org.apache.logging.log4j.Logger;

import java.util.Locale;

/**
 * 内存监控与清理的核心逻辑,与具体触发方式(定时 tick、按键)解耦。
 *
 * <p>支持的触发方式:</p>
 * <ul>
 *   <li>定时:每隔 {@code intervalMinutes} 分钟(默认 15 分钟)清理一次</li>
 *   <li>使用率:堆内存占用达到 {@code memoryUsageThresholdPercent}(默认 80%)时清理,
 *       但距离上一次清理(无论是何种触发方式)不足 {@code usageCheckCooldownSeconds}
 *       (默认 120 秒,即 2 分钟)时不会重复触发。</li>
 *   <li>手动:玩家按下快捷键(见 {@code KeybindListener}),仅在本地/单人环境下生效,
 *       因为这只是一个客户端按键,清理的是当前 JVM 的内存 —— 单人游戏时这个 JVM 同时
 *       跑着客户端和内嵌服务端,联机连接他人服务器时则只会清理你自己客户端这一侧。</li>
 * </ul>
 */
public final class MemoryMonitor {

    public enum CleanupReason {
        MANUAL("手动清理(快捷键)"),
        SCHEDULED("定时自动清理"),
        USAGE_TRIGGERED("内存使用率触发清理");

        private final String label;

        CleanupReason(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public static final class CleanupResult {
        private final long beforeUsedBytes;
        private final long afterUsedBytes;
        private final long maxBytes;
        private final long durationMillis;
        private final CleanupReason reason;

        CleanupResult(long beforeUsedBytes, long afterUsedBytes, long maxBytes, long durationMillis, CleanupReason reason) {
            this.beforeUsedBytes = beforeUsedBytes;
            this.afterUsedBytes = afterUsedBytes;
            this.maxBytes = maxBytes;
            this.durationMillis = durationMillis;
            this.reason = reason;
        }

        public long freedBytes() {
            return Math.max(0L, beforeUsedBytes - afterUsedBytes);
        }

        public double beforePercent() {
            return maxBytes <= 0 ? 0.0 : (beforeUsedBytes * 100.0) / maxBytes;
        }

        public double afterPercent() {
            return maxBytes <= 0 ? 0.0 : (afterUsedBytes * 100.0) / maxBytes;
        }

        public String toLogText() {
            return String.format(Locale.ROOT,
                    "%s完成 | 清理前 %d MB (%.1f%%) -> 清理后 %d MB (%.1f%%) | 释放约 %d MB | 耗时 %d ms",
                    reason.label(), toMb(beforeUsedBytes), beforePercent(), toMb(afterUsedBytes), afterPercent(),
                    toMb(freedBytes()), durationMillis);
        }

        public String toChatText() {
            return String.format(Locale.ROOT, "[内存清理] %s | %d MB -> %d MB | 释放约 %d MB | 耗时 %d ms",
                    reason.label(), toMb(beforeUsedBytes), toMb(afterUsedBytes), toMb(freedBytes()), durationMillis);
        }

        private static long toMb(long bytes) {
            return bytes / (1024L * 1024L);
        }
    }

    private final Logger logger;
    private final boolean logToConsole;

    private long lastCleanupTimeMillis = 0L;

    public MemoryMonitor(Logger logger, boolean logToConsole) {
        this.logger = logger;
        this.logToConsole = logToConsole;
    }

    /** 当前堆内存使用率(0-100)。 */
    public double currentUsagePercent() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        if (max <= 0) {
            return 0.0;
        }
        long used = runtime.totalMemory() - runtime.freeMemory();
        return (used * 100.0) / max;
    }

    /** 距离上一次清理(无论何种触发方式)经过的毫秒数;尚未清理过则返回一个很大的数。 */
    public long millisSinceLastCleanup() {
        if (lastCleanupTimeMillis == 0L) {
            return Long.MAX_VALUE / 2;
        }
        return System.currentTimeMillis() - lastCleanupTimeMillis;
    }

    /**
     * 立即执行一次内存清理(调用 {@link System#gc()}),并根据配置输出日志。
     * 该方法本身不做冷却判断 —— 冷却只用于限制"使用率自动触发",调用方需要自行决定何时调用本方法。
     */
    public CleanupResult performCleanup(CleanupReason reason) {
        Runtime runtime = Runtime.getRuntime();
        long beforeUsed = runtime.totalMemory() - runtime.freeMemory();

        long startNanos = System.nanoTime();
        System.gc();
        long durationMillis = (System.nanoTime() - startNanos) / 1_000_000L;

        long afterUsed = runtime.totalMemory() - runtime.freeMemory();

        lastCleanupTimeMillis = System.currentTimeMillis();

        CleanupResult result = new CleanupResult(beforeUsed, afterUsed, runtime.maxMemory(), durationMillis, reason);

        if (logToConsole) {
            logger.info("[MemorySweep] " + result.toLogText());
        }

        return result;
    }
}

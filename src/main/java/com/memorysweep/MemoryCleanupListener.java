package com.memorysweep;

import com.memorysweep.config.MemorySweepConfig;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.event.tick.GameTickEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.EntrypointManager;

import java.lang.invoke.MethodHandles;

/**
 * 服务端(专用服务器 / 单人内嵌服务端)tick 驱动的自动清理逻辑。
 * <p>
 * 注册在 {@code stationapi:event_bus_server} 上,只在服务端被扫描/触发。
 * {@link GameTickEvent.End} 本身不携带 MinecraftServer 引用,但清理动作
 * (调用 {@link System#gc()} 并读取 {@link Runtime})本身就是纯 JVM 级别的操作,
 * 不需要 MinecraftServer 实例也能正确完成。
 */
public final class MemoryCleanupListener {

    static {
        EntrypointManager.registerLookup(MethodHandles.lookup());
    }

    /** 20 tick 才检查一次(服务端满速运行时约等于 1 秒),避免每 tick 都做时间/内存运算。 */
    private static int tickCounter = 0;

    private static long lastScheduledCleanupTimeMillis = 0L;
    private static long lastUsageCheckTimeMillis = 0L;

    private MemoryCleanupListener() {
    }

    @EventListener
    private static void onGameTickEnd(GameTickEvent.End event) {
        MemorySweepConfig config = MemorySweepMod.getConfig();
        MemoryMonitor monitor = MemorySweepMod.getMemoryMonitor();
        if (config == null || monitor == null) {
            return; // 初始化尚未完成(理论上不会发生,保险起见判空)
        }

        tickCounter++;
        if (tickCounter < 20) {
            return;
        }
        tickCounter = 0;

        long now = System.currentTimeMillis();

        if (lastScheduledCleanupTimeMillis == 0L) {
            lastScheduledCleanupTimeMillis = now; // 第一次 tick,从此刻开始计时定时清理
        }

        if (config.autoCleanupEnabled) {
            long intervalMillis = config.intervalMinutes * 60_000L;
            if (now - lastScheduledCleanupTimeMillis >= intervalMillis) {
                lastScheduledCleanupTimeMillis = now;
                monitor.performCleanup(MemoryMonitor.CleanupReason.SCHEDULED);
            }
        }

        if (config.usageBasedCleanupEnabled) {
            long usageCheckIntervalMillis = config.usageCheckIntervalSeconds * 1000L;
            if (now - lastUsageCheckTimeMillis >= usageCheckIntervalMillis) {
                lastUsageCheckTimeMillis = now;
                maybeTriggerUsageCleanup(config, monitor);
            }
        }
    }

    private static void maybeTriggerUsageCleanup(MemorySweepConfig config, MemoryMonitor monitor) {
        if (monitor.currentUsagePercent() < config.memoryUsageThresholdPercent) {
            return;
        }

        long cooldownMillis = config.usageCheckCooldownSeconds * 1000L;
        if (monitor.millisSinceLastCleanup() < cooldownMillis) {
            return; // 冷却中:同一冷却周期内(默认 2 分钟)只允许触发一次
        }

        monitor.performCleanup(MemoryMonitor.CleanupReason.USAGE_TRIGGERED);
    }
}

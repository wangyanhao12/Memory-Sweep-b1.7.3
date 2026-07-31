package com.memorysweep;

import com.memorysweep.config.MemorySweepConfig;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.event.mod.InitEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.EntrypointManager;
import net.modificationstation.stationapi.api.util.Namespace;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;

/**
 * MemorySweep 的核心静态持有者。
 * <p>
 * 在 {@link InitEvent}(StationAPI 通用初始化事件,客户端/服务端都会触发)时加载配置、
 * 创建共享的 {@link MemoryMonitor} 实例,供 {@link MemoryCleanupListener}(服务端 tick 触发)
 * 与 {@link KeybindListener}(客户端按键触发)共同使用。
 */
public final class MemorySweepMod {

    static {
        EntrypointManager.registerLookup(MethodHandles.lookup());
    }

    @SuppressWarnings("UnstableApiUsage")
    public static final Namespace NAMESPACE = Namespace.resolve();

    public static final Logger LOGGER = NAMESPACE.getLogger();

    private static MemorySweepConfig config;
    private static MemoryMonitor memoryMonitor;

    private MemorySweepMod() {
    }

    @EventListener
    private static void onInit(InitEvent event) {
        config = MemorySweepConfig.load(LOGGER);
        memoryMonitor = new MemoryMonitor(LOGGER, config.logToConsole);

        LOGGER.info("[MemorySweep] 模组已加载 | 定时清理: " + describeSchedule()
                + " | 使用率触发清理: " + describeUsageTrigger()
                + " | 手动触发: 默认快捷键 H(可在 控制选项 里重新绑定,仅本地/单人生效)");
    }

    private static String describeSchedule() {
        return config.autoCleanupEnabled ? ("每 " + config.intervalMinutes + " 分钟一次") : "已禁用";
    }

    private static String describeUsageTrigger() {
        return config.usageBasedCleanupEnabled
                ? ("已启用(阈值 " + config.memoryUsageThresholdPercent + "%,冷却 " + config.usageCheckCooldownSeconds + " 秒)")
                : "已禁用";
    }

    public static MemorySweepConfig getConfig() {
        return config;
    }

    public static MemoryMonitor getMemoryMonitor() {
        return memoryMonitor;
    }
}

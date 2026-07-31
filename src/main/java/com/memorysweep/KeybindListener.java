package com.memorysweep;

import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.modificationstation.stationapi.api.client.event.keyboard.KeyStateChangedEvent;
import net.modificationstation.stationapi.api.client.event.option.KeyBindingRegisterEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.EntrypointManager;
import org.lwjgl.input.Keyboard;

import java.lang.invoke.MethodHandles;

/**
 * 客户端快捷键:手动触发一次内存清理。
 * <p>
 * 只注册在 {@code stationapi:event_bus_client} 上,只在客户端被扫描/触发。
 * 清理的是"当前 JVM"的内存 —— 单人游戏时这个 JVM 同时跑着客户端和内嵌服务端,
 * 所以效果等同于清理整个游戏进程;如果只是作为客户端连接到别人的服务器,
 * 则只会清理你本地客户端这一侧的内存,不会影响远程服务器。
 * <p>
 * 默认按键是 J,可以在游戏内 “控制选项” 里重新绑定。
 */
public final class KeybindListener {

    static {
        EntrypointManager.registerLookup(MethodHandles.lookup());
    }

    private static KeyBinding cleanupKeyBinding;

    private KeybindListener() {
    }

    @EventListener
    private static void registerKeybind(KeyBindingRegisterEvent event) {
        cleanupKeyBinding = new KeyBinding("key.memorysweep.cleanup", Keyboard.KEY_J);
        event.keyBindings.add(cleanupKeyBinding);
    }

    @EventListener
    private static void onKeyStateChanged(KeyStateChangedEvent event) {
        if (cleanupKeyBinding == null || event.environment != KeyStateChangedEvent.Environment.IN_GAME) {
            return; // 在菜单/GUI 里按键时不触发,避免和文本输入等冲突
        }

        if (!Keyboard.getEventKeyState()) {
            return; // 只在按下(而不是松开)的瞬间触发一次
        }

        if (!Keyboard.isKeyDown(cleanupKeyBinding.code)) {
            return; // 这次状态变化不是我们绑定的那个键
        }

        MemoryMonitor monitor = MemorySweepMod.getMemoryMonitor();
        if (monitor == null) {
            return;
        }

        MemoryMonitor.CleanupResult result = monitor.performCleanup(MemoryMonitor.CleanupReason.MANUAL);

        Minecraft minecraft = Minecraft.INSTANCE;
        if (minecraft != null && minecraft.inGameHud != null) {
            // addChatMessage 只会在本地聊天栏显示,不会发送给服务器或其他玩家
            minecraft.inGameHud.addChatMessage(result.toChatText());
        }
    }
}

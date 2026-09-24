package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.AasGranate;
import com.danilfb123.aasgranate.HitboxDebugPacket;
import com.danilfb123.aasgranate.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Следит за состоянием F3+B (отображение хитбоксов) у игрока и сообщает об изменении
 * серверу, чтобы граната знала, нужно ли показывать этому игроку отладочные лучи осколков.
 */
@Mod.EventBusSubscriber(modid = AasGranate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientHitboxDebugTracker {

    private static boolean lastSent = false;
    private static int resyncTimer = 0;
    private static final int RESYNC_INTERVAL_TICKS = 100; // раз в 5 сек, на случай потери пакета

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean current = mc.getEntityRenderDispatcher().shouldRenderHitBoxes();

        resyncTimer++;
        if (current != lastSent || resyncTimer >= RESYNC_INTERVAL_TICKS) {
            ModNetworking.CHANNEL.sendToServer(new HitboxDebugPacket(current));
            lastSent = current;
            resyncTimer = 0;
        }
    }
    // Метод для безопасного получения времени мира на клиенте
    public static long getClientGameTime() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return mc.level.getGameTime();
        }
        return 0L;
    }
}

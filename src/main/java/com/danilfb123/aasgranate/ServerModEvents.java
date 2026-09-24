package com.danilfb123.aasgranate;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Убирает за игроком в статических map'ах мода при выходе из мира, чтобы не копить мусор.
 */
@Mod.EventBusSubscriber(modid = AasGranate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerModEvents {

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        ModNetworking.removePlayer(player.getUUID());
        // Состояние анимации гранаты (чека/бросок) теперь хранится в NBT самого
        // предмета, а не в статической Map<UUID, Long> — чистить за игроком нечего.
    }
}
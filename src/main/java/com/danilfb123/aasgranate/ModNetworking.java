package com.danilfb123.aasgranate;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сетевой канал мода. Сейчас используется только для того, чтобы клиент сообщал серверу,
 * включён ли у игрока показ хитбоксов (F3+B) — это нужно, чтобы граната знала, кому
 * показывать отладочные лучи осколков при взрыве.
 */
public class ModNetworking {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AasGranate.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static final Set<UUID> HITBOX_DEBUG_PLAYERS = ConcurrentHashMap.newKeySet();

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++,
                HitboxDebugPacket.class,
                HitboxDebugPacket::encode,
                HitboxDebugPacket::decode,
                HitboxDebugPacket::handle);
    }

    public static void setHitboxDebug(UUID playerId, boolean enabled) {
        if (enabled) HITBOX_DEBUG_PLAYERS.add(playerId);
        else HITBOX_DEBUG_PLAYERS.remove(playerId);
    }

    public static boolean hasHitboxDebug(UUID playerId) {
        return HITBOX_DEBUG_PLAYERS.contains(playerId);
    }

    public static void removePlayer(UUID playerId) {
        HITBOX_DEBUG_PLAYERS.remove(playerId);
    }
}

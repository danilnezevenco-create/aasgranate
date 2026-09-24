package com.danilfb123.aasgranate;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Клиент -> сервер: сообщает, включён ли у игрока показ хитбоксов (F3+B).
 */
public class HitboxDebugPacket {

    private final boolean enabled;

    public HitboxDebugPacket(boolean enabled) {
        this.enabled = enabled;
    }

    public static void encode(HitboxDebugPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static HitboxDebugPacket decode(FriendlyByteBuf buf) {
        return new HitboxDebugPacket(buf.readBoolean());
    }

    public static void handle(HitboxDebugPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ModNetworking.setHitboxDebug(player.getUUID(), msg.enabled);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

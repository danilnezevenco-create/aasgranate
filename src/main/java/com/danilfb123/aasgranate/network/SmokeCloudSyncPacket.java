package com.danilfb123.aasgranate.network;

import com.danilfb123.aasgranate.client.ClientSmokeCloudManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * "Снимок" состояния дымового облака (M18 / RDG-2), рассылаемый отдельным
 * каналом с собственным, фиксированным радиусом (см. SmokeCloudBroadcaster).
 *
 * Он НЕ зависит от entity-broadcast-range-percentage (серверная "прогрузка
 * сущностей") и от clientTrackingRange самого EntityType. Это то, чего не
 * хватало раньше: раньше клиент узнавал о дыме только через обычный
 * entity-tracking, и как только сущность переставала трекаться — клиент её
 * немедленно удалял, а вместе с ней пропадал и визуал дыма, даже если по
 * своим собственным timer'ам (growTicks/holdTicks/fadeTicks) дым должен был
 * ещё долго гореть.
 *
 * Теперь клиент хранит состояние облака отдельно от факта существования
 * Entity на клиенте (см. ClientSmokeCloudManager) и продолжает рисовать дым,
 * пока не истечёт его собственное время жизни.
 */
public class SmokeCloudSyncPacket {

    public enum Kind { M18, RDG2 }

    private final int entityId;
    private final Kind kind;
    private final double x, y, z;
    private final int smokeTicks;

    public SmokeCloudSyncPacket(int entityId, Kind kind, double x, double y, double z, int smokeTicks) {
        this.entityId = entityId;
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.z = z;
        this.smokeTicks = smokeTicks;
    }

    public static void encode(SmokeCloudSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeEnum(msg.kind);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeVarInt(msg.smokeTicks);
    }

    public static SmokeCloudSyncPacket decode(FriendlyByteBuf buf) {
        return new SmokeCloudSyncPacket(
                buf.readVarInt(),
                buf.readEnum(Kind.class),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readVarInt());
    }

    public static void handle(SmokeCloudSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                ClientSmokeCloudManager.sync(msg.entityId, msg.kind, msg.x, msg.y, msg.z, msg.smokeTicks));
        ctx.get().setPacketHandled(true);
    }
}
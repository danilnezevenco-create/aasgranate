package com.danilfb123.aasgranate;

import com.danilfb123.aasgranate.network.SmokeCloudSyncPacket;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;

/**
 * Рассылает состояние дымового облака ВСЕМ игрокам в фиксированном радиусе
 * вокруг него — в обход обычного entity-tracking'а.
 *
 * Это ключевое отличие от текущего поведения: сейчас клиент узнаёт о
 * положении/тиках дыма только из синхронизации самой Entity, дистанция
 * которой регулируется clientTrackingRange и дополнительно масштабируется
 * серверным entity-broadcast-range-percentage ("прогрузка сущностей"). Если
 * этот процент занижен, дальность резко падает, и дым обрывается мгновенно,
 * а не гаснет по своему обычному fade.
 *
 * SYNC_RADIUS ниже — это независимая, всегда одинаковая дальность именно для
 * визуала дыма, вне зависимости от того, как настроен трекинг остальных
 * сущностей на сервере.
 */
public final class SmokeCloudBroadcaster {

    /** Радиус в блоках, на котором дым остаётся видимым, независимо от entity-broadcast-range-percentage. */
    public static final double SYNC_RADIUS = 256.0;

    private SmokeCloudBroadcaster() {}

    public static void sync(Entity smokeEntity, SmokeCloudSyncPacket.Kind kind, int smokeTicks) {
        if (smokeEntity.level().isClientSide) return;
        ModNetworking.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        smokeEntity.getX(), smokeEntity.getY(), smokeEntity.getZ(),
                        SYNC_RADIUS, smokeEntity.level().dimension())),
                new SmokeCloudSyncPacket(smokeEntity.getId(), kind,
                        smokeEntity.getX(), smokeEntity.getY(), smokeEntity.getZ(), smokeTicks));
    }
}
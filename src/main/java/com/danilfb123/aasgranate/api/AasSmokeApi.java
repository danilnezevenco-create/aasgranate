package com.danilfb123.aasgranate.api;

import com.danilfb123.aasgranate.AasSmokeSourceEntity;
import com.danilfb123.aasgranate.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Публичный вход для других модов, которым нужно то же объёмное дымовое
 * облако, что и у дымовых шашек этого мода (M18/RDG-2), но без самой гранаты.
 *
 * Использование в другом моде (например, дымогенератор или дымовой
 * гранатомёт техники):
 *
 *   // 1. Разовая шашка/канистра дыма, которая гаснет сама:
 *   AasSmokeSourceEntity cloud = AasSmokeApi.spawnSmokeCloud(
 *           serverLevel, landingPos,
 *           6.5F, 3.0F,      // радиус, высота (как у M18)
 *           600, 1200, 200); // growTicks, holdTicks, fadeTicks
 *
 *   // 2. Дым, который висит, пока активен генератор техники:
 *   AasSmokeSourceEntity generatorCloud = AasSmokeApi.spawnFollowingSmoke(
 *           serverLevel, vehicle, new Vec3(0.95, 0.7, 1.1),
 *           4.0F, 2.2F, 100, 40, 100);
 *   // ... каждый тик, пока генератор включён:
 *   AasSmokeApi.keepAlive(generatorCloud);
 *   // Больше ничего вызывать не нужно — как только keepAlive перестанут
 *   // вызывать, через ~2 сек облако само начнёт гаснуть и удалится.
 *
 * Модуль опционален для вызывающего мода: перед использованием стоит
 * проверить ModList.get().isLoaded("aasgranate"), если зависимость мягкая.
 */
public final class AasSmokeApi {

    private AasSmokeApi() {}

    /**
     * Создать "разовое" объёмное облако дыма в фиксированной точке мира.
     * Растёт growTicks, держится holdTicks, гаснет за fadeTicks, затем
     * само себя удаляет — вызывающей стороне ничего дальше делать не нужно.
     */
    public static AasSmokeSourceEntity spawnSmokeCloud(ServerLevel level, Vec3 pos,
            float radius, float height, int growTicks, int holdTicks, int fadeTicks) {
        AasSmokeSourceEntity cloud = new AasSmokeSourceEntity(ModEntities.AAS_SMOKE_SOURCE.get(), level);
        cloud.setPos(pos.x, pos.y, pos.z);
        cloud.configure(radius, height, growTicks, holdTicks, fadeTicks);
        level.addFreshEntity(cloud);
        return cloud;
    }

    /**
     * То же самое, но облако каждый тик пересчитывает свою позицию из
     * позиции/поворота carrier + localOffset — используйте это для дыма,
     * который должен ехать вместе с техникой (дымогенератор выхлопа и т.п.).
     * Если carrier исчезнет, облако останется висеть на последнем месте и
     * доиграет удержание/затухание само.
     */
    public static AasSmokeSourceEntity spawnFollowingSmoke(ServerLevel level, Entity carrier, Vec3 localOffset,
            float radius, float height, int growTicks, int holdTicks, int fadeTicks) {
        Vec3 worldPos = carrier.position().add(localOffset);
        AasSmokeSourceEntity cloud = spawnSmokeCloud(level, worldPos, radius, height, growTicks, holdTicks, fadeTicks);
        cloud.attachToCarrier(carrier, localOffset);
        return cloud;
    }

    /**
     * Продлить жизнь "постоянного" облака ещё на пару секунд вперёд.
     * Вызывать каждый тик, пока источник дыма (генератор, работающий
     * гранатомёт и т.п.) активен. Безопасно вызывать и на клиенте, и на
     * сервере — на клиенте вызов ничего не делает (состояние приходит с сервера).
     */
    public static void keepAlive(AasSmokeSourceEntity cloud) {
        if (cloud == null || cloud.isRemoved()) return;
        cloud.keepAlive();
    }
}

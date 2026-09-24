package com.danilfb123.aasgranate;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Общие визуальные эффекты взрыва гранаты: вспышка, огненные искры, облако
 * пыли/гари и дым, похожий на костровой (тянется вверх ещё несколько секунд
 * после подрыва).
 *
 * Вынесено в отдельный класс, чтобы M67Entity и Rgd5Entity использовали один
 * и тот же эффект и не дублировали код.
 */
public final class GrenadeVfxUtils {

    private GrenadeVfxUtils() {}

    public static void playExplosionVisuals(ServerLevel serverLevel, Vec3 center, Random rand) {

        // 1. Яркая вспышка в момент подрыва
        serverLevel.sendParticles(ParticleTypes.FLASH,
                center.x, center.y + 0.15, center.z,
                1, 0.0, 0.0, 0.0, 0.0);

        // 2. Короткая огненная сфера в эпицентре
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                center.x, center.y + 0.1, center.z,
                1, 0.0, 0.0, 0.0, 0.0);

        // 3. Горячие искры/угли, разлетающиеся во все стороны
        for (int i = 0; i < 40; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double horiz = 0.15 + rand.nextDouble() * 0.35;
            double vx = Math.cos(angle) * horiz;
            double vz = Math.sin(angle) * horiz;
            double vy = 0.2 + rand.nextDouble() * 0.35;

            serverLevel.sendParticles(
                    rand.nextFloat() < 0.5f ? ParticleTypes.LAVA : ParticleTypes.FLAME,
                    center.x, center.y + 0.1, center.z,
                    1, vx, vy, vz, 0.0);
        }

        // 4. Тёплое облако пыли/грунта у самого эпицентра (было и раньше, оставлено)
        for (int i = 0; i < 45; i++) {
            serverLevel.sendParticles(
                    new DustParticleOptions(new Vector3f(0.78f, 0.62f, 0.42f), 2.4f),
                    center.x + rand.nextGaussian() * 0.8,
                    center.y + Math.abs(rand.nextGaussian()) * 0.4,
                    center.z + rand.nextGaussian() * 0.8,
                    1, (rand.nextDouble() - 0.5) * 0.7, 0.3 + rand.nextDouble() * 0.5,
                    (rand.nextDouble() - 0.5) * 0.7, 0.0);
        }

        // 5. Внешнее, более тёмное и рассеянное облако гари (было и раньше, оставлено)
        for (int i = 0; i < 60; i++) {
            serverLevel.sendParticles(
                    new DustParticleOptions(new Vector3f(0.32f, 0.30f, 0.28f), 2.8f),
                    center.x + rand.nextGaussian() * 2.2,
                    center.y + rand.nextDouble() * 0.8,
                    center.z + rand.nextGaussian() * 2.2,
                    1, (rand.nextDouble() - 0.5) * 0.4, 0.1 + rand.nextDouble() * 0.25,
                    (rand.nextDouble() - 0.5) * 0.4, 0.0);
        }

        // 6. Хлопья пепла, медленно поднимающиеся и оседающие
        for (int i = 0; i < 25; i++) {
            serverLevel.sendParticles(ParticleTypes.ASH,
                    center.x + rand.nextGaussian() * 1.6,
                    center.y + 0.3 + rand.nextDouble() * 1.2,
                    center.z + rand.nextGaussian() * 1.6,
                    1, (rand.nextDouble() - 0.5) * 0.1, 0.02,
                    (rand.nextDouble() - 0.5) * 0.1, 0.0);
        }

        // 7. Плотный клуб дыма прямо в момент взрыва ("гриб")
        for (int i = 0; i < 18; i++) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    center.x + rand.nextGaussian() * 0.5,
                    center.y + 0.2 + rand.nextDouble() * 0.6,
                    center.z + rand.nextGaussian() * 0.5,
                    1, (rand.nextDouble() - 0.5) * 0.1, 0.08 + rand.nextDouble() * 0.08,
                    (rand.nextDouble() - 0.5) * 0.1, 0.0);
        }

        // 8. Костровой дым: место взрыва ещё несколько секунд тлеет и тянет
        // вверх лёгкий дымок, как обычный костёр (см. SmokeScheduler)
        SmokeScheduler.scheduleCampfireSmoke(serverLevel, center, 1, 5);
    }
}
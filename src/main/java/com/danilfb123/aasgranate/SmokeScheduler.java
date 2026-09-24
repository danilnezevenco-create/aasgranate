package com.danilfb123.aasgranate;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Лёгкий планировщик отложенных "пуфов" дыма на месте взрыва.
 *
 * Зачем это нужно: обычный serverLevel.sendParticles() шлёт частицы только
 * один раз, в момент вызова. Чтобы место взрыва ещё несколько секунд
 * действительно ТЛЕЛО и тянуло дым вверх (как костёр), нужно досылать новые
 * порции частиц на протяжении времени — этим и занимается этот класс,
 * подписываясь на серверный тик.
 *
 * Гранату (M67Entity/Rgd5Entity) саму по себе для этого хранить не нужно —
 * она discard()-ится сразу после взрыва, поэтому эффект привязан просто
 * к точке в мире (level + координаты), а не к сущности.
 */
@Mod.EventBusSubscriber(modid = AasGranate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SmokeScheduler {

    private static final List<SmokeTask> TASKS = new ArrayList<>();
    private static final Random RAND = new Random();

    /**
     * Запланировать серию "пуфов" кострового дыма в точке center.
     *
     * @param bursts        сколько всего порций дыма выпустить
     * @param intervalTicks интервал между порциями, в тиках (20 тиков = 1 сек)
     */
    public static void scheduleCampfireSmoke(ServerLevel level, Vec3 center, int bursts, int intervalTicks) {
        TASKS.add(new SmokeTask(level, center, bursts, intervalTicks));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (TASKS.isEmpty()) return;

        Iterator<SmokeTask> it = TASKS.iterator();
        while (it.hasNext()) {
            SmokeTask task = it.next();
            task.ticksUntilNext--;

            if (task.ticksUntilNext <= 0) {
                task.ticksUntilNext = task.intervalTicks;
                task.burstsLeft--;
                spawnPuff(task.level, task.center);

                if (task.burstsLeft <= 0) {
                    it.remove();
                }
            }
        }
    }

    private static void spawnPuff(ServerLevel level, Vec3 center) {
        // Небольшая горстка "костровых" частиц, слегка поднимающихся вверх
        for (int i = 0; i < 6; i++) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    center.x + RAND.nextGaussian() * 0.35,
                    center.y + 0.2 + RAND.nextDouble() * 0.3,
                    center.z + RAND.nextGaussian() * 0.35,
                    1, 0.0, 0.03 + RAND.nextDouble() * 0.02, 0.0, 0.0);
        }

        // Изредка добавляем более тонкую высокую струйку — как затухающий костёр
        if (RAND.nextFloat() < 0.5f) {
            level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    center.x, center.y + 0.25, center.z,
                    1, 0.1, 0.02, 0.1, 0.0);
        }
    }

    private static class SmokeTask {
        final ServerLevel level;
        final Vec3 center;
        int burstsLeft;
        final int intervalTicks;
        int ticksUntilNext;

        SmokeTask(ServerLevel level, Vec3 center, int bursts, int intervalTicks) {
            this.level = level;
            this.center = center;
            this.burstsLeft = bursts;
            this.intervalTicks = intervalTicks;
            this.ticksUntilNext = intervalTicks;
        }
    }
}
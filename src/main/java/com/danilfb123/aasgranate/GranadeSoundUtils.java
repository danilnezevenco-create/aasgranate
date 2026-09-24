package com.danilfb123.aasgranate;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Общая утилита для гранат (M67, RGD-5 и т.д.): рассылает звук взрыва
 * каждому игроку персонально, выбирая сэмпл в зависимости от его
 * расстояния до точки взрыва:
 *   0-24   -> close
 *   24-64  -> mid
 *   64-128 -> far
 *   128-256-> distant
 * Дальше 128 блоков звук не отправляется.
 *
 * ВАЖНО: раньше здесь использовался ServerLevel#playSound(Player, ...).
 * У этого метода есть особенность — сервер шлёт пакет со звуком ВСЕМ
 * игрокам, КРОМЕ переданного в параметре player (он используется для
 * случаев, когда клиент уже проиграл звук локально сам). Из-за этого:
 *   - в одиночной игре звук взрыва не проигрывался вообще (единственный
 *     игрок каждый раз сам оказывался тем, кого исключают из рассылки);
 *   - в мультиплеере игроки получали НЕ СВОЙ вариант звука/громкости,
 *     а тот, что был посчитан для другого игрока в этой итерации.
 *
 * Исправлено: пакет ClientboundSoundPacket собирается вручную и
 * отправляется конкретному игроку через player.connection.send(...),
 * без исключающей логики.
 */
public final class GranadeSoundUtils {

    private GranadeSoundUtils() {
        // utility class, экземпляры не нужны
    }

    public static void playBlastSoundByDistance(ServerLevel level, Vec3 blastPos,
                                                SoundEvent close,
                                                SoundEvent mid,
                                                SoundEvent far,
                                                SoundEvent distant,
                                                float pitch) {
        long seed = level.getRandom().nextLong();

        for (ServerPlayer player : level.players()) {
            double dist = player.position().distanceTo(blastPos);
            if (dist > 256.0) continue;

            SoundEvent sound;
            float volume;
            if (dist <= 24.0) {
                sound = close;
                volume = 4.0F;
            } else if (dist <= 64.0) {
                sound = mid;
                volume = 4.0F;
            } else if (dist <= 128.0) {
                sound = far;
                volume = 6.0F;
            } else {
                sound = distant;
                volume = 8.0F;
            }

            Holder<SoundEvent> soundHolder = ForgeRegistries.SOUND_EVENTS
                    .getDelegate(sound)
                    .orElseThrow(() -> new IllegalStateException(
                            "Sound event is not registered: " + sound));

            ClientboundSoundPacket packet = new ClientboundSoundPacket(
                    soundHolder, SoundSource.HOSTILE,
                    blastPos.x, blastPos.y, blastPos.z,
                    volume, pitch, seed);

            player.connection.send(packet);
        }
    }
}
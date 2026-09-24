package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.ModSounds;
import com.danilfb123.aasgranate.Rdg2Entity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * Зацикленный звук горящей дымовой шашки.
 *
 * Звук привязан к сущности: каждый тик обновляются координаты, так что если
 * граната катится — звук едет вместе с ней. Затухание по расстоянию делает
 * сам звуковой движок (ATTENUATION LINEAR), поэтому шашку слышно только вблизи.
 * Громкость дополнительно приглушается на раскрытии и на затухании облака.
 */
public class Rdg2SmokeSoundInstance extends AbstractTickableSoundInstance {

    private final Rdg2Entity grenade;

    public Rdg2SmokeSoundInstance(Rdg2Entity grenade) {
        super(ModSounds.RDG2_SMOKE_LOOP.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.grenade = grenade;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.x = (float) grenade.getX();
        this.y = (float) grenade.getY();
        this.z = (float) grenade.getZ();
    }

    @Override
    public void tick() {
        if (!this.grenade.isAlive() || !this.grenade.isSmoking()) {
            this.stop();
            return;
        }

        this.x = (float) this.grenade.getX();
        this.y = (float) this.grenade.getY();
        this.z = (float) this.grenade.getZ();

        // Чуть тише на разгоне и на затухании, полный звук — пока шашка работает в полную.
        float density = this.grenade.getSmokeDensity(1.0F);
        this.volume = 0.35F + 0.65F * Math.min(1.0F, density * 2.0F);
    }
}

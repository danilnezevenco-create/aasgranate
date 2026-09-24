package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.M18Entity;
import com.danilfb123.aasgranate.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * Зацикленный звук горящей дымовой шашки M18. Полный аналог Rdg2SmokeSoundInstance.
 */
public class M18SmokeSoundInstance extends AbstractTickableSoundInstance {

    private final M18Entity grenade;

    public M18SmokeSoundInstance(M18Entity grenade) {
        super(ModSounds.M18_SMOKE_LOOP.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
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

        float density = this.grenade.getSmokeDensity(1.0F);
        this.volume = 0.35F + 0.65F * Math.min(1.0F, density * 2.0F);
    }
}

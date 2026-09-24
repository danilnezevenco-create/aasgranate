package com.danilfb123.aasgranate.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;

/** Immutable random attributes; weak keys do not retain removed grenades/worlds. */
final class SmokePuffCache {
    private static final Map<Entity, Puff[]> CACHE = new WeakHashMap<>();

    private SmokePuffCache() {}

    static Puff[] get(Entity entity, int count) {
        Puff[] puffs = CACHE.get(entity);
        if (puffs == null || puffs.length != count) {
            puffs = new Puff[count];
            for (int i = 0; i < count; i++) puffs[i] = new Puff(entity.getId() * 8191 + i * 131);
            CACHE.put(entity, puffs);
        }
        return puffs;
    }

    static void clear() { CACHE.clear(); }

    static final class Puff {
        final float azimuth, rFrac, yFrac, sizeVar, spin, p1, p2, p3, greyVar, alphaFactor;

        Puff(int seed) {
            azimuth = hash(seed) * ((float) Math.PI * 2.0F);
            rFrac = (float) Math.sqrt(hash(seed + 1));
            yFrac = (float) Math.pow(hash(seed + 2), 1.35);
            sizeVar = 0.55F + 0.55F * hash(seed + 3);
            spin = (hash(seed + 4) - 0.5F) * 0.0035F;
            p1 = hash(seed + 5) * 6.2832F;
            p2 = hash(seed + 6) * 6.2832F;
            p3 = hash(seed + 7) * 6.2832F;
            greyVar = (hash(seed + 8) - 0.5F) * 0.12F;
            alphaFactor = (0.75F + 0.5F * hash(seed + 9)) * (1.0F - 0.40F * rFrac);
        }
    }

    private static float hash(int seed) {
        int h = seed * 0x27D4EB2D;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return (h >>> 8) / (float) (1 << 24);
    }
}

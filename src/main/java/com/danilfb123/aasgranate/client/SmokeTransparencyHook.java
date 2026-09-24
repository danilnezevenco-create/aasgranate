package com.danilfb123.aasgranate.client;

import java.lang.reflect.Field;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Forge 1.20.1 has an AFTER_TRANSLUCENT event, but no BEFORE_TRANSLUCENT event.
 * Wrap just this RenderType's two state callbacks once, retaining the original callbacks.
 * The SRG field names are remapped by Forge in both development and installed builds.
 * No per-frame reflection and no interception of block breaking, entities or other layers.
 */
final class SmokeTransparencyHook {
    private static boolean installed;

    private SmokeTransparencyHook() {}

    static void install() {
        if (installed) return;
        RenderType type = RenderType.translucent();
        Field setup = ObfuscationReflectionHelper.findField(RenderStateShard.class, "f_110131_");
        Field clear = ObfuscationReflectionHelper.findField(RenderStateShard.class, "f_110132_");
        try {
            Runnable originalSetup = (Runnable) setup.get(type);
            Runnable originalClear = (Runnable) clear.get(type);
            setup.set(type, (Runnable) () -> {
                originalSetup.run();
                SmokePipeline.beginTranslucent();
            });
            try {
                clear.set(type, (Runnable) () -> {
                    try {
                        SmokePipeline.endTranslucent();
                    } finally {
                        originalClear.run();
                    }
                });
            } catch (IllegalAccessException e) {
                setup.set(type, originalSetup);
                throw e;
            }
            installed = true;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot install aasgranate smoke transparency for Forge 1.20.1", e);
        }
    }
}

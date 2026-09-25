package com.danilfb123.aasgranate.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Мягкое пятно дыма (тот же восьмиугольник, что в smoke_cloud.fsh) для режима шейдерпака. */
final class SmokePuffTexture {
    static final ResourceLocation ID = new ResourceLocation("aasgranate", "dynamic/smoke_puff");
    private static final int SIZE = 64;
    private static boolean ready;

    private SmokePuffTexture() {}

    static void ensure() {
        if (ready) return;
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, SIZE, SIZE, false);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float px = Math.abs((x + 0.5F) / SIZE * 2.0F - 1.0F);
                float py = Math.abs((y + 0.5F) / SIZE * 2.0F - 1.0F);
                float edge = Math.max(px + 0.41421356F * py, py + 0.41421356F * px);
                float cov = Math.max(0.0F, Math.min(1.0F, (1.0F - edge) / 0.40F));
                int a = (int) (cov * 255.0F);
                img.setPixelRGBA(x, y, (a << 24) | 0x00FFFFFF);
            }
        }
        Minecraft.getInstance().getTextureManager().register(ID, new DynamicTexture(img));
        ready = true;
    }

    static void invalidate() { ready = false; }
}
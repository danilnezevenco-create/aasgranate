package com.danilfb123.aasgranate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.danilfb123.aasgranate.M18Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class M18SmokeCloudRenderer {

    private M18SmokeCloudRenderer() {}

    private static final int PUFF_COUNT = 260;


    private static final float PUFF_ALPHA = 0.30F;


    private static final float BASE_GREY = 0.58F;

    private static final int TRAIL_PUFFS = 3;

    private static final float TRAIL_ALPHA = 0.20F;

    public static void render(M18Entity entity, float partialTick, PoseStack poseStack,
                              MultiBufferSource bufferSource, int packedLight) {

        float density = entity.getSmokeDensity(partialTick);
        if (density <= 0.002F) return;

        float t = entity.getSmokeTicks(partialTick);
        if (t < 0.0F) return;

        int blockLight = LightTexture.block(packedLight);
        int skyLight   = LightTexture.sky(packedLight);
        float lum = Math.max(blockLight, skyLight) / 15.0F;
        float bright = 0.30F + 0.70F * lum;

        Quaternionf camera = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        Vector3f right = camera.transform(new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f up    = camera.transform(new Vector3f(0.0F, 1.0F, 0.0F));

        VertexConsumer vc = bufferSource.getBuffer(SmokeRenderTypes.SMOKE);
        Matrix4f pose = poseStack.last().pose();

        renderTrail(entity, partialTick, vc, pose, right, up, bright);

        float growth = entity.getSmokeGrowth(partialTick);
        float inv = 1.0F - growth;
        float ease = 1.0F - inv * inv * inv;

        float radius = M18Entity.SMOKE_RADIUS * (0.12F + 0.88F * ease);
        float height = M18Entity.SMOKE_HEIGHT * (0.15F + 0.85F * ease);

        SmokePuffCache.Puff[] puffs = SmokePuffCache.get(entity, PUFF_COUNT);

        for (int i = 0; i < PUFF_COUNT; i++) {
            SmokePuffCache.Puff puff = puffs[i];
            float alpha = PUFF_ALPHA * puff.alphaFactor * density;
            if (alpha <= 0.002F) continue;
            float azimuth = puff.azimuth, rFrac = puff.rFrac, yFrac = puff.yFrac;
            float sizeVar = puff.sizeVar, spin = puff.spin;
            float p1 = puff.p1, p2 = puff.p2, p3 = puff.p3;
            float greyVar = puff.greyVar;

            float ang = azimuth + t * spin;
            float rr  = rFrac * radius * (0.92F + 0.08F * sin(t * 0.010F + p1));

            float px = cos(ang) * rr + sin(t * 0.017F + p2) * 0.10F * radius;
            float pz = sin(ang) * rr + cos(t * 0.013F + p3) * 0.10F * radius;
            float py = 0.10F + yFrac * height + sin(t * 0.011F + p1) * 0.06F * height;

            if (py < 0.05F) py = 0.05F;
            if (py > height) py = height;

            float puffR = sizeVar * radius * 0.62F * (0.9F + 0.1F * sin(t * 0.02F + p2));

            float grey = (BASE_GREY + greyVar) * bright;
            int cr = clamp255(grey * 255.0F);
            int cg = clamp255(grey * 255.0F);
            int cb = clamp255((grey * 0.99F) * 255.0F);
            int ca = clamp255(alpha * 255.0F);
            if (ca <= 0) continue;

            drawPuff(vc, pose, right, up, px, py, pz, puffR, cr, cg, cb, ca);
        }
    }

    private static void renderTrail(M18Entity entity, float partialTick,
                                    VertexConsumer vc, Matrix4f pose,
                                    Vector3f right, Vector3f up, float bright) {

        double ex = Mth.lerp(partialTick, entity.xo, entity.getX());
        double ey = Mth.lerp(partialTick, entity.yo, entity.getY());
        double ez = Mth.lerp(partialTick, entity.zo, entity.getZ());

        for (M18Entity.TrailPuff puff : entity.getTrail()) {
            float age = puff.age + partialTick;
            float life = age / M18Entity.TRAIL_LIFE_TICKS;
            if (life >= 1.0F) continue;

            float fadeIn  = Math.min(1.0F, age / 4.0F);
            float fadeOut = 1.0F - life * life;
            float alphaBase = TRAIL_ALPHA * fadeIn * fadeOut;
            if (alphaBase <= 0.002F) continue;

            float grow = 0.22F + 0.85F * life;

            float ox = (float) (puff.x - ex);
            float oy = (float) (puff.y - ey);
            float oz = (float) (puff.z - ez);

            for (int k = 0; k < TRAIL_PUFFS; k++) {
                int s = puff.seed + k * 977;

                float dx = (hash(s)     - 0.5F) * 0.55F * grow;
                float dy = (hash(s + 1) - 0.5F) * 0.55F * grow + life * 0.45F;
                float dz = (hash(s + 2) - 0.5F) * 0.55F * grow;

                float r = grow * (0.45F + 0.45F * hash(s + 3));
                float greyVar = (hash(s + 4) - 0.5F) * 0.10F;
                float grey = (BASE_GREY + greyVar) * bright;

                int cr = clamp255(grey * 255.0F);
                int ca = clamp255(alphaBase * (0.7F + 0.6F * hash(s + 5)) * 255.0F);
                if (ca <= 0) continue;

                drawPuff(vc, pose, right, up, ox + dx, oy + dy, oz + dz, r, cr, cr, cr, ca);
            }
        }
    }

    private static void drawPuff(VertexConsumer vc, Matrix4f pose, Vector3f right, Vector3f up,
                                 float cx, float cy, float cz, float r,
                                 int cr, int cg, int cb, int ca) {
        SmokeBillboard.draw(vc, pose, right, up, cx, cy, cz, r, cr, cg, cb, ca);
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

    private static float sin(float a) { return (float) Math.sin(a); }
    private static float cos(float a) { return (float) Math.cos(a); }

    private static int clamp255(float v) {
        int i = (int) v;
        if (i < 0) return 0;
        return Math.min(i, 255);
    }
}

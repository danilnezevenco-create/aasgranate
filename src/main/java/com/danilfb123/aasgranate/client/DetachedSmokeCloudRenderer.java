package com.danilfb123.aasgranate.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * То же самое облако из "затяжек" (puffs), что рисует SmokeCloudRenderer /
 * M18SmokeCloudRenderer — но не завязанное на живую Entity. Используется
 * только для облаков, у которых на клиенте прямо сейчас нет соответствующей
 * сущности (см. SmokeStageRenderer), то есть именно в том случае, который
 * раньше приводил к мгновенному исчезновению дыма при маленькой дистанции
 * трекинга сущностей.
 *
 * Шлейф (trail) намеренно не рисуется: он актуален только пока граната летит,
 * а в этот момент сущность и так гарантированно ещё трекается (игрок рядом).
 */
public final class DetachedSmokeCloudRenderer {

    private static final int PUFF_COUNT = 260;
    private static final float PUFF_ALPHA = 0.30F;
    private static final float BASE_GREY = 0.58F;

    private DetachedSmokeCloudRenderer() {}

    public static void render(int cloudId, ClientSmokeCloudManager.Cloud cloud, float partialTick,
                              com.mojang.blaze3d.vertex.PoseStack poseStack,
                              MultiBufferSource bufferSource, int packedLight) {

        float density = cloud.getSmokeDensity(partialTick);
        if (density <= 0.002F) return;

        float t = cloud.getSmokeTicks(partialTick);

        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        float lum = Math.max(blockLight, skyLight) / 15.0F;
        float bright = (0.30F + 0.70F * lum) * SmokeTimeOfDay.ambient();
        float tint = 1.0F - 0.10F * SmokeTimeOfDay.coolTint();

        Quaternionf camera = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        Vector3f right = camera.transform(new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f up = camera.transform(new Vector3f(0.0F, 1.0F, 0.0F));

        VertexConsumer vc = bufferSource.getBuffer(SmokeRenderTypes.SMOKE);
        Matrix4f pose = poseStack.last().pose();

        float growth = cloud.getSmokeGrowth(partialTick);
        float inv = 1.0F - growth;
        float ease = 1.0F - inv * inv * inv;

        float radius = cloud.getRadius() * (0.12F + 0.88F * ease);
        float height = cloud.getHeight() * (0.15F + 0.85F * ease);

        SmokePuffCache.Puff[] puffs = SmokePuffCache.get(cloudId, PUFF_COUNT);

        for (int i = 0; i < PUFF_COUNT; i++) {
            SmokePuffCache.Puff puff = puffs[i];
            float alpha = PUFF_ALPHA * puff.alphaFactor * density;
            if (alpha <= 0.002F) continue;

            float ang = puff.azimuth + t * puff.spin;
            float rr = puff.rFrac * radius * (0.92F + 0.08F * sin(t * 0.010F + puff.p1));

            float px = cos(ang) * rr + sin(t * 0.017F + puff.p2) * 0.10F * radius;
            float pz = sin(ang) * rr + cos(t * 0.013F + puff.p3) * 0.10F * radius;
            float py = 0.10F + puff.yFrac * height + sin(t * 0.011F + puff.p1) * 0.06F * height;
            if (py < 0.05F) py = 0.05F;
            if (py > height) py = height;

            float puffR = puff.sizeVar * radius * 0.62F * (0.9F + 0.1F * sin(t * 0.02F + puff.p2));

            float grey = (BASE_GREY + puff.greyVar) * bright;
            int cr = clamp255(grey * tint * 255.0F);
            int ca = clamp255(alpha * 255.0F);
            if (ca <= 0) continue;

            SmokeBillboard.draw(vc, pose, right, up, px, py, pz, puffR, cr, cr, cr, ca);
        }
    }

    /** Вызывается менеджером, когда облако окончательно снято с учёта — чистим кэш затяжек. */
    static void onCloudRemoved(int cloudId) {
        SmokePuffCache.remove(cloudId);
    }

    private static float sin(float a) { return (float) Math.sin(a); }
    private static float cos(float a) { return (float) Math.cos(a); }

    private static int clamp255(float v) {
        int i = (int) v;
        if (i < 0) return 0;
        return Math.min(i, 255);
    }
}
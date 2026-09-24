package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.Rdg2Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Процедурные искры у корпуса дымовой шашки.
 *
 * Как и дым, искры не используют ни одной текстуры: каждая искра — это
 * маленький вытянутый квад-«штрих» (голова к камере ярче, хвост гаснет в ноль),
 * ориентированный вдоль направления полёта самой искры. Рисуются отдельным
 * аддитивным RenderType (SmokeRenderTypes.SPARKS), поэтому они светятся и
 * заметны даже на фоне плотного дыма.
 *
 * --- Оптимизация (картинка не меняется) ---
 * Раньше на каждую искру каждый кадр создавалось 3 новых Vector3f
 * (dir/widthAxis/tailOffset). При SPARK_MAX = 40 искр на шашку и нескольких
 * одновременно летящих гранатах это заметная нагрузка на GC. Теперь под
 * dir/widthAxis/tailOffset заведены переиспользуемые «черновики», которые
 * на каждой искре просто перезаписываются (.set/.cross(...,dest)/.mul(...,dest))
 * вместо аллокации новых объектов. Числа получаются ровно те же самые.
 */
public final class SparkRenderer {

    private SparkRenderer() {}

    /** Половина ширины штриха искры, в блоках. */
    private static final float SPARK_WIDTH = 0.028F;
    /** Множитель длины штриха относительно скорости искры (чем быстрее — тем длиннее след). */
    private static final float STREAK_LENGTH_SCALE = 2.2F;
    /** Минимальная длина штриха, чтобы даже медленные искры были видны как чёрточка. */
    private static final float STREAK_LENGTH_MIN = 0.05F;

    public static void render(Rdg2Entity entity, float partialTick, PoseStack poseStack,
                              MultiBufferSource bufferSource) {

        Iterable<Rdg2Entity.Spark> sparks = entity.getSparks();

        // Быстрая проверка «есть ли вообще что рисовать», чтобы не трогать буфер зря.
        if (!sparks.iterator().hasNext()) return;

        double ex = Mth.lerp(partialTick, entity.xo, entity.getX());
        double ey = Mth.lerp(partialTick, entity.yo, entity.getY());
        double ez = Mth.lerp(partialTick, entity.zo, entity.getZ());

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        // В этой версии Forge/Minecraft getLookVector() возвращает JOML Vector3f,
        // а не net.minecraft.world.phys.Vec3 — берём как есть, без промежуточной конвертации.
        Vector3f viewDir = new Vector3f(camera.getLookVector());

        VertexConsumer vc = bufferSource.getBuffer(SmokeRenderTypes.SPARKS);
        Matrix4f pose = poseStack.last().pose();

        // Черновики под direction/widthAxis/tailOffset — заводятся один раз на вызов
        // и переиспользуются для каждой искры, вместо `new Vector3f(...)` в цикле.
        Vector3f dir = new Vector3f();
        Vector3f widthAxis = new Vector3f();
        Vector3f tailOffset = new Vector3f();

        for (Rdg2Entity.Spark spark : sparks) {
            float age = spark.age + partialTick;
            float life = age / spark.life;
            if (life >= 1.0F) continue;

            // Позиция искры на этом кадре (линейно докручиваем её локальную скорость).
            float px = (float) (spark.x - ex + spark.vx * partialTick);
            float py = (float) (spark.y - ey + spark.vy * partialTick);
            float pz = (float) (spark.z - ez + spark.vz * partialTick);

            dir.set((float) spark.vx, (float) spark.vy, (float) spark.vz);
            float speed = dir.length();
            if (speed < 1.0e-4F) continue;
            dir.div(speed);

            // Ось ширины штриха: перпендикулярна и направлению искры, и взгляду камеры.
            dir.cross(viewDir, widthAxis);
            if (widthAxis.lengthSquared() < 1.0e-6F) {
                widthAxis.set(1.0F, 0.0F, 0.0F);
            } else {
                widthAxis.normalize();
            }
            widthAxis.mul(SPARK_WIDTH);

            float streakLen = Math.max(STREAK_LENGTH_MIN, speed * STREAK_LENGTH_SCALE);
            dir.mul(-streakLen, tailOffset);

            // Голова горячая и яркая, хвост гаснет и в цвете, и в альфе — эффект «уголька».
            float fadeIn = Math.min(1.0F, age / 2.0F); // не мигает мгновенно на спавне
            float headAlpha = (1.0F - life) * fadeIn;

            int[] headColor = sparkColor(life, headAlpha);
            int[] tailColor = sparkColor(Math.min(1.0F, life + 0.35F), 0.0F);

            float hx = px + widthAxis.x(), hy = py + widthAxis.y(), hz = pz + widthAxis.z();
            float hx2 = px - widthAxis.x(), hy2 = py - widthAxis.y(), hz2 = pz - widthAxis.z();
            float tx = px + tailOffset.x() + widthAxis.x() * 0.35F;
            float ty = py + tailOffset.y() + widthAxis.y() * 0.35F;
            float tz = pz + tailOffset.z() + widthAxis.z() * 0.35F;
            float tx2 = px + tailOffset.x() - widthAxis.x() * 0.35F;
            float ty2 = py + tailOffset.y() - widthAxis.y() * 0.35F;
            float tz2 = pz + tailOffset.z() - widthAxis.z() * 0.35F;

            vertex(vc, pose, hx, hy, hz, headColor);
            vertex(vc, pose, hx2, hy2, hz2, headColor);
            vertex(vc, pose, tx2, ty2, tz2, tailColor);
            vertex(vc, pose, tx, ty, tz, tailColor);
        }
    }

    /**
     * Цвет искры по её «жизни» (0 = только родилась, 1 = гаснет):
     * ярко-жёлтая/белая -> оранжевая -> тёмно-красная.
     */
    private static int[] sparkColor(float life, float alpha) {
        life = Mth.clamp(life, 0.0F, 1.0F);

        int r, g, b;
        if (life < 0.5F) {
            float f = life / 0.5F;
            r = 255;
            g = lerpInt(235, 130, f);
            b = lerpInt(160, 30, f);
        } else {
            float f = (life - 0.5F) / 0.5F;
            r = lerpInt(255, 110, f);
            g = lerpInt(130, 20, f);
            b = lerpInt(30, 10, f);
        }

        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        return new int[]{r, g, b, a};
    }

    private static int lerpInt(int from, int to, float f) {
        return from + (int) ((to - from) * f);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, float x, float y, float z, int[] color) {
        vc.vertex(pose, x, y, z).color(color[0], color[1], color[2], color[3]).endVertex();
    }
}
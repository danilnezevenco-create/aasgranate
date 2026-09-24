package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.M18Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * РџСЂРѕС†РµРґСѓСЂРЅС‹Рµ РёСЃРєСЂС‹ Сѓ РєРѕСЂРїСѓСЃР° РґС‹РјРѕРІРѕР№ С€Р°С€РєРё.
 *
 * РљР°Рє Рё РґС‹Рј, РёСЃРєСЂС‹ РЅРµ РёСЃРїРѕР»СЊР·СѓСЋС‚ РЅРё РѕРґРЅРѕР№ С‚РµРєСЃС‚СѓСЂС‹: РєР°Р¶РґР°СЏ РёСЃРєСЂР° вЂ” СЌС‚Рѕ
 * РјР°Р»РµРЅСЊРєРёР№ РІС‹С‚СЏРЅСѓС‚С‹Р№ РєРІР°Рґ-В«С€С‚СЂРёС…В» (РіРѕР»РѕРІР° Рє РєР°РјРµСЂРµ СЏСЂС‡Рµ, С…РІРѕСЃС‚ РіР°СЃРЅРµС‚ РІ РЅРѕР»СЊ),
 * РѕСЂРёРµРЅС‚РёСЂРѕРІР°РЅРЅС‹Р№ РІРґРѕР»СЊ РЅР°РїСЂР°РІР»РµРЅРёСЏ РїРѕР»С‘С‚Р° СЃР°РјРѕР№ РёСЃРєСЂС‹. Р РёСЃСѓСЋС‚СЃСЏ РѕС‚РґРµР»СЊРЅС‹Рј
 * Р°РґРґРёС‚РёРІРЅС‹Рј RenderType (SmokeRenderTypes.SPARKS), РїРѕСЌС‚РѕРјСѓ РѕРЅРё СЃРІРµС‚СЏС‚СЃСЏ Рё
 * Р·Р°РјРµС‚РЅС‹ РґР°Р¶Рµ РЅР° С„РѕРЅРµ РїР»РѕС‚РЅРѕРіРѕ РґС‹РјР°.
 *
 * --- РћРїС‚РёРјРёР·Р°С†РёСЏ (РєР°СЂС‚РёРЅРєР° РЅРµ РјРµРЅСЏРµС‚СЃСЏ) ---
 * Р Р°РЅСЊС€Рµ РЅР° РєР°Р¶РґСѓСЋ РёСЃРєСЂСѓ РєР°Р¶РґС‹Р№ РєР°РґСЂ СЃРѕР·РґР°РІР°Р»РѕСЃСЊ 3 РЅРѕРІС‹С… Vector3f
 * (dir/widthAxis/tailOffset). РџСЂРё SPARK_MAX = 40 РёСЃРєСЂ РЅР° С€Р°С€РєСѓ Рё РЅРµСЃРєРѕР»СЊРєРёС…
 * РѕРґРЅРѕРІСЂРµРјРµРЅРЅРѕ Р»РµС‚СЏС‰РёС… РіСЂР°РЅР°С‚Р°С… СЌС‚Рѕ Р·Р°РјРµС‚РЅР°СЏ РЅР°РіСЂСѓР·РєР° РЅР° GC. РўРµРїРµСЂСЊ РїРѕРґ
 * dir/widthAxis/tailOffset Р·Р°РІРµРґРµРЅС‹ РїРµСЂРµРёСЃРїРѕР»СЊР·СѓРµРјС‹Рµ В«С‡РµСЂРЅРѕРІРёРєРёВ», РєРѕС‚РѕСЂС‹Рµ
 * РЅР° РєР°Р¶РґРѕР№ РёСЃРєСЂРµ РїСЂРѕСЃС‚Рѕ РїРµСЂРµР·Р°РїРёСЃС‹РІР°СЋС‚СЃСЏ (.set/.cross(...,dest)/.mul(...,dest))
 * РІРјРµСЃС‚Рѕ Р°Р»Р»РѕРєР°С†РёРё РЅРѕРІС‹С… РѕР±СЉРµРєС‚РѕРІ. Р§РёСЃР»Р° РїРѕР»СѓС‡Р°СЋС‚СЃСЏ СЂРѕРІРЅРѕ С‚Рµ Р¶Рµ СЃР°РјС‹Рµ.
 */
public final class M18SparkRenderer {

    private M18SparkRenderer() {}

    /** РџРѕР»РѕРІРёРЅР° С€РёСЂРёРЅС‹ С€С‚СЂРёС…Р° РёСЃРєСЂС‹, РІ Р±Р»РѕРєР°С…. */
    private static final float SPARK_WIDTH = 0.028F;
    /** РњРЅРѕР¶РёС‚РµР»СЊ РґР»РёРЅС‹ С€С‚СЂРёС…Р° РѕС‚РЅРѕСЃРёС‚РµР»СЊРЅРѕ СЃРєРѕСЂРѕСЃС‚Рё РёСЃРєСЂС‹ (С‡РµРј Р±С‹СЃС‚СЂРµРµ вЂ” С‚РµРј РґР»РёРЅРЅРµРµ СЃР»РµРґ). */
    private static final float STREAK_LENGTH_SCALE = 2.2F;
    /** РњРёРЅРёРјР°Р»СЊРЅР°СЏ РґР»РёРЅР° С€С‚СЂРёС…Р°, С‡С‚РѕР±С‹ РґР°Р¶Рµ РјРµРґР»РµРЅРЅС‹Рµ РёСЃРєСЂС‹ Р±С‹Р»Рё РІРёРґРЅС‹ РєР°Рє С‡С‘СЂС‚РѕС‡РєР°. */
    private static final float STREAK_LENGTH_MIN = 0.05F;

    public static void render(M18Entity entity, float partialTick, PoseStack poseStack,
                              MultiBufferSource bufferSource) {

        Iterable<M18Entity.Spark> sparks = entity.getSparks();

        // Р‘С‹СЃС‚СЂР°СЏ РїСЂРѕРІРµСЂРєР° В«РµСЃС‚СЊ Р»Рё РІРѕРѕР±С‰Рµ С‡С‚Рѕ СЂРёСЃРѕРІР°С‚СЊВ», С‡С‚РѕР±С‹ РЅРµ С‚СЂРѕРіР°С‚СЊ Р±СѓС„РµСЂ Р·СЂСЏ.
        if (!sparks.iterator().hasNext()) return;

        double ex = Mth.lerp(partialTick, entity.xo, entity.getX());
        double ey = Mth.lerp(partialTick, entity.yo, entity.getY());
        double ez = Mth.lerp(partialTick, entity.zo, entity.getZ());

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        // Р’ СЌС‚РѕР№ РІРµСЂСЃРёРё Forge/Minecraft getLookVector() РІРѕР·РІСЂР°С‰Р°РµС‚ JOML Vector3f,
        // Р° РЅРµ net.minecraft.world.phys.Vec3 вЂ” Р±РµСЂС‘Рј РєР°Рє РµСЃС‚СЊ, Р±РµР· РїСЂРѕРјРµР¶СѓС‚РѕС‡РЅРѕР№ РєРѕРЅРІРµСЂС‚Р°С†РёРё.
        Vector3f viewDir = new Vector3f(camera.getLookVector());

        VertexConsumer vc = bufferSource.getBuffer(SmokeRenderTypes.SPARKS);
        Matrix4f pose = poseStack.last().pose();

        // Р§РµСЂРЅРѕРІРёРєРё РїРѕРґ direction/widthAxis/tailOffset вЂ” Р·Р°РІРѕРґСЏС‚СЃСЏ РѕРґРёРЅ СЂР°Р· РЅР° РІС‹Р·РѕРІ
        // Рё РїРµСЂРµРёСЃРїРѕР»СЊР·СѓСЋС‚СЃСЏ РґР»СЏ РєР°Р¶РґРѕР№ РёСЃРєСЂС‹, РІРјРµСЃС‚Рѕ `new Vector3f(...)` РІ С†РёРєР»Рµ.
        Vector3f dir = new Vector3f();
        Vector3f widthAxis = new Vector3f();
        Vector3f tailOffset = new Vector3f();

        for (M18Entity.Spark spark : sparks) {
            float age = spark.age + partialTick;
            float life = age / spark.life;
            if (life >= 1.0F) continue;

            // РџРѕР·РёС†РёСЏ РёСЃРєСЂС‹ РЅР° СЌС‚РѕРј РєР°РґСЂРµ (Р»РёРЅРµР№РЅРѕ РґРѕРєСЂСѓС‡РёРІР°РµРј РµС‘ Р»РѕРєР°Р»СЊРЅСѓСЋ СЃРєРѕСЂРѕСЃС‚СЊ).
            float px = (float) (spark.x - ex + spark.vx * partialTick);
            float py = (float) (spark.y - ey + spark.vy * partialTick);
            float pz = (float) (spark.z - ez + spark.vz * partialTick);

            dir.set((float) spark.vx, (float) spark.vy, (float) spark.vz);
            float speed = dir.length();
            if (speed < 1.0e-4F) continue;
            dir.div(speed);

            // РћСЃСЊ С€РёСЂРёРЅС‹ С€С‚СЂРёС…Р°: РїРµСЂРїРµРЅРґРёРєСѓР»СЏСЂРЅР° Рё РЅР°РїСЂР°РІР»РµРЅРёСЋ РёСЃРєСЂС‹, Рё РІР·РіР»СЏРґСѓ РєР°РјРµСЂС‹.
            dir.cross(viewDir, widthAxis);
            if (widthAxis.lengthSquared() < 1.0e-6F) {
                widthAxis.set(1.0F, 0.0F, 0.0F);
            } else {
                widthAxis.normalize();
            }
            widthAxis.mul(SPARK_WIDTH);

            float streakLen = Math.max(STREAK_LENGTH_MIN, speed * STREAK_LENGTH_SCALE);
            dir.mul(-streakLen, tailOffset);

            // Р“РѕР»РѕРІР° РіРѕСЂСЏС‡Р°СЏ Рё СЏСЂРєР°СЏ, С…РІРѕСЃС‚ РіР°СЃРЅРµС‚ Рё РІ С†РІРµС‚Рµ, Рё РІ Р°Р»СЊС„Рµ вЂ” СЌС„С„РµРєС‚ В«СѓРіРѕР»СЊРєР°В».
            float fadeIn = Math.min(1.0F, age / 2.0F); // РЅРµ РјРёРіР°РµС‚ РјРіРЅРѕРІРµРЅРЅРѕ РЅР° СЃРїР°РІРЅРµ
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
     * Р¦РІРµС‚ РёСЃРєСЂС‹ РїРѕ РµС‘ В«Р¶РёР·РЅРёВ» (0 = С‚РѕР»СЊРєРѕ СЂРѕРґРёР»Р°СЃСЊ, 1 = РіР°СЃРЅРµС‚):
     * СЏСЂРєРѕ-Р¶С‘Р»С‚Р°СЏ/Р±РµР»Р°СЏ -> РѕСЂР°РЅР¶РµРІР°СЏ -> С‚С‘РјРЅРѕ-РєСЂР°СЃРЅР°СЏ.
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

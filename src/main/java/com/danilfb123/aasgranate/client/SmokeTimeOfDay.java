package com.danilfb123.aasgranate.client;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * Затемнение дыма по времени суток. Днём ambient = 1.0, ночью около 0.38.
 * Вызывается один раз за кадр из SmokeStageRenderer.prepare().
 */
final class SmokeTimeOfDay {

    private static float ambient = 1.0F;
    private static float coolTint = 0.0F; // 0 - день, 1 - глубокая ночь

    private SmokeTimeOfDay() {}

    static void update(Level level, float partialTick) {
        // Высота солнца: 1.0 = полдень, 0 = горизонт, -1 = полночь.
        float sunAlt = level == null ? 1.0F : Mth.cos(level.getSunAngle(partialTick));
        float day = Mth.clamp(0.5F + sunAlt * 1.4F, 0.0F, 1.0F);
        day *= 1.0F - (level == null ? 0.0F : level.getRainLevel(partialTick)) * 0.25F;
        ambient  = Mth.lerp(day, 0.38F, 1.0F);
        coolTint = 1.0F - day;
    }

    /** Режим шейдерпака: пак сам освещает дым, наши множители не нужны. */
    static void neutral() {
        ambient = 1.0F;
        coolTint = 0.0F;
    }

    static float ambient() { return ambient; }

    static float coolTint() { return coolTint; }
}
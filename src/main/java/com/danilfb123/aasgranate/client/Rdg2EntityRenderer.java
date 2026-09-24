package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.Rdg2Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Rdg2EntityRenderer extends GeoEntityRenderer<Rdg2Entity> {

    public Rdg2EntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Rdg2EntityModel());
    }

    @Override
    public void render(Rdg2Entity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        // сама шашка
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        // Облако дыма и искры теперь рисуются отдельным проходом в SmokeStageRenderer
        // (RenderLevelStageEvent.AFTER_PARTICLES) — после частиц, чтобы дым их перекрывал.
    }

    @Override
    public boolean shouldRender(Rdg2Entity entity, Frustum frustum, double camX, double camY, double camZ) {
        if (super.shouldRender(entity, frustum, camX, camY, camZ)) return true;
        // хитбокс гранаты крошечный, а облако — 4 блока, поэтому проверяем расширенный бокс
        return entity.isSmoking() && frustum.isVisible(entity.getBoundingBoxForCulling());
    }
}

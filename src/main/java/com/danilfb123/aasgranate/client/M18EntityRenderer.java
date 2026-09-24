package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.M18Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class M18EntityRenderer extends GeoEntityRenderer<M18Entity> {

    public M18EntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new M18EntityModel());
    }

    @Override
    public void render(M18Entity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        // сама шашка (с throw/fly анимацией, в отличие от РДГ-2)
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        // Облако дыма и искры теперь рисуются отдельным проходом в SmokeStageRenderer
        // (RenderLevelStageEvent.AFTER_PARTICLES) — после частиц, чтобы дым их перекрывал.
    }

    @Override
    public boolean shouldRender(M18Entity entity, Frustum frustum, double camX, double camY, double camZ) {
        if (super.shouldRender(entity, frustum, camX, camY, camZ)) return true;
        // хитбокс гранаты крошечный, а облако — несколько блоков, поэтому проверяем расширенный бокс
        return entity.isSmoking() && frustum.isVisible(entity.getBoundingBoxForCulling());
    }
}

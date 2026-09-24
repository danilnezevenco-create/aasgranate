package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.ClientModEvents;
import com.danilfb123.aasgranate.RgoItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RgoItemRenderer extends GeoItemRenderer<RgoItem> {
    public RgoItemRenderer() {
        super(new RgoItemModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (transformType == ItemDisplayContext.GUI
                || transformType == ItemDisplayContext.GROUND
                || transformType == ItemDisplayContext.FIXED) {

            Minecraft mc = Minecraft.getInstance();
            BakedModel flatModel = mc.getModelManager().getModel(ClientModEvents.RGO_FLAT_MODEL);

            // 1. Отменяем трансформации 3D-модели (сбрасываем матрицу обратно)
            poseStack.popPose();

            // 2. Рендерим 2D-иконку (она сама применит ванильные GUI-настройки)
            mc.getItemRenderer().render(stack, transformType, false, poseStack, bufferSource,
                    packedLight, packedOverlay, flatModel);

            // 3. Возвращаем матрицу на место, чтобы не сломать ванильный код
            poseStack.pushPose();
            return;
        }
        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
    }
}

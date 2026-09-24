package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.ClientModEvents;
import com.danilfb123.aasgranate.M67Item;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class M67ItemRenderer extends GeoItemRenderer<M67Item> {
    public M67ItemRenderer() {
        super(new M67ItemModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (transformType == ItemDisplayContext.GUI
                || transformType == ItemDisplayContext.GROUND
                || transformType == ItemDisplayContext.FIXED) {

            Minecraft mc = Minecraft.getInstance();
            BakedModel flatModel = mc.getModelManager().getModel(ClientModEvents.M_67_FLAT_MODEL);

            // 1. Отменяем трансформации от 3D-модели (сбрасываем матрицу обратно)
            // Иначе модель сожмется дважды и улетит далеко за пределы слота инвентаря.
            poseStack.popPose();

            // 2. Рендерим 2D иконку (она сама применит свои ванильные GUI-настройки)
            mc.getItemRenderer().render(stack, transformType, false, poseStack, bufferSource,
                    packedLight, packedOverlay, flatModel);

            // 3. Возвращаем матрицу на место, чтобы не сломать ванильный код игры,
            // который ожидает, что сможет сделать popPose() после нашего рендера.
            poseStack.pushPose();
            return;
        }
        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
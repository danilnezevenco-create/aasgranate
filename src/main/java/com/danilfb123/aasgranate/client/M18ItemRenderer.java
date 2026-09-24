package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.ClientModEvents;
import com.danilfb123.aasgranate.M18Item;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class M18ItemRenderer extends GeoItemRenderer<M18Item> {
    public M18ItemRenderer() {
        super(new M18ItemModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (transformType == ItemDisplayContext.GUI
                || transformType == ItemDisplayContext.GROUND
                || transformType == ItemDisplayContext.FIXED) {

            Minecraft mc = Minecraft.getInstance();
            BakedModel flatModel = mc.getModelManager().getModel(ClientModEvents.M_18_FLAT_MODEL);

            poseStack.popPose();
            mc.getItemRenderer().render(stack, transformType, false, poseStack, bufferSource,
                    packedLight, packedOverlay, flatModel);
            poseStack.pushPose();
            return;
        }
        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
    }
}

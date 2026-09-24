package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.RgoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RgoEntityRenderer extends GeoEntityRenderer<RgoEntity> {
    public RgoEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RgoEntityModel());
    }
}

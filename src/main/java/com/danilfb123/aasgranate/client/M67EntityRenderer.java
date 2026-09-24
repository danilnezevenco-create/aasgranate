package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.M67Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class M67EntityRenderer extends GeoEntityRenderer<M67Entity> {
    public M67EntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new M67EntityModel());
    }
}
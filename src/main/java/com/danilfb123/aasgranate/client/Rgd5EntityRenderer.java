package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.Rgd5Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Rgd5EntityRenderer extends GeoEntityRenderer<Rgd5Entity> {
    public Rgd5EntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Rgd5EntityModel());
    }
}
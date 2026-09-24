package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.AasGranate;
import com.danilfb123.aasgranate.Rgd5Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Rgd5EntityModel extends GeoModel<Rgd5Entity> {
    @Override
    public ResourceLocation getModelResource(Rgd5Entity object) {
        return new ResourceLocation(AasGranate.MOD_ID, "geo/rgd_5.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Rgd5Entity object) {
        return new ResourceLocation(AasGranate.MOD_ID, "textures/item/rgd_5.png");
    }

    @Override
    public ResourceLocation getAnimationResource(Rgd5Entity animatable) {
        return new ResourceLocation(AasGranate.MOD_ID, "animations/rgd_5.animation.json");
    }
}
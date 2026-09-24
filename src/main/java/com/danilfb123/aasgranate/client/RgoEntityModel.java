package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.AasGranate;
import com.danilfb123.aasgranate.RgoEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RgoEntityModel extends GeoModel<RgoEntity> {
    @Override
    public ResourceLocation getModelResource(RgoEntity object) {
        return new ResourceLocation(AasGranate.MOD_ID, "geo/rgo.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RgoEntity object) {
        return new ResourceLocation(AasGranate.MOD_ID, "textures/item/rgo.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RgoEntity animatable) {
        return new ResourceLocation(AasGranate.MOD_ID, "animations/rgo.animation.json");
    }
}

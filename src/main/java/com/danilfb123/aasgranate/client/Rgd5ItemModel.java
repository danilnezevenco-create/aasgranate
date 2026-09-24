package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.AasGranate;
import com.danilfb123.aasgranate.Rgd5Item;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Rgd5ItemModel extends GeoModel<Rgd5Item> {
    @Override
    public ResourceLocation getModelResource(Rgd5Item object) {
        return new ResourceLocation(AasGranate.MOD_ID, "geo/rgd_5.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Rgd5Item object) {
        return new ResourceLocation(AasGranate.MOD_ID, "textures/item/rgd_5.png");
    }

    @Override
    public ResourceLocation getAnimationResource(Rgd5Item animatable) {
        return new ResourceLocation(AasGranate.MOD_ID, "animations/rgd_5.animation.json");
    }
}
package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.AasGranate;
import com.danilfb123.aasgranate.RgoItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RgoItemModel extends GeoModel<RgoItem> {
    @Override
    public ResourceLocation getModelResource(RgoItem object) {
        return new ResourceLocation(AasGranate.MOD_ID, "geo/rgo.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RgoItem object) {
        return new ResourceLocation(AasGranate.MOD_ID, "textures/item/rgo.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RgoItem animatable) {
        return new ResourceLocation(AasGranate.MOD_ID, "animations/rgo.animation.json");
    }
}

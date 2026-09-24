package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.AasGranate;
import com.danilfb123.aasgranate.M67Item;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class M67ItemModel extends GeoModel<M67Item> {
    @Override
    public ResourceLocation getModelResource(M67Item object) {
        return new ResourceLocation(AasGranate.MOD_ID, "geo/m_67.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(M67Item object) {
        return new ResourceLocation(AasGranate.MOD_ID, "textures/item/m_67.png");
    }

    @Override
    public ResourceLocation getAnimationResource(M67Item animatable) {
        return new ResourceLocation(AasGranate.MOD_ID, "animations/m_67.animation.json");
    }
}
package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.AasGranate;
import com.danilfb123.aasgranate.M18Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class M18EntityModel extends GeoModel<M18Entity> {
    @Override
    public ResourceLocation getModelResource(M18Entity object) {
        return new ResourceLocation(AasGranate.MOD_ID, "geo/m_18.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(M18Entity object) {
        return new ResourceLocation(AasGranate.MOD_ID, "textures/item/m_18.png");
    }

    @Override
    public ResourceLocation getAnimationResource(M18Entity animatable) {
        return new ResourceLocation(AasGranate.MOD_ID, "animations/m_18.animation.json");
    }
}

package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.AasGranate;
import com.danilfb123.aasgranate.M67Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class M67EntityModel extends GeoModel<M67Entity> {
    @Override
    public ResourceLocation getModelResource(M67Entity object) {
        return new ResourceLocation(AasGranate.MOD_ID, "geo/m_67.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(M67Entity object) {
        return new ResourceLocation(AasGranate.MOD_ID, "textures/item/m_67.png");
    }

    @Override
    public ResourceLocation getAnimationResource(M67Entity animatable) {
        return new ResourceLocation(AasGranate.MOD_ID, "animations/m_67.animation.json");
    }
}
package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.AasGranate;
import com.danilfb123.aasgranate.Rdg2Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Rdg2EntityModel extends GeoModel<Rdg2Entity> {
    @Override
    public ResourceLocation getModelResource(Rdg2Entity object) {
        return new ResourceLocation(AasGranate.MOD_ID, "geo/rdg_2.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Rdg2Entity object) {
        return new ResourceLocation(AasGranate.MOD_ID, "textures/item/rdg_2.png");
    }

    @Override
    public ResourceLocation getAnimationResource(Rdg2Entity animatable) {
        return new ResourceLocation(AasGranate.MOD_ID, "animations/rdg_2.animation.json");
    }
}

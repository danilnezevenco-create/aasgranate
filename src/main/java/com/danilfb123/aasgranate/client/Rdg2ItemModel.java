package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.AasGranate;
import com.danilfb123.aasgranate.Rdg2Item;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Rdg2ItemModel extends GeoModel<Rdg2Item> {
    @Override
    public ResourceLocation getModelResource(Rdg2Item object) {
        return new ResourceLocation(AasGranate.MOD_ID, "geo/rdg_2.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Rdg2Item object) {
        return new ResourceLocation(AasGranate.MOD_ID, "textures/item/rdg_2.png");
    }

    @Override
    public ResourceLocation getAnimationResource(Rdg2Item animatable) {
        return new ResourceLocation(AasGranate.MOD_ID, "animations/rdg_2.animation.json");
    }
}

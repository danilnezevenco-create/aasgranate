package com.danilfb123.aasgranate;

import com.danilfb123.aasgranate.client.M18EntityRenderer;
import com.danilfb123.aasgranate.client.M67EntityRenderer;
import com.danilfb123.aasgranate.client.Rdg2EntityRenderer;
import com.danilfb123.aasgranate.client.Rgd5EntityRenderer;
import com.danilfb123.aasgranate.client.RgoEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AasGranate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    public static final ResourceLocation RGD_5_FLAT_MODEL =
            new ResourceLocation(AasGranate.MOD_ID, "item/rgd_5_flat");

    public static final ResourceLocation M_67_FLAT_MODEL =
            new ResourceLocation(AasGranate.MOD_ID, "item/m_67_flat");

    public static final ResourceLocation RGO_FLAT_MODEL =
            new ResourceLocation(AasGranate.MOD_ID, "item/rgo_flat");

    public static final ResourceLocation RDG_2_FLAT_MODEL =
            new ResourceLocation(AasGranate.MOD_ID, "item/rdg_2_flat");

    public static final ResourceLocation M_18_FLAT_MODEL =
            new ResourceLocation(AasGranate.MOD_ID, "item/m_18_flat");

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RGD_5_PROJECTILE.get(), Rgd5EntityRenderer::new);
        event.registerEntityRenderer(ModEntities.M_67_PROJECTILE.get(), M67EntityRenderer::new);
        event.registerEntityRenderer(ModEntities.RGO_PROJECTILE.get(), RgoEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.RDG_2_PROJECTILE.get(), Rdg2EntityRenderer::new);
        event.registerEntityRenderer(ModEntities.M_18_PROJECTILE.get(), M18EntityRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        event.register(RGD_5_FLAT_MODEL);
        event.register(M_67_FLAT_MODEL);
        event.register(RGO_FLAT_MODEL);
        event.register(RDG_2_FLAT_MODEL);
        event.register(M_18_FLAT_MODEL);
    }
}

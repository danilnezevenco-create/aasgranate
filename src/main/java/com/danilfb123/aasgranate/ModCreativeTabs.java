package com.danilfb123.aasgranate;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AasGranate.MOD_ID);

    public static final RegistryObject<CreativeModeTab> AAS_TAB = CREATIVE_MODE_TABS.register("aas_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.RGD_5.get()))
                    .title(Component.translatable("creativetab.aas_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.RGD_5.get());
                        pOutput.accept(ModItems.M_67.get());
                        pOutput.accept(ModItems.RGO.get());
                        pOutput.accept(ModItems.RDG_2.get());
                        pOutput.accept(ModItems.M_18.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

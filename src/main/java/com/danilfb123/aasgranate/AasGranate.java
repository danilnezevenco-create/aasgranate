package com.danilfb123.aasgranate;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AasGranate.MOD_ID)
public class AasGranate {
    public static final String MOD_ID = "aasgranate";

    public AasGranate() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Тут мы будем регистрировать наши предметы, сущности и звуки
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        // Сетевой канал (передача состояния F3+B с клиента на сервер)
        ModNetworking.register();
    }
}

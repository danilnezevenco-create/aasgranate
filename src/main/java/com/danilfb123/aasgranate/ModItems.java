package com.danilfb123.aasgranate;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AasGranate.MOD_ID);

    public static final RegistryObject<Item> RGD_5 = ITEMS.register("rgd_5",
            () -> new Rgd5Item(new Item.Properties().stacksTo(16)));

    // M-67
    public static final RegistryObject<Item> M_67 = ITEMS.register("m_67",
            () -> new M67Item(new Item.Properties().stacksTo(16)));

    // Р Р“Рћ вЂ” СѓРґР°СЂРЅРѕ-РґРёСЃС‚Р°РЅС†РёРѕРЅРЅР°СЏ
    public static final RegistryObject<Item> RGO = ITEMS.register("rgo",
            () -> new RgoItem(new Item.Properties().stacksTo(16)));

    // Р Р”Р“-2 вЂ” РґС‹РјРѕРІР°СЏ
    public static final RegistryObject<Item> RDG_2 = ITEMS.register("rdg_2",
            () -> new Rdg2Item(new Item.Properties().stacksTo(16)));

    // M18 -- дымовая, аналог РДГ-2, но с анимацией броска/полёта
    public static final RegistryObject<Item> M_18 = ITEMS.register("m_18",
            () -> new M18Item(new Item.Properties().stacksTo(16)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

package com.danilfb123.aasgranate;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AasGranate.MOD_ID);

    public static final RegistryObject<EntityType<Rgd5Entity>> RGD_5_PROJECTILE =
            ENTITY_TYPES.register("rgd_5_projectile",
                    () -> EntityType.Builder.<Rgd5Entity>of(Rgd5Entity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("rgd_5_projectile"));

    public static final RegistryObject<EntityType<M67Entity>> M_67_PROJECTILE =
            ENTITY_TYPES.register("m_67_projectile",
                    () -> EntityType.Builder.<M67Entity>of(M67Entity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("m_67_projectile"));

    // Р Р“Рћ
    public static final RegistryObject<EntityType<RgoEntity>> RGO_PROJECTILE =
            ENTITY_TYPES.register("rgo_projectile",
                    () -> EntityType.Builder.<RgoEntity>of(RgoEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("rgo_projectile"));

    // Р Р”Р“-2. Р”Р°Р»СЊРЅРѕСЃС‚СЊ С‚СЂРµРєРёРЅРіР° Р±РѕР»СЊС€Рµ: РѕР±Р»Р°РєРѕ РІРёРґРЅРѕ РёР·РґР°Р»РµРєР°,
    // Р° РѕР±РЅРѕРІР»РµРЅРёСЏ РїРѕР·РёС†РёРё С‡Р°С‰Рµ, С‡С‚РѕР±С‹ РґС‹Рј РЅРµ В«С‚РµР»РµРїРѕСЂС‚РёСЂРѕРІР°Р»СЃСЏВ» РїСЂРё РєР°С‡РµРЅРёРё.
    public static final RegistryObject<EntityType<Rdg2Entity>> RDG_2_PROJECTILE =
            ENTITY_TYPES.register("rdg_2_projectile",
                    () -> EntityType.Builder.<Rdg2Entity>of(Rdg2Entity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build("rdg_2_projectile"));

    // M18. РЎРІРѕР№СЃС‚РІР° С‚СЂРµРєРёРЅРіР° С‚Р°РєРёРµ Р¶Рµ, РєР°Рє Сѓ Р Р”Р“-2 вЂ” СЌС‚Рѕ С‚РѕР¶Рµ РґС‹РјРѕРІР°СЏ С€Р°С€РєР°
    // СЃ РѕР±Р»Р°РєРѕРј, РІРёРґРёРјС‹Рј РёР·РґР°Р»РµРєР°.
    public static final RegistryObject<EntityType<M18Entity>> M_18_PROJECTILE =
            ENTITY_TYPES.register("m_18_projectile",
                    () -> EntityType.Builder.<M18Entity>of(M18Entity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build("m_18_projectile"));

    public static final RegistryObject<EntityType<AasSmokeSourceEntity>> AAS_SMOKE_SOURCE =
            ENTITY_TYPES.register("aas_smoke_source",
                    () -> EntityType.Builder.<AasSmokeSourceEntity>of(AasSmokeSourceEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(8)
                            .updateInterval(10)
                            .fireImmune()
                            .noSave()
                            .build("aas_smoke_source"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}

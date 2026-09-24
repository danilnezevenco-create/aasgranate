package com.danilfb123.aasgranate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AasGranate.MOD_ID);

    // --- RGD-5 ---
    public static final RegistryObject<SoundEvent> RGD5_PIN_PULL = registerSoundEvent("rgd5_pin_pull");
    public static final RegistryObject<SoundEvent> RGD5_THROW    = registerSoundEvent("rgd5_throw");
    public static final RegistryObject<SoundEvent> RGD5_BLAST_CLOSE   = registerSoundEvent("rgd5_blast_close");
    public static final RegistryObject<SoundEvent> RGD5_BLAST_MID     = registerSoundEvent("rgd5_blast_mid");
    public static final RegistryObject<SoundEvent> RGD5_BLAST_FAR     = registerSoundEvent("rgd5_blast_far");
    public static final RegistryObject<SoundEvent> RGD5_BLAST_DISTANT = registerSoundEvent("rgd5_blast_distant");

    // --- M-67 ---
    public static final RegistryObject<SoundEvent> M67_PIN_PULL = registerSoundEvent("m_67_pin_pull");
    public static final RegistryObject<SoundEvent> M67_THROW    = registerSoundEvent("m_67_throw");
    public static final RegistryObject<SoundEvent> M67_BLAST_CLOSE   = registerSoundEvent("m_67_blast_close");
    public static final RegistryObject<SoundEvent> M67_BLAST_MID     = registerSoundEvent("m_67_blast_mid");
    public static final RegistryObject<SoundEvent> M67_BLAST_FAR     = registerSoundEvent("m_67_blast_far");
    public static final RegistryObject<SoundEvent> M67_BLAST_DISTANT = registerSoundEvent("m_67_blast_distant");

    // --- Р Р“Рћ ---
    public static final RegistryObject<SoundEvent> RGO_PIN_PULL = registerSoundEvent("rgo_pin_pull");
    public static final RegistryObject<SoundEvent> RGO_THROW    = registerSoundEvent("rgo_throw");
    public static final RegistryObject<SoundEvent> RGO_BLAST_CLOSE   = registerSoundEvent("rgo_blast_close");
    public static final RegistryObject<SoundEvent> RGO_BLAST_MID     = registerSoundEvent("rgo_blast_mid");
    public static final RegistryObject<SoundEvent> RGO_BLAST_FAR     = registerSoundEvent("rgo_blast_far");
    public static final RegistryObject<SoundEvent> RGO_BLAST_DISTANT = registerSoundEvent("rgo_blast_distant");

    // --- Р Р”Р“-2 (РґС‹РјРѕРІР°СЏ): РІР·СЂС‹РІР° РЅРµС‚, РІРјРµСЃС‚Рѕ РЅРµРіРѕ Р·Р°С†РёРєР»РµРЅРЅС‹Р№ Р·РІСѓРє РіРѕСЂРµРЅРёСЏ С€Р°С€РєРё ---
    public static final RegistryObject<SoundEvent> RDG2_PIN_PULL   = registerSoundEvent("rdg2_pin_pull");
    public static final RegistryObject<SoundEvent> RDG2_THROW      = registerSoundEvent("rdg2_throw");
    public static final RegistryObject<SoundEvent> RDG2_SMOKE_LOOP = registerSoundEvent("rdg2_smoke_loop");

    // --- M18 (дымовая, аналог РДГ-2) ---
    public static final RegistryObject<SoundEvent> M18_PIN_PULL   = registerSoundEvent("m18_pin_pull");
    public static final RegistryObject<SoundEvent> M18_THROW      = registerSoundEvent("m18_throw");
    public static final RegistryObject<SoundEvent> M18_SMOKE_LOOP = registerSoundEvent("m18_smoke_loop");

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(
                        new ResourceLocation(AasGranate.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}

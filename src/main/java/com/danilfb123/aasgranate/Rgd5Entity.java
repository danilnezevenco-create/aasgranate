package com.danilfb123.aasgranate;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.Random;

import static com.danilfb123.aasgranate.GranadeSoundUtils.playBlastSoundByDistance;

public class Rgd5Entity extends ThrowableProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int fuseTimer;

    private static final RawAnimation ANIM_THROW =
            RawAnimation.begin().thenPlayAndHold("animation.grenade.throw");

    private static final RawAnimation ANIM_FLY =
            RawAnimation.begin().thenLoop("animation.grenade.fly");

    // Р С™Р С•Р В»Р С‘РЎвЂЎР ВµРЎРѓРЎвЂљР Р†Р С• Р С•РЎРѓР С”Р С•Р В»Р С”Р С•Р Р† РЎС“Р Р†Р ВµР В»Р С‘РЎвЂЎР ВµР Р…Р С• Р Р† 2 РЎР‚Р В°Р В·Р В° (Р В±РЎвЂ№Р В»Р С• 420, РЎРѓРЎвЂљР В°Р В»Р С• 840).
    private static final int    SHRAPNEL_COUNT  = 840;
    // Р вЂќР В°Р В»РЎРЉР Р…Р С•РЎРѓРЎвЂљРЎРЉ Р В»РЎС“РЎвЂЎР ВµР в„– (Р В±Р ВµР В· Р С‘Р В·Р СР ВµР Р…Р ВµР Р…Р С‘Р в„–: 14-36 Р В±Р В»Р С•Р С”Р С•Р Р†).
    private static final double MAX_DIST_MIN    = 14.0;
    private static final double MAX_DIST_MAX    = 36.0;
    // Р вЂР В°Р В·Р С•Р Р†РЎвЂ№Р в„– РЎС“РЎР‚Р С•Р Р… Р С•РЎРѓР С”Р С•Р В»Р С”Р В° Р Р…Р В° Р Р…РЎС“Р В»Р ВµР Р†Р С•Р в„– Р Т‘Р С‘РЎРѓРЎвЂљР В°Р Р…РЎвЂ Р С‘Р С‘. Р В Р ВµР В°Р В»РЎРЉР Р…РЎвЂ№Р в„– РЎС“РЎР‚Р С•Р Р… Р С—РЎР‚Р С‘ Р С—Р С•Р С—Р В°Р Т‘Р В°Р Р…Р С‘Р С‘
    // Р В»Р С‘Р Р…Р ВµР в„–Р Р…Р С• Р С—Р В°Р Т‘Р В°Р ВµРЎвЂљ Р Т‘Р С• 0 Р С” Р Т‘Р С‘РЎРѓРЎвЂљР В°Р Р…РЎвЂ Р С‘Р С‘ SHRAPNEL_FALLOFF_DIST (РЎРѓР С. РЎР‚Р В°РЎРѓРЎвЂЎРЎвЂРЎвЂљ Р Р…Р С‘Р В¶Р Вµ) РІР‚вЂќ
    // РЎвЂЎР ВµР С Р Т‘Р В°Р В»РЎРЉРЎв‚¬Р Вµ РЎвЂ Р ВµР В»РЎРЉ Р С•РЎвЂљ Р С–РЎР‚Р В°Р Р…Р В°РЎвЂљРЎвЂ№, РЎвЂљР ВµР С Р СР ВµР Р…РЎРЉРЎв‚¬Р Вµ РЎС“РЎР‚Р С•Р Р…Р В° Р С•Р Р…Р В° Р С—Р С•Р В»РЎС“РЎвЂЎР В°Р ВµРЎвЂљ Р С•РЎвЂљ Р С”Р С•Р Р…Р С”РЎР‚Р ВµРЎвЂљР Р…Р С•Р С–Р С•
    // Р С•РЎРѓР С”Р С•Р В»Р С”Р В°.
    private static final float  SHRAPNEL_DAMAGE = 32.0F;
    // Р СњР В° Р С”Р В°Р С”Р С•Р в„– Р Т‘Р С‘РЎРѓРЎвЂљР В°Р Р…РЎвЂ Р С‘Р С‘ РЎС“РЎР‚Р С•Р Р… Р С•РЎРѓР С”Р С•Р В»Р С”Р В° Р С—Р С•Р В»Р Р…Р С•РЎРѓРЎвЂљРЎРЉРЎР‹ Р С•Р В±Р Р…РЎС“Р В»РЎРЏР ВµРЎвЂљРЎРѓРЎРЏ. Р вЂ™Р В·РЎРЏРЎвЂљР С• РЎР‚Р В°Р Р†Р Р…РЎвЂ№Р С
    // Р СР В°Р С”РЎРѓР С‘Р СР В°Р В»РЎРЉР Р…Р С•Р в„– Р Т‘Р В°Р В»РЎРЉР Р…Р С•РЎРѓРЎвЂљР С‘ Р В»РЎС“РЎвЂЎР В°, РЎвЂљР В°Р С” РЎвЂЎРЎвЂљР С• Р С—Р В°Р Т‘Р ВµР Р…Р С‘Р Вµ РЎС“РЎР‚Р С•Р Р…Р В° РЎР‚Р В°РЎРѓРЎвЂљРЎРЏР Р…РЎС“РЎвЂљР С• Р Р…Р В° Р Р†Р ВµРЎРѓРЎРЉ
    // Р Р†Р С•Р В·Р СР С•Р В¶Р Р…РЎвЂ№Р в„– Р С—РЎС“РЎвЂљРЎРЉ Р С•РЎРѓР С”Р С•Р В»Р С”Р В°.
    private static final double SHRAPNEL_FALLOFF_DIST = MAX_DIST_MAX;
    // Р СњР С‘Р В¶Р Р…Р С‘Р в„– Р С—Р С•РЎР‚Р С•Р С– РЎС“РЎР‚Р С•Р Р…Р В° Р Р† Р Т‘Р С•Р В»РЎРЏРЎвЂ¦ Р С•РЎвЂљ Р В±Р В°Р В·Р С•Р Р†Р С•Р С–Р С• (0.0 = РЎС“ РЎРѓР В°Р СР С•Р С–Р С• Р С”РЎР‚Р В°РЎРЏ Р Т‘Р В°Р В»РЎРЉР Р…Р С•РЎРѓРЎвЂљР С‘
    // РЎС“РЎР‚Р С•Р Р… Р С—Р В°Р Т‘Р В°Р ВµРЎвЂљ Р Т‘Р С• Р Р…РЎС“Р В»РЎРЏ; Р С—Р С•РЎРѓРЎвЂљР В°Р Р†РЎРЉ, Р Р…Р В°Р С—РЎР‚Р С‘Р СР ВµРЎР‚, 0.1, Р ВµРЎРѓР В»Р С‘ РЎвЂ¦Р С•РЎвЂЎР ВµРЎв‚¬РЎРЉ, РЎвЂЎРЎвЂљР С•Р В±РЎвЂ№ Р Т‘Р В°Р В¶Р Вµ Р Р…Р В°
    // Р СР В°Р С”РЎРѓР С‘Р СР В°Р В»РЎРЉР Р…Р С•Р в„– Р Т‘Р С‘РЎРѓРЎвЂљР В°Р Р…РЎвЂ Р С‘Р С‘ Р С•РЎРѓРЎвЂљР В°Р Р†Р В°Р В»РЎРѓРЎРЏ Р Р…Р ВµР В±Р С•Р В»РЎРЉРЎв‚¬Р С•Р в„– "Р Т‘Р С•Р Р†Р ВµРЎРѓР С•Р С”" РЎС“РЎР‚Р С•Р Р…Р В°).
    private static final float  SHRAPNEL_MIN_DAMAGE_FRACTION = 0.0F;

    // РІвЂќР‚РІвЂќР‚ Р вЂњР В°РЎР‚Р В°Р Р…РЎвЂљР С‘РЎР‚Р С•Р Р†Р В°Р Р…Р Р…Р С•Р Вµ РЎС“Р В±Р С‘Р в„–РЎРѓРЎвЂљР Р†Р С• Р Р† РЎС“Р С—Р С•РЎР‚ РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚
    // Р СџРЎР‚Р С•Р В±Р В»Р ВµР СР В° Р В±РЎвЂ№Р В»Р В° Р Р…Р Вµ Р Р† Р Р…Р В°Р С—РЎР‚Р В°Р Р†Р В»Р ВµР Р…Р С‘Р С‘ Р В»РЎС“РЎвЂЎР ВµР в„– (РЎРѓРЎвЂћР ВµРЎР‚Р В° РЎРѓРЎРЊР СР С—Р В»Р С‘РЎР‚РЎС“Р ВµРЎвЂљРЎРѓРЎРЏ РЎвЂЎР ВµРЎРѓРЎвЂљР Р…Р С•), Р В° Р Р† РЎвЂљР С•Р С,
    // РЎвЂЎРЎвЂљР С• Р С—РЎР‚Р С‘ Р В±Р С•Р В»РЎРЉРЎв‚¬Р С•Р С Р С”Р С•Р В»Р С‘РЎвЂЎР ВµРЎРѓРЎвЂљР Р†Р Вµ Р В»РЎС“РЎвЂЎР ВµР в„– Р Р…Р В° Р Р†РЎРѓРЎР‹ РЎРѓРЎвЂћР ВµРЎР‚РЎС“ РЎвЂљР ВµР В»Р ВµРЎРѓР Р…РЎвЂ№Р в„– РЎС“Р С–Р С•Р В», Р С”Р С•РЎвЂљР С•РЎР‚РЎвЂ№Р в„–
    // Р В·Р В°Р Р…Р С‘Р СР В°Р ВµРЎвЂљ РЎвЂ¦Р С‘РЎвЂљР В±Р С•Р С”РЎРѓ Р С‘Р С–РЎР‚Р С•Р С”Р В° Р Р† 2 Р В±Р В»Р С•Р С”Р В°РЎвЂ¦ Р С•РЎвЂљ Р С–РЎР‚Р В°Р Р…Р В°РЎвЂљРЎвЂ№, Р С•РЎвЂЎР ВµР Р…РЎРЉ Р СР В°Р В» РІР‚вЂќ Р С‘ Р Р† Р В·Р В°Р СР ВµРЎвЂљР Р…Р С•Р в„–
    // Р Т‘Р С•Р В»Р Вµ РЎРѓР В»РЎС“РЎвЂЎР В°Р ВµР Р† Р Р…Р С‘ Р С•Р Т‘Р С‘Р Р… Р В»РЎС“РЎвЂЎ РЎвЂћР С‘Р В·Р С‘РЎвЂЎР ВµРЎРѓР С”Р С‘ Р Р…Р Вµ Р В·Р В°Р Т‘Р ВµР Р†Р В°Р ВµРЎвЂљ Р С‘Р С–РЎР‚Р С•Р С”Р В°. Р С›РЎвЂљРЎРѓРЎР‹Р Т‘Р В° Р С‘ "Р Р†РЎвЂ№Р В¶Р С‘Р В»,
    // РЎРѓРЎвЂљР С•РЎРЏ Р С—РЎР‚РЎРЏР СР С• Р Р…Р В°Р Т‘ Р С–РЎР‚Р В°Р Р…Р В°РЎвЂљР С•Р в„–". Р В­РЎвЂљР С• Р Р…Р Вµ Р В±Р В°Р С– direction'Р В°, Р В° РЎвЂЎР С‘РЎРѓРЎвЂљР В°РЎРЏ Р Р†Р ВµРЎР‚Р С•РЎРЏРЎвЂљР Р…Р С•РЎРѓРЎвЂљРЎРЉ,
    // Р С—Р С•РЎРЊРЎвЂљР С•Р СРЎС“ Р ВµРЎРѓРЎвЂљРЎРЉ Р С•РЎвЂљР Т‘Р ВµР В»РЎРЉР Р…Р В°РЎРЏ Р Т‘Р ВµРЎвЂљР ВµРЎР‚Р СР С‘Р Р…Р С‘РЎР‚Р С•Р Р†Р В°Р Р…Р Р…Р В°РЎРЏ Р С—РЎР‚Р С•Р Р†Р ВµРЎР‚Р С”Р В° Р Р…Р С‘Р В¶Р Вµ.
    //
    // Р вЂР В°Р С– Р В±РЎвЂ№Р В» Р Р† РЎвЂљР С•Р С, Р В§Р СћР С› Р С‘Р СР ВµР Р…Р Р…Р С• Р СР ВµРЎР‚Р С‘Р В»Р С•РЎРѓРЎРЉ: Р Т‘Р С‘РЎРѓРЎвЂљР В°Р Р…РЎвЂ Р С‘РЎРЏ Р Т‘Р С• РЎвЂ Р ВµР В»Р С‘ РЎРѓРЎвЂЎР С‘РЎвЂљР В°Р В»Р В°РЎРѓРЎРЉ Р Т‘Р С•
    // Р В¦Р вЂўР СњР СћР В Р С’ РЎвЂ¦Р С‘РЎвЂљР В±Р С•Р С”РЎРѓР В°, Р В° Р Р…Р Вµ Р Т‘Р С• Р ВµР С–Р С• Р В±Р В»Р С‘Р В¶Р В°Р в„–РЎв‚¬Р ВµР в„– РЎвЂљР С•РЎвЂЎР С”Р С‘. Р ВР С–РЎР‚Р С•Р С” РЎРѓРЎвЂљР С•Р С‘РЎвЂљ Р Р† Р С—Р С•Р В»Р Р…РЎвЂ№Р в„– РЎР‚Р С•РЎРѓРЎвЂљ
    // (~1.8 Р В±Р В»Р С•Р С”Р В°), Р ВµР С–Р С• РЎвЂ Р ВµР Р…РЎвЂљРЎР‚ РІР‚вЂќ Р С—РЎР‚Р С‘Р СР ВµРЎР‚Р Р…Р С• Р Р…Р В° 0.9 Р В±Р В»Р С•Р С”Р В° Р Р†РЎвЂ№РЎв‚¬Р Вµ Р В·Р ВµР СР В»Р С‘, Р В° Р С–РЎР‚Р В°Р Р…Р В°РЎвЂљР В°
    // Р В»Р ВµР В¶Р С‘РЎвЂљ РЎС“ РЎРѓР В°Р СР С•Р в„– Р В·Р ВµР СР В»Р С‘ (+0.1). Р СџРЎР‚Р С‘ Р С–Р С•РЎР‚Р С‘Р В·Р С•Р Р…РЎвЂљР В°Р В»РЎРЉР Р…Р С•Р С РЎР‚Р В°РЎРѓРЎРѓРЎвЂљР С•РЎРЏР Р…Р С‘Р С‘ РЎР‚Р С•Р Р†Р Р…Р С• "2 Р В±Р В»Р С•Р С”Р В°"
    // РЎР‚Р ВµР В°Р В»РЎРЉР Р…Р В°РЎРЏ 3D-Р Т‘Р С‘РЎРѓРЎвЂљР В°Р Р…РЎвЂ Р С‘РЎРЏ Р Т‘Р С• Р В¦Р вЂўР СњР СћР В Р С’ Р С‘Р С–РЎР‚Р С•Р С”Р В° Р С—Р С•Р В»РЎС“РЎвЂЎР В°Р В»Р В°РЎРѓРЎРЉ
    // РІв‚¬С™(2.0Р’Р† + 0.8Р’Р†) РІвЂ°в‚¬ 2.15 Р В±Р В»Р С•Р С”Р В° РІР‚вЂќ РЎвЂљР С• Р ВµРЎРѓРЎвЂљРЎРЉ РЎвЂЎРЎС“РЎвЂљРЎРЉ Р В±Р С•Р В»РЎРЉРЎв‚¬Р Вµ LETHAL_RADIUS, Р С‘ Р С‘Р С–РЎР‚Р С•Р С”
    // Р Р†РЎвЂ№Р С—Р В°Р Т‘Р В°Р В» Р С‘Р В· Р С–Р В°РЎР‚Р В°Р Р…РЎвЂљР С‘РЎР‚Р С•Р Р†Р В°Р Р…Р Р…Р С•Р в„– Р В·Р С•Р Р…РЎвЂ№ РЎРѓР СР ВµРЎР‚РЎвЂљР С‘ Р С—РЎР‚РЎРЏР СР С• Р Р…Р В° Р С–РЎР‚Р В°Р Р…Р С‘РЎвЂ Р Вµ, Р С—Р С•Р С—Р В°Р Т‘Р В°РЎРЏ Р Р†
    // Р С•Р В±РЎвЂ№РЎвЂЎР Р…РЎС“РЎР‹ Р В·Р С•Р Р…РЎС“ Р С—Р В°Р Т‘Р ВµР Р…Р С‘РЎРЏ РЎС“РЎР‚Р С•Р Р…Р В° Р С•РЎРѓР С”Р С•Р В»Р С”Р С•Р Р† (РЎвЂљР В°Р С РЎС“Р В¶Р Вµ Р В±РЎР‚Р С•Р Р…РЎРЏ Р В»Р ВµР С–Р С”Р С• РЎРѓР С—Р В°РЎРѓР В°Р ВµРЎвЂљ).
    //
    // Р ВРЎРѓР С—РЎР‚Р В°Р Р†Р В»Р ВµР Р…Р С•: Р Т‘Р С‘РЎРѓРЎвЂљР В°Р Р…РЎвЂ Р С‘РЎРЏ РЎвЂљР ВµР С—Р ВµРЎР‚РЎРЉ РЎРѓРЎвЂЎР С‘РЎвЂљР В°Р ВµРЎвЂљРЎРѓРЎРЏ Р Т‘Р С• Р вЂР вЂєР ВР вЂ“Р С’Р в„ўР РЃР вЂўР в„ў РЎвЂљР С•РЎвЂЎР С”Р С‘ РЎвЂ¦Р С‘РЎвЂљР В±Р С•Р С”РЎРѓР В°
    // (РЎвЂЎР ВµРЎР‚Р ВµР В· AABB.distanceToSqr), Р В° Р Р…Р Вµ Р Т‘Р С• РЎвЂ Р ВµР Р…РЎвЂљРЎР‚Р В°. Р СћР В°Р С”Р В¶Р Вµ РЎРЊРЎвЂљР В° Р С—РЎР‚Р С•Р Р†Р ВµРЎР‚Р С”Р В° РЎвЂљР ВµР С—Р ВµРЎР‚РЎРЉ
    // Р Р†РЎвЂ№Р С—Р С•Р В»Р Р…РЎРЏР ВµРЎвЂљРЎРѓРЎРЏ РЎР‚Р В°Р Р…РЎРЉРЎв‚¬Р Вµ РЎРѓР В»РЎС“РЎвЂЎР В°Р в„–Р Р…РЎвЂ№РЎвЂ¦ Р С•РЎРѓР С”Р С•Р В»Р С”Р С•Р Р†, РЎвЂЎРЎвЂљР С•Р В±РЎвЂ№ Р Р…Р В° Р Р…Р ВµРЎвЂ Р Р…Р С‘РЎвЂЎР ВµР С–Р С• Р Р…Р Вµ Р Р†Р В»Р С‘РЎРЏР В»Р С•
    // Р С—Р С• Р С”Р В°РЎРѓР В°РЎвЂљР ВµР В»РЎРЉР Р…Р С•Р в„– (Р Р…Р В°Р С—РЎР‚Р С‘Р СР ВµРЎР‚, invulnerability-РЎвЂљР С‘Р С” Р С•РЎвЂљ РЎС“РЎР‚Р С•Р Р…Р В° Р С•РЎРѓР С”Р С•Р В»Р С”Р С•Р Р†).
    private static final double LETHAL_RADIUS = 2.0;
    private static final float  LETHAL_DAMAGE = 1000.0F; // Р В·Р В°Р Р†Р ВµР Т‘Р С•Р СР С• Р В±Р С•Р В»РЎРЉРЎв‚¬Р Вµ Р В»РЎР‹Р В±Р С•Р С–Р С• РЎР‚Р ВµР В°Р В»Р С‘РЎРѓРЎвЂљР С‘РЎвЂЎР Р…Р С•Р С–Р С• Р В·Р В°Р С—Р В°РЎРѓР В° HP/Р В±РЎР‚Р С•Р Р…Р С‘

    private static final int THROW_ANIM_TICKS = 15;
    private int ticksAlive = 0;

    // РІвЂќР‚РІвЂќР‚ Р В¤Р С‘Р В·Р С‘Р С”Р В° Р С—РЎР‚Р С‘Р В·Р ВµР СР В»Р ВµР Р…Р С‘РЎРЏ (РЎР‚Р ВµР В°Р В»Р С‘РЎРѓРЎвЂљР С‘РЎвЂЎР Р…Р В°РЎРЏ, Р В±Р ВµР В· Р С‘РЎРѓР С”РЎС“РЎРѓРЎРѓРЎвЂљР Р†Р ВµР Р…Р Р…Р С•Р С–Р С• Р В»Р С‘Р СР С‘РЎвЂљР В° Р С•РЎвЂљРЎРѓР С”Р С•Р С”Р С•Р Р†) РІвЂќР‚РІвЂќР‚
    // Р вЂњРЎР‚Р В°Р Р…Р В°РЎвЂљР В° РЎвЂљР ВµРЎР‚РЎРЏР ВµРЎвЂљ РЎРѓР С”Р С•РЎР‚Р С•РЎРѓРЎвЂљРЎРЉ Р РЋР С’Р СљР С’ Р В·Р В° РЎРѓРЎвЂЎРЎвЂРЎвЂљ РЎвЂљР С•Р С–Р С•, РЎвЂЎРЎвЂљР С• Р Р†Р С•РЎРѓРЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р В»Р ВµР Р…Р С‘Р Вµ РЎРЊР Р…Р ВµРЎР‚Р С–Р С‘Р С‘
    // РЎРѓР В»Р В°Р В±Р ВµР ВµРЎвЂљ РЎРѓ Р С”Р В°Р В¶Р Т‘РЎвЂ№Р С РЎРѓР В»Р ВµР Т‘РЎС“РЎР‹РЎвЂ°Р С‘Р С РЎС“Р Т‘Р В°РЎР‚Р С•Р С (RESTITUTION_DECAY) РІР‚вЂќ Р С”Р В°Р С” Р Р…Р В°РЎРѓРЎвЂљР С•РЎРЏРЎвЂ°Р В°РЎРЏ
    // Р СР Р…РЎС“РЎвЂ°Р В°РЎРЏРЎРѓРЎРЏ Р В¶Р ВµР В»Р ВµР В·Р С”Р В°, Р В° Р Р…Р Вµ РЎР‚Р ВµР В·Р С‘Р Р…Р С•Р Р†РЎвЂ№Р в„– Р СРЎРЏРЎвЂЎР С‘Р С”. Р С›РЎвЂљР Т‘Р ВµР В»РЎРЉР Р…Р С•Р С–Р С• "Р С—Р С•РЎРѓР В»Р Вµ 2 РЎС“Р Т‘Р В°РЎР‚Р С•Р Р†
    // Р В·Р В°Р СР С•РЎР‚Р С•Р В·Р С‘РЎвЂљРЎРЉ" Р В±Р С•Р В»РЎРЉРЎв‚¬Р Вµ Р Р…Р ВµРЎвЂљ: Р С•Р Р…Р В° Р С•РЎРѓРЎвЂљР В°Р Р…Р В°Р Р†Р В»Р С‘Р Р†Р В°Р ВµРЎвЂљРЎРѓРЎРЏ РЎР‚Р С•Р Р†Р Р…Р С• РЎвЂљР С•Р С–Р Т‘Р В°, Р С”Р С•Р С–Р Т‘Р В° РЎРѓР С”Р С•РЎР‚Р С•РЎРѓРЎвЂљРЎРЉ
    // Р ВµРЎРѓРЎвЂљР ВµРЎРѓРЎвЂљР Р†Р ВµР Р…Р Р…РЎвЂ№Р С Р С•Р В±РЎР‚Р В°Р В·Р С•Р С Р С—Р В°Р Т‘Р В°Р ВµРЎвЂљ Р Р…Р С‘Р В¶Р Вµ SETTLE_SPEED_SQ.
    private static final double BASE_RESTITUTION        = 0.21;  // уменьшено в 2 раза (было 0.42) — гранаты отскакивают вдвое слабее
    private static final double RESTITUTION_DECAY       = 0.80;  // Р Р†Р С• РЎРѓР С”Р С•Р В»РЎРЉР С”Р С• РЎР‚Р В°Р В· РЎРѓР В»Р В°Р В±Р ВµР Вµ Р С”Р В°Р В¶Р Т‘РЎвЂ№Р в„– РЎРѓР В»Р ВµР Т‘РЎС“РЎР‹РЎвЂ°Р С‘Р в„– Р С•РЎвЂљРЎРѓР С”Р С•Р С”
    private static final double MIN_RESTITUTION         = 0.10;  // Р Р…Р С‘Р В¶Р Вµ РЎРЊРЎвЂљР С•Р С–Р С• Р Р†Р С•РЎРѓРЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р В»Р ВµР Р…Р С‘Р Вµ Р Р…Р Вµ Р С—Р В°Р Т‘Р В°Р ВµРЎвЂљ (Р С‘Р Р…Р В°РЎвЂЎР Вµ Р В±РЎС“Р Т‘Р ВµРЎвЂљ "Р С—РЎР‚Р С‘Р В»Р С‘Р С—Р В°РЎвЂљРЎРЉ" Р Р†Р С‘Р В·РЎС“Р В°Р В»РЎРЉР Р…Р С•)
    private static final double BOUNCE_TANGENT_FRICTION = 0.55;  // Р С–Р В°РЎв‚¬Р ВµР Р…Р С‘Р Вµ РЎРѓР С”Р С•РЎР‚Р С•РЎРѓРЎвЂљР С‘ Р С—Р С• Р С”Р В°РЎРѓР В°РЎвЂљР ВµР В»РЎРЉР Р…РЎвЂ№Р С Р С•РЎРѓРЎРЏР С Р С—РЎР‚Р С‘ РЎС“Р Т‘Р В°РЎР‚Р Вµ
    private static final double SETTLE_SPEED_SQ         = 0.03 * 0.03; // Р Р…Р С‘Р В¶Р Вµ РЎРЊРЎвЂљР С•Р в„– РЎРѓР С”Р С•РЎР‚Р С•РЎРѓРЎвЂљР С‘ Р С–РЎР‚Р В°Р Р…Р В°РЎвЂљР В° Р С•РЎРѓРЎвЂљР В°Р Р…Р В°Р Р†Р В»Р С‘Р Р†Р В°Р ВµРЎвЂљРЎРѓРЎРЏ РЎРѓР В°Р СР В°

    // Небольшой отступ от поверхности блока сразу после отскока, чтобы граната
    // не оставалась ровно на границе блока и не "проваливалась" внутрь него
    // на следующем тике при повторном отскоке (особенно у углов/стыков блоков).
    private static final double BOUNCE_PUSH_OUT = 0.06;

    // РІвЂќР‚РІвЂќР‚ Р С™РЎС“Р Р†РЎвЂ№РЎР‚Р С”Р В°Р Р…Р С‘Р Вµ (РЎвЂљРЎС“Р СР В±Р В»Р С‘Р Р…Р С–) Р Р† Р С—Р С•Р В»РЎвЂРЎвЂљР Вµ, Р В°-Р В»РЎРЏ Squad РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚
    // Р вЂњРЎР‚Р В°Р Р…Р В°РЎвЂљР В° Р С”РЎР‚РЎС“РЎвЂљР С‘РЎвЂљРЎРѓРЎРЏ Р С—Р С• РЎвЂљР В°Р Р…Р С–Р В°Р В¶РЎС“ Р С‘ РЎР‚РЎвЂ№РЎРѓР С”Р В°Р Р…РЎРЉРЎР‹ РЎРѓР В»РЎС“РЎвЂЎР В°Р в„–Р Р…РЎвЂ№Р С Р С•Р В±РЎР‚Р В°Р В·Р С•Р С, Р В° Р Р…Р Вµ Р В»Р ВµРЎвЂљР С‘РЎвЂљ
    // "РЎР‚Р С•Р Р†Р Р…Р С• Р С”Р В°Р С” РЎРѓРЎвЂљРЎР‚Р ВµР В»Р В°". Р СџРЎР‚Р С‘ Р С”Р В°Р В¶Р Т‘Р С•Р С РЎС“Р Т‘Р В°РЎР‚Р Вµ Р С• Р С—Р С•Р Р†Р ВµРЎР‚РЎвЂ¦Р Р…Р С•РЎРѓРЎвЂљРЎРЉ Р С—Р С•Р В»РЎС“РЎвЂЎР В°Р ВµРЎвЂљ РЎРѓР В»РЎС“РЎвЂЎР В°Р в„–Р Р…РЎвЂ№Р в„–
    // Р С‘Р СР С—РЎС“Р В»РЎРЉРЎРѓ Р Р†РЎР‚Р В°РЎвЂ°Р ВµР Р…Р С‘РЎРЏ, Р С—Р С•РЎРЊРЎвЂљР С•Р СРЎС“ Р СР С•Р В¶Р ВµРЎвЂљ РЎС“Р В»Р ВµРЎвЂЎРЎРЉРЎРѓРЎРЏ Р Р…Р В° Р В±Р С•Р С”, Р Р…Р В° Р С—Р С•Р С—Р В°, Р Р†Р Р†Р ВµРЎР‚РЎвЂ¦ Р Т‘Р Р…Р С•Р С РІР‚вЂќ
    // Р С‘РЎвЂљР С•Р С–Р С•Р Р†РЎвЂ№Р в„– РЎС“Р С–Р С•Р В» Р Р…Р В°Р С”Р В»Р С•Р Р…Р В° Р С—РЎР‚Р С‘ Р С•РЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р С”Р Вµ Р Р…Р Вµ РЎРѓР В±РЎР‚Р В°РЎРѓРЎвЂ№Р Р†Р В°Р ВµРЎвЂљРЎРѓРЎРЏ РЎРѓР С—Р ВµРЎвЂ Р С‘Р В°Р В»РЎРЉР Р…Р С•.
    private static final float SPIN_DAMPING        = 0.985f; // Р ВµРЎРѓРЎвЂљР ВµРЎРѓРЎвЂљР Р†Р ВµР Р…Р Р…Р С•Р Вµ РЎС“Р С–Р В°РЎРѓР В°Р Р…Р С‘Р Вµ Р Р†РЎР‚Р В°РЎвЂ°Р ВµР Р…Р С‘РЎРЏ Р Р† Р С—Р С•Р В»РЎвЂРЎвЂљР Вµ (Р В·Р В° РЎвЂљР С‘Р С”)
    private static final float SPIN_DAMPING_ON_HIT  = 0.9f;   // Р Т‘Р С•Р С—Р С•Р В»Р Р…Р С‘РЎвЂљР ВµР В»РЎРЉР Р…Р С•Р Вµ Р С–Р В°РЎв‚¬Р ВµР Р…Р С‘Р Вµ Р Р†РЎР‚Р В°РЎвЂ°Р ВµР Р…Р С‘РЎРЏ Р С—РЎР‚Р С‘ РЎС“Р Т‘Р В°РЎР‚Р Вµ
    private static final float SPIN_KICK_DEGREES    = 70f;    // Р СР В°Р С”РЎРѓР С‘Р СР В°Р В»РЎРЉР Р…РЎвЂ№Р в„– РЎРѓР В»РЎС“РЎвЂЎР В°Р в„–Р Р…РЎвЂ№Р в„– Р С‘Р СР С—РЎС“Р В»РЎРЉРЎРѓ Р Р†РЎР‚Р В°РЎвЂ°Р ВµР Р…Р С‘РЎРЏ Р С—РЎР‚Р С‘ РЎС“Р Т‘Р В°РЎР‚Р Вµ (Р С–РЎР‚Р В°Р Т‘/РЎвЂљР С‘Р С”)
    private float pitchSpin; // Р С–РЎР‚Р В°Р Т‘/РЎвЂљР С‘Р С” РІР‚вЂќ РЎвЂљР В°Р Р…Р С–Р В°Р В¶
    private float yawSpin;   // Р С–РЎР‚Р В°Р Т‘/РЎвЂљР С‘Р С” РІР‚вЂќ РЎР‚РЎвЂ№РЎРѓР С”Р В°Р Р…РЎРЉР Вµ

    private boolean settled = false;
    private int bounceCount = 0;

    public Rgd5Entity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
        initFuse();
    }

    public Rgd5Entity(Level level, LivingEntity shooter) {
        super(ModEntities.RGD_5_PROJECTILE.get(), shooter, level);
        initFuse();
    }

    private void initFuse() {
        this.fuseTimer = 64 + new Random().nextInt(21);

        // Р РЋР В»РЎС“РЎвЂЎР В°Р в„–Р Р…Р С•Р Вµ Р Р…Р В°РЎвЂЎР В°Р В»РЎРЉР Р…Р С•Р Вµ Р С”РЎС“Р Р†РЎвЂ№РЎР‚Р С”Р В°Р Р…Р С‘Р Вµ РІР‚вЂќ Р С–РЎР‚Р В°Р Р…Р В°РЎвЂљР В° Р Р…Р Вµ Р В»Р ВµРЎвЂљР С‘РЎвЂљ "РЎР‚Р С•Р Р†Р Р…Р С•", Р В° РЎвЂ¦Р В°Р С•РЎвЂљР С‘РЎвЂЎР Р…Р С•
        // Р С”РЎР‚РЎС“РЎвЂљР С‘РЎвЂљРЎРѓРЎРЏ Р Р† Р С—Р С•Р В»РЎвЂРЎвЂљР Вµ РЎРѓ РЎРѓР В°Р СР С•Р С–Р С• Р В±РЎР‚Р С•РЎРѓР С”Р В°, Р С”Р В°Р С” Р Р† Squad.
        Random rand = new Random();
        this.pitchSpin = (rand.nextFloat() - 0.5f) * 50f;
        this.yawSpin   = (rand.nextFloat() - 0.5f) * 50f;
    }

    @Override
    protected float getGravity() { return 0.05F; }

    @Override
    public void tick() {
        super.tick();
        ticksAlive++;
        this.fuseTimer--;

        if (!settled && ticksAlive > THROW_ANIM_TICKS) {
            this.setXRot(wrapDegrees(this.getXRot() + pitchSpin));
            this.setYRot(wrapDegrees(this.getYRot() + yawSpin));

            pitchSpin *= SPIN_DAMPING;
            yawSpin   *= SPIN_DAMPING;
        }

        if (this.fuseTimer <= 0) explode();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (settled) return;

        Direction face = result.getDirection();
        Vec3 v = this.getDeltaMovement();
        double mx = v.x, my = v.y, mz = v.z;

        // Отталкиваем гранату от поверхности блока вдоль нормали грани, по которой
        // произошло попадание. Без этого сущность остаётся ровно на границе блока,
        // и при повторном отскоке (особенно на стыке двух блоков) может быть
        // ошибочно засчитана как находящаяся внутри блока и застрять там.
        this.setPos(
                this.getX() + face.getStepX() * BOUNCE_PUSH_OUT,
                this.getY() + face.getStepY() * BOUNCE_PUSH_OUT,
                this.getZ() + face.getStepZ() * BOUNCE_PUSH_OUT);

        // Р вЂ™Р С•РЎРѓРЎРѓРЎвЂљР В°Р Р…Р С•Р Р†Р В»Р ВµР Р…Р С‘Р Вµ РЎРЊР Р…Р ВµРЎР‚Р С–Р С‘Р С‘ РЎРѓР В»Р В°Р В±Р ВµР ВµРЎвЂљ РЎРѓ Р С”Р В°Р В¶Р Т‘РЎвЂ№Р С РЎРѓР В»Р ВµР Т‘РЎС“РЎР‹РЎвЂ°Р С‘Р С РЎС“Р Т‘Р В°РЎР‚Р С•Р С РІР‚вЂќ РЎвЂљР В°Р С” Р С–РЎР‚Р В°Р Р…Р В°РЎвЂљР В°
        // Р С–Р В°РЎРѓР С‘РЎвЂљ РЎРѓР ВµР В±РЎРЏ РЎРѓР В°Р СР В°, Р В±Р ВµР В·Р С• Р Р†РЎРѓРЎРЏР С”Р С•Р С–Р С• Р С‘РЎРѓР С”РЎС“РЎРѓРЎРѓРЎвЂљР Р†Р ВµР Р…Р Р…Р С•Р С–Р С• Р В»Р С‘Р СР С‘РЎвЂљР В° Р Р…Р В° РЎвЂЎР С‘РЎРѓР В»Р С• Р С•РЎвЂљРЎРѓР С”Р С•Р С”Р С•Р Р†.
        double restitution = Math.max(MIN_RESTITUTION,
                BASE_RESTITUTION * Math.pow(RESTITUTION_DECAY, bounceCount));

        switch (face.getAxis()) {
            case X -> { mx = -mx * restitution; my *= BOUNCE_TANGENT_FRICTION; mz *= BOUNCE_TANGENT_FRICTION; }
            case Y -> { my = -my * restitution; mx *= BOUNCE_TANGENT_FRICTION; mz *= BOUNCE_TANGENT_FRICTION; }
            case Z -> { mz = -mz * restitution; mx *= BOUNCE_TANGENT_FRICTION; my *= BOUNCE_TANGENT_FRICTION; }
        }

        bounceCount++;

        // Р С™Р В°Р В¶Р Т‘РЎвЂ№Р в„– РЎС“Р Т‘Р В°РЎР‚ Р С—РЎР‚Р С‘Р Т‘Р В°РЎвЂРЎвЂљ РЎРѓР В»РЎС“РЎвЂЎР В°Р в„–Р Р…РЎвЂ№Р в„– Р С‘Р СР С—РЎС“Р В»РЎРЉРЎРѓ Р С”РЎС“Р Р†РЎвЂ№РЎР‚Р С”Р В°Р Р…Р С‘РЎР‹ РІР‚вЂќ Р С–РЎР‚Р В°Р Р…Р В°РЎвЂљР В° Р СР С•Р В¶Р ВµРЎвЂљ
        // РЎС“Р В»Р ВµРЎвЂЎРЎРЉРЎРѓРЎРЏ Р Р…Р В° Р В±Р С•Р С”, Р Р…Р В° Р С—Р С•Р С—Р В° Р С‘Р В»Р С‘ Р С”Р В°Р С” РЎС“Р С–Р С•Р Т‘Р Р…Р С• Р ВµРЎвЂ°РЎвЂ, Р В° Р Р…Р Вµ РЎвЂљР С•Р В»РЎРЉР С”Р С• "РЎР‚Р С•Р Р†Р Р…Р С•".
        Random rand = new Random();
        pitchSpin += (rand.nextFloat() - 0.5f) * SPIN_KICK_DEGREES;
        yawSpin   += (rand.nextFloat() - 0.5f) * SPIN_KICK_DEGREES;
        pitchSpin *= SPIN_DAMPING_ON_HIT;
        yawSpin   *= SPIN_DAMPING_ON_HIT;

        double speedSq = mx*mx + my*my + mz*mz;

        // Р вЂњРЎР‚Р В°Р Р…Р В°РЎвЂљР В° Р С•РЎРѓРЎвЂљР В°Р Р…Р В°Р Р†Р В»Р С‘Р Р†Р В°Р ВµРЎвЂљРЎРѓРЎРЏ Р РЋР С’Р СљР С’, Р С”Р С•Р С–Р Т‘Р В° РЎРѓР С”Р С•РЎР‚Р С•РЎРѓРЎвЂљРЎРЉ Р ВµРЎРѓРЎвЂљР ВµРЎРѓРЎвЂљР Р†Р ВµР Р…Р Р…РЎвЂ№Р С Р С•Р В±РЎР‚Р В°Р В·Р С•Р С
        // Р С—Р В°Р Т‘Р В°Р ВµРЎвЂљ Р Р…Р С‘Р В¶Р Вµ Р С—Р С•РЎР‚Р С•Р С–Р В° РІР‚вЂќ Р В° Р Р…Р Вµ Р С—Р С• РЎРѓРЎвЂЎРЎвЂРЎвЂљРЎвЂЎР С‘Р С”РЎС“ "Р Т‘Р Р†Р В° РЎС“Р Т‘Р В°РЎР‚Р В° Р С‘ Р В·Р В°Р СР С•РЎР‚Р С•Р В·Р С”Р В°".
        if (speedSq < SETTLE_SPEED_SQ) {
            settle();
            return;
        }

        this.setDeltaMovement(mx, my, mz);
    }

    private void settle() {
        settled = true;
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        pitchSpin = 0f;
        yawSpin = 0f;
        // xRot/yRot Р СњР вЂў РЎРѓР В±РЎР‚Р В°РЎРѓРЎвЂ№Р Р†Р В°Р ВµР С: Р С–РЎР‚Р В°Р Р…Р В°РЎвЂљР В° Р С•РЎРѓРЎвЂљР В°РЎвЂРЎвЂљРЎРѓРЎРЏ Р В»Р ВµР В¶Р В°РЎвЂљРЎРЉ Р С—Р С•Р Т‘ РЎвЂљР ВµР С РЎС“Р С–Р В»Р С•Р С, Р Р…Р В°
        // Р С”Р С•РЎвЂљР С•РЎР‚РЎвЂ№Р в„– Р ВµРЎвЂ Р Т‘Р С•Р С”РЎР‚РЎС“РЎвЂљР С‘Р В»Р С• Р С—РЎР‚Р С‘ Р С—Р С•РЎРѓР В»Р ВµР Т‘Р Р…Р ВµР С Р С•РЎвЂљРЎРѓР С”Р С•Р С”Р Вµ РІР‚вЂќ Р Р…Р В° Р В±Р С•Р С”РЎС“, Р Р…Р В° Р С—Р С•Р С—Р В°,
        // Р Р†Р Р†Р ВµРЎР‚РЎвЂ¦ Р Т‘Р Р…Р С•Р С Р С‘ РЎвЂљ.Р Т‘., Р В° Р Р…Р Вµ Р Р†РЎРѓР ВµР С–Р Т‘Р В° РЎРѓРЎвЂљРЎР‚Р С•Р С–Р С• "РЎР‚Р С•Р Р†Р Р…Р С•".
    }

    private static float wrapDegrees(float angle) {
        angle %= 360f;
        if (angle < -180f) angle += 360f;
        if (angle > 180f) angle -= 360f;
        return angle;
    }

    private void explode() {
        Level level = this.level();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;

        Random rand = new Random();

        // Р СњР ВµР В±Р С•Р В»РЎРЉРЎв‚¬Р С•Р в„– РЎРѓР В»РЎС“РЎвЂЎР В°Р в„–Р Р…РЎвЂ№Р в„– Р С—Р С‘РЎвЂљРЎвЂЎ (0.9РІР‚вЂњ1.1), РЎвЂЎРЎвЂљР С•Р В±РЎвЂ№ Р Р†Р В·РЎР‚РЎвЂ№Р Р† Р Р…Р Вµ Р В·Р Р†РЎС“РЎвЂЎР В°Р В» Р С•Р Т‘Р С‘Р Р…Р В°Р С”Р С•Р Р†Р С•
        // "Р С”Р С•Р Р…РЎРѓР ВµРЎР‚Р Р†Р Р…Р С•" Р С”Р В°Р В¶Р Т‘РЎвЂ№Р в„– РЎР‚Р В°Р В· Р С•Р Т‘Р Р…Р С•Р в„– Р С‘ РЎвЂљР С•Р в„– Р В¶Р Вµ Р Р…Р С•РЎвЂљР С•Р в„–.
        float explosionPitch = 0.9F + rand.nextFloat() * 0.2F;

        // Р вЂ™Р В°Р В¶Р Р…Р С•: Р В·Р Р†РЎС“Р С” Р В±Р ВµРЎР‚РЎвЂРЎвЂљРЎРѓРЎРЏ Р С—Р С• Р С”Р С•Р С•РЎР‚Р Т‘Р С‘Р Р…Р В°РЎвЂљР В°Р С Р С–РЎР‚Р В°Р Р…Р В°РЎвЂљРЎвЂ№ (Р В° Р Р…Р Вµ entity-sourced Р Р†Р ВµРЎР‚РЎРѓР С‘Р ВµР в„–
        // playSound(Player, Entity, ...)). Entity-sourced Р В·Р Р†РЎС“Р С” Р С”Р В»Р С‘Р ВµР Р…РЎвЂљ Р С—РЎР‚Р С‘Р Р†РЎРЏР В·РЎвЂ№Р Р†Р В°Р ВµРЎвЂљ
        // Р С” Р В¶Р С‘Р Р†Р С•Р СРЎС“ entity-РЎвЂљРЎР‚Р ВµР С”Р ВµРЎР‚РЎС“, Р В° РЎвЂЎР ВµРЎР‚Р ВµР В· Р С—Р В°РЎР‚РЎС“ РЎРѓРЎвЂљРЎР‚Р С•Р С” Р Р…Р С‘Р В¶Р Вµ Р С–РЎР‚Р В°Р Р…Р В°РЎвЂљР В° РЎС“Р Т‘Р В°Р В»РЎРЏР ВµРЎвЂљРЎРѓРЎРЏ
        // РЎвЂЎР ВµРЎР‚Р ВµР В· this.discard() РІР‚вЂќ РЎвЂљРЎР‚Р ВµР С”Р ВµРЎР‚ Р ВµРЎвЂ РЎвЂљР ВµРЎР‚РЎРЏР ВµРЎвЂљ Р С‘ Р С•Р В±РЎР‚РЎвЂ№Р Р†Р В°Р ВµРЎвЂљ Р В·Р Р†РЎС“Р С” Р С—Р С•РЎвЂЎРЎвЂљР С‘ РЎРѓРЎР‚Р В°Р В·РЎС“
        // (РЎвЂљР Вµ РЎРѓР В°Р СРЎвЂ№Р Вµ ~0.1 РЎРѓР ВµР С”). Р СџР С•Р В·Р С‘РЎвЂ Р С‘Р С•Р Р…Р Р…РЎвЂ№Р в„– Р В·Р Р†РЎС“Р С” Р С•РЎвЂљ Р В¶Р С‘Р В·Р Р…Р С‘ РЎРѓРЎС“РЎвЂ°Р Р…Р С•РЎРѓРЎвЂљР С‘ Р Р…Р Вµ Р В·Р В°Р Р†Р С‘РЎРѓР С‘РЎвЂљ Р С‘
        // Р Т‘Р С•Р С‘Р С–РЎР‚РЎвЂ№Р Р†Р В°Р ВµРЎвЂљ Р Т‘Р С• Р С”Р С•Р Р…РЎвЂ Р В°, Р В° Р С”Р С•Р С•РЎР‚Р Т‘Р С‘Р Р…Р В°РЎвЂљРЎвЂ№ Р Р†РЎРѓРЎвЂ РЎР‚Р В°Р Р†Р Р…Р С• Р В±Р ВµРЎР‚РЎС“РЎвЂљРЎРѓРЎРЏ Р С‘Р СР ВµР Р…Р Р…Р С• РЎРѓ Р С–РЎР‚Р В°Р Р…Р В°РЎвЂљРЎвЂ№.
        Vec3 startPos = this.position().add(0, 0.1, 0);

        playBlastSoundByDistance(serverLevel, startPos,
                ModSounds.RGD5_BLAST_CLOSE.get(), ModSounds.RGD5_BLAST_MID.get(),
                ModSounds.RGD5_BLAST_FAR.get(), ModSounds.RGD5_BLAST_DISTANT.get(),
                explosionPitch);

        // Р вЂњР В°РЎР‚Р В°Р Р…РЎвЂљР С‘РЎР‚Р С•Р Р†Р В°Р Р…Р Р…Р В°РЎРЏ Р В»Р ВµРЎвЂљР В°Р В»РЎРЉР Р…Р С•РЎРѓРЎвЂљРЎРЉ Р Р† РЎС“Р С—Р С•РЎР‚ РЎРѓРЎвЂЎР С‘РЎвЂљР В°Р ВµРЎвЂљРЎРѓРЎРЏ Р СџР вЂўР В Р вЂ™Р С›Р в„ў, Р Т‘Р С• РЎРѓР В»РЎС“РЎвЂЎР В°Р в„–Р Р…РЎвЂ№РЎвЂ¦
        // Р С•РЎРѓР С”Р С•Р В»Р С”Р С•Р Р† РІР‚вЂќ РЎРѓР С. Р С”Р С•Р СР СР ВµР Р…РЎвЂљР В°РЎР‚Р С‘Р в„– РЎС“ LETHAL_RADIUS Р Р†РЎвЂ№РЎв‚¬Р Вµ.
        AABB lethalSearchBox = new AABB(startPos, startPos).inflate(LETHAL_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, lethalSearchBox)) {
            AABB targetBox = target.getBoundingBox();

            // Р вЂќР С‘РЎРѓРЎвЂљР В°Р Р…РЎвЂ Р С‘РЎРЏ Р Т‘Р С• Р вЂР вЂєР ВР вЂ“Р С’Р в„ўР РЃР вЂўР в„ў РЎвЂљР С•РЎвЂЎР С”Р С‘ РЎвЂ¦Р С‘РЎвЂљР В±Р С•Р С”РЎРѓР В°, Р В° Р Р…Р Вµ Р Т‘Р С• Р ВµР С–Р С• РЎвЂ Р ВµР Р…РЎвЂљРЎР‚Р В°.
            double distSq = targetBox.distanceToSqr(startPos);
            boolean withinRadius = distSq <= LETHAL_RADIUS * LETHAL_RADIUS;

            Vec3 nearestPoint = closestPointOnAABB(targetBox, startPos);
            BlockHitResult cover = level.clip(new ClipContext(
                    startPos, nearestPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            boolean covered = cover.getType() != HitResult.Type.MISS;

            // this.damageSources().thrown(...), Р В° Р Р…Р Вµ explosion(...) РІР‚вЂќ РЎвЂљР С‘Р С— explosion
            // Р Р…Р В° Peaceful Р В·Р В°Р Р…РЎС“Р В»РЎРЏР ВµРЎвЂљРЎРѓРЎРЏ Р Р†Р В°Р Р…Р С‘Р В»РЎРЉР Р…РЎвЂ№Р С difficulty-РЎРѓР С”Р ВµР в„–Р В»Р С‘Р Р…Р С–Р С•Р С (scaling:
            // always) Р Р…Р ВµР В·Р В°Р Р†Р С‘РЎРѓР С‘Р СР С• Р С•РЎвЂљ Р С”Р С•Р В»Р С‘РЎвЂЎР ВµРЎРѓРЎвЂљР Р†Р В° РЎС“РЎР‚Р С•Р Р…Р В°. thrown РЎвЂљР В°Р С”Р С•Р СРЎС“ РЎРѓР С”Р ВµР в„–Р В»Р С‘Р Р…Р С–РЎС“ Р Р…Р Вµ
            // Р С—Р С•Р Т‘Р Р†Р ВµРЎР‚Р В¶Р ВµР Р…, Р В° Р Р…Р В° Р С—Р С•Р Р†Р ВµР Т‘Р ВµР Р…Р С‘Р Вµ (Р Р…Р С•Р С”Р В±РЎРЊР С” Р С‘ РЎвЂљ.Р Т‘.) РЎРЊРЎвЂљР С• Р Р…Р Вµ Р Р†Р В»Р С‘РЎРЏР ВµРЎвЂљ, РЎвЂљ.Р С”. РЎРЊРЎвЂљР С•
            // Р С—РЎР‚РЎРЏР СР С•Р в„– hurt(), Р В° Р Р…Р Вµ Р Р…Р В°РЎРѓРЎвЂљР С•РЎРЏРЎвЂ°Р С‘Р в„– Р Р†Р В·РЎР‚РЎвЂ№Р Р† Р В±Р В»Р С•Р С”Р С•Р Р†.
            var lethalDamageSource = this.damageSources().thrown(this, this.getOwner());

            if (!withinRadius) continue;
            if (covered) continue; // Р СР ВµР В¶Р Т‘РЎС“ Р С–РЎР‚Р В°Р Р…Р В°РЎвЂљР С•Р в„– Р С‘ РЎвЂ Р ВµР В»РЎРЉРЎР‹ Р ВµРЎРѓРЎвЂљРЎРЉ Р В±Р В»Р С•Р С” РІР‚вЂќ РЎвЂ Р ВµР В»РЎРЉ Р Р† РЎС“Р С”РЎР‚РЎвЂ№РЎвЂљР С‘Р С‘

            target.hurt(lethalDamageSource, LETHAL_DAMAGE);
        }

        for (int i = 0; i < SHRAPNEL_COUNT; i++) {
            double theta = 2 * Math.PI * rand.nextDouble();
            double phi   = Math.acos(1 - 2 * rand.nextDouble());
            double dx    = Math.sin(phi) * Math.cos(theta);
            double dy    = Math.sin(phi) * Math.sin(theta);
            double dz    = Math.cos(phi);

            double dist = MAX_DIST_MIN + rand.nextDouble() * (MAX_DIST_MAX - MAX_DIST_MIN);
            if (rand.nextDouble() < 0.2) dist *= (0.3 + rand.nextDouble() * 0.5);

            Vec3 dir    = new Vec3(dx, dy, dz).normalize();
            Vec3 endPos = startPos.add(dir.scale(dist));

            BlockHitResult blockHit = level.clip(new ClipContext(
                    startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            Vec3 hitPos = (blockHit.getType() != HitResult.Type.MISS)
                    ? blockHit.getLocation() : endPos;

            AABB searchBox = this.getBoundingBox().expandTowards(dir.scale(dist)).inflate(1.0);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox)) {
                // Р Р†Р В»Р В°Р Т‘Р ВµР В»Р ВµРЎвЂ  (Р В±РЎР‚Р С•РЎРѓР С‘Р Р†РЎв‚¬Р С‘Р в„– Р С‘Р С–РЎР‚Р С•Р С”) РЎвЂљР С•Р В¶Р Вµ Р СР С•Р В¶Р ВµРЎвЂљ Р С—Р С•Р В»РЎС“РЎвЂЎР С‘РЎвЂљРЎРЉ РЎС“РЎР‚Р С•Р Р… Р С•РЎвЂљ РЎРѓР Р†Р С•Р ВµР в„– Р В¶Р Вµ Р С–РЎР‚Р В°Р Р…Р В°РЎвЂљРЎвЂ№
                Optional<Vec3> entityHit = target.getBoundingBox().inflate(0.1).clip(startPos, hitPos);
                if (entityHit.isPresent()) {
                    hitPos = entityHit.get();

                    double travelDist = startPos.distanceTo(hitPos);
                    float damage = shrapnelDamageAt(travelDist);
                    if (damage > 0.0F) {
                        target.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
                    }
                    break;
                }
            }

            sendDebugRay(serverLevel, startPos, hitPos);
        }

        GrenadeVfxUtils.playExplosionVisuals(serverLevel, startPos, rand);

        this.discard();
    }

    /**
     * Р вЂР В»Р С‘Р В¶Р В°Р в„–РЎв‚¬Р В°РЎРЏ Р С” point РЎвЂљР С•РЎвЂЎР С”Р В° Р Р†Р Р…РЎС“РЎвЂљРЎР‚Р С‘/Р Р…Р В° Р С–РЎР‚Р В°Р Р…Р С‘РЎвЂ Р Вµ box. Р ВРЎРѓР С—Р С•Р В»РЎРЉР В·РЎС“Р ВµРЎвЂљРЎРѓРЎРЏ, РЎвЂЎРЎвЂљР С•Р В±РЎвЂ№ Р СР ВµРЎР‚Р С‘РЎвЂљРЎРЉ
     * Р Т‘Р С‘РЎРѓРЎвЂљР В°Р Р…РЎвЂ Р С‘РЎР‹ "Р Т‘Р С• РЎвЂ¦Р С‘РЎвЂљР В±Р С•Р С”РЎРѓР В°", Р В° Р Р…Р Вµ "Р Т‘Р С• РЎвЂ Р ВµР Р…РЎвЂљРЎР‚Р В° РЎРѓРЎС“РЎвЂ°Р Р…Р С•РЎРѓРЎвЂљР С‘", Р С‘ РЎвЂЎРЎвЂљР С•Р В±РЎвЂ№ Р В»РЎС“РЎвЂЎ Р С—РЎР‚Р С•Р Р†Р ВµРЎР‚Р С”Р С‘
     * РЎС“Р С”РЎР‚РЎвЂ№РЎвЂљР С‘РЎРЏ Р В±РЎвЂ№Р В» Р Р…Р В°Р С—РЎР‚Р В°Р Р†Р В»Р ВµР Р… Р Р† РЎвЂљРЎС“ Р В¶Р Вµ РЎРѓР В°Р СРЎС“РЎР‹ РЎвЂљР С•РЎвЂЎР С”РЎС“.
     */
    private static Vec3 closestPointOnAABB(AABB box, Vec3 point) {
        double x = Math.max(box.minX, Math.min(point.x, box.maxX));
        double y = Math.max(box.minY, Math.min(point.y, box.maxY));
        double z = Math.max(box.minZ, Math.min(point.z, box.maxZ));
        return new Vec3(x, y, z);
    }

    /**
     * Р вЂєР С‘Р Р…Р ВµР в„–Р Р…РЎвЂ№Р в„– РЎРѓР С—Р В°Р Т‘ РЎС“РЎР‚Р С•Р Р…Р В° Р С•РЎРѓР С”Р С•Р В»Р С”Р В°: SHRAPNEL_DAMAGE Р Р…Р В° Р Т‘Р С‘РЎРѓРЎвЂљР В°Р Р…РЎвЂ Р С‘Р С‘ 0,
     * Р С—Р В»Р В°Р Р†Р Р…Р С• Р С—Р В°Р Т‘Р В°Р ВµРЎвЂљ Р Т‘Р С• SHRAPNEL_DAMAGE * SHRAPNEL_MIN_DAMAGE_FRACTION
     * Р Р…Р В° Р Т‘Р С‘РЎРѓРЎвЂљР В°Р Р…РЎвЂ Р С‘Р С‘ SHRAPNEL_FALLOFF_DIST Р С‘ Р Т‘Р В°Р В»Р ВµР Вµ.
     */
    private float shrapnelDamageAt(double travelDist) {
        double t = travelDist / SHRAPNEL_FALLOFF_DIST;
        if (t < 0.0) t = 0.0;
        if (t > 1.0) t = 1.0;

        float fraction = (float) (1.0 - t * (1.0 - SHRAPNEL_MIN_DAMAGE_FRACTION));
        return SHRAPNEL_DAMAGE * fraction;
    }

    private void sendDebugRay(ServerLevel serverLevel, Vec3 startPos, Vec3 hitPos) {
        for (ServerPlayer sp : serverLevel.players()) {
            if (!sp.isCreative() || !ModNetworking.hasHitboxDebug(sp.getUUID())) continue;

            double rayLen = startPos.distanceTo(hitPos);
            int steps = Math.max(1, (int)(rayLen / 0.55));
            Vec3 stepDir = hitPos.subtract(startPos).scale(1.0 / steps);
            for (int s = 1; s <= steps; s++) {
                Vec3 p = startPos.add(stepDir.scale(s));
                sp.connection.send(new ClientboundLevelParticlesPacket(
                        ParticleTypes.CRIT, false,
                        p.x, p.y, p.z, 0f, 0f, 0f, 0.001f, 1));
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<Rgd5Entity> event) {
        if (ticksAlive <= THROW_ANIM_TICKS) {
            return event.setAndContinue(ANIM_THROW);
        }

        Vec3 vel = this.getDeltaMovement();
        double speed = Math.sqrt(vel.x*vel.x + vel.z*vel.z + vel.y*vel.y);

        if (speed > 0.01) {
            event.getController().setAnimationSpeed(speed * 2.5);
            return event.setAndContinue(ANIM_FLY);
        }

        return PlayState.STOP;
    }
    @Override
    protected void defineSynchedData() { }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
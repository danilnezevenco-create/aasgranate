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

/**
 * M67 РІР‚вЂќ РЎвЂљР В° Р В¶Р Вµ РЎРѓР С‘РЎРѓРЎвЂљР ВµР СР В° (Р С•РЎРѓР С”Р С•Р В»Р С”Р С‘, РЎвЂћР С‘Р В·Р С‘Р С”Р В° Р С—РЎР‚Р С‘Р В·Р ВµР СР В»Р ВµР Р…Р С‘РЎРЏ, Р С”РЎС“Р Р†РЎвЂ№РЎР‚Р С”Р В°Р Р…Р С‘Р Вµ), РЎвЂЎРЎвЂљР С• Р С‘ РЎС“ RGD-5,
 * Р С•РЎвЂљР В»Р С‘РЎвЂЎР В°Р ВµРЎвЂљРЎРѓРЎРЏ РЎвЂљР С•Р В»РЎРЉР С”Р С• Р В·Р В°Р Т‘Р ВµРЎР‚Р В¶Р С”Р С•Р в„– Р Р†Р В·РЎР‚РЎвЂ№Р Р†Р В° (РЎРѓР С. initFuse) Р С‘ РЎРѓР Р†Р С•Р С‘Р СР С‘ Р В·Р Р†РЎС“Р С”Р В°Р СР С‘/Р СР С•Р Т‘Р ВµР В»РЎРЉРЎР‹.
 */
public class M67Entity extends ThrowableProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int fuseTimer;

    private static final RawAnimation ANIM_THROW =
            RawAnimation.begin().thenPlayAndHold("animation.grenade.throw");

    private static final RawAnimation ANIM_FLY =
            RawAnimation.begin().thenLoop("animation.grenade.fly");

    private static final int    SHRAPNEL_COUNT  = 840;
    private static final double MAX_DIST_MIN    = 14.0;
    private static final double MAX_DIST_MAX    = 36.0;
    private static final float  SHRAPNEL_DAMAGE = 32.0F;
    private static final double SHRAPNEL_FALLOFF_DIST = MAX_DIST_MAX;
    private static final float  SHRAPNEL_MIN_DAMAGE_FRACTION = 0.0F;

    private static final double LETHAL_RADIUS = 2.0;
    private static final float  LETHAL_DAMAGE = 1000.0F;

    private static final int THROW_ANIM_TICKS = 15;
    private int ticksAlive = 0;

    // РІвЂќР‚РІвЂќР‚ Р В¤Р С‘Р В·Р С‘Р С”Р В° Р С—РЎР‚Р С‘Р В·Р ВµР СР В»Р ВµР Р…Р С‘РЎРЏ (РЎР‚Р ВµР В°Р В»Р С‘РЎРѓРЎвЂљР С‘РЎвЂЎР Р…Р В°РЎРЏ, Р В±Р ВµР В· Р С‘РЎРѓР С”РЎС“РЎРѓРЎРѓРЎвЂљР Р†Р ВµР Р…Р Р…Р С•Р С–Р С• Р В»Р С‘Р СР С‘РЎвЂљР В° Р С•РЎвЂљРЎРѓР С”Р С•Р С”Р С•Р Р†) РІвЂќР‚РІвЂќР‚
    private static final double BASE_RESTITUTION        = 0.21; // уменьшено в 2 раза (было 0.42) — гранаты отскакивают вдвое слабее
    private static final double RESTITUTION_DECAY       = 0.80;
    private static final double MIN_RESTITUTION         = 0.10;
    private static final double BOUNCE_TANGENT_FRICTION = 0.55;
    private static final double SETTLE_SPEED_SQ         = 0.03 * 0.03;

    // Небольшой отступ от поверхности блока сразу после отскока, чтобы граната
    // не оставалась ровно на границе блока и не "проваливалась" внутрь него
    // на следующем тике при повторном отскоке (особенно у углов/стыков блоков).
    private static final double BOUNCE_PUSH_OUT = 0.06;

    // РІвЂќР‚РІвЂќР‚ Р С™РЎС“Р Р†РЎвЂ№РЎР‚Р С”Р В°Р Р…Р С‘Р Вµ (РЎвЂљРЎС“Р СР В±Р В»Р С‘Р Р…Р С–) Р Р† Р С—Р С•Р В»РЎвЂРЎвЂљР Вµ, Р В°-Р В»РЎРЏ Squad РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚
    private static final float SPIN_DAMPING        = 0.985f;
    private static final float SPIN_DAMPING_ON_HIT  = 0.9f;
    private static final float SPIN_KICK_DEGREES    = 70f;
    private float pitchSpin;
    private float yawSpin;

    private boolean settled = false;
    private int bounceCount = 0;

    public M67Entity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
        initFuse();
    }

    public M67Entity(Level level, LivingEntity shooter) {
        super(ModEntities.M_67_PROJECTILE.get(), shooter, level);
        initFuse();
    }

    private void initFuse() {
        // Р В Р ВµР В°Р В»РЎРЉР Р…Р В°РЎРЏ M67 Р С‘РЎРѓР С—Р С•Р В»РЎРЉР В·РЎС“Р ВµРЎвЂљ Р С—Р С‘РЎР‚Р С•РЎвЂљР ВµРЎвЂ¦Р Р…Р С‘РЎвЂЎР ВµРЎРѓР С”Р С‘Р в„– Р В·Р В°Р СР ВµР Т‘Р В»Р С‘РЎвЂљР ВµР В»РЎРЉ РЎРѓ Р В·Р В°Р Т‘Р ВµРЎР‚Р В¶Р С”Р С•Р в„–
        // 4РІР‚вЂњ5 РЎРѓР ВµР С”РЎС“Р Р…Р Т‘ (Р Р…Р С•Р СР С‘Р Р…Р В°Р В»РЎРЉР Р…Р С• ~4.5 РЎРѓ) РІР‚вЂќ РЎРѓРЎвЂљР В°Р Р…Р Т‘Р В°РЎР‚РЎвЂљР Р…РЎвЂ№Р в„– РЎР‚Р В°Р В·Р В±РЎР‚Р С•РЎРѓ РЎРѓР ВµРЎР‚Р С‘Р в„–Р Р…Р С•Р С–Р С•
        // Р Р†Р В·РЎР‚РЎвЂ№Р Р†Р В°РЎвЂљР ВµР В»РЎРЏ Р Сљ213. Р СџРЎР‚Р С‘ 20 РЎвЂљР С‘Р С”Р В°РЎвЂ¦/РЎРѓР ВµР С” РЎРЊРЎвЂљР С• 80РІР‚вЂњ100 РЎвЂљР С‘Р С”Р С•Р Р†.
        this.fuseTimer = 80 + new Random().nextInt(21);

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

        double restitution = Math.max(MIN_RESTITUTION,
                BASE_RESTITUTION * Math.pow(RESTITUTION_DECAY, bounceCount));

        switch (face.getAxis()) {
            case X -> { mx = -mx * restitution; my *= BOUNCE_TANGENT_FRICTION; mz *= BOUNCE_TANGENT_FRICTION; }
            case Y -> { my = -my * restitution; mx *= BOUNCE_TANGENT_FRICTION; mz *= BOUNCE_TANGENT_FRICTION; }
            case Z -> { mz = -mz * restitution; mx *= BOUNCE_TANGENT_FRICTION; my *= BOUNCE_TANGENT_FRICTION; }
        }

        bounceCount++;

        Random rand = new Random();
        pitchSpin += (rand.nextFloat() - 0.5f) * SPIN_KICK_DEGREES;
        yawSpin   += (rand.nextFloat() - 0.5f) * SPIN_KICK_DEGREES;
        pitchSpin *= SPIN_DAMPING_ON_HIT;
        yawSpin   *= SPIN_DAMPING_ON_HIT;

        double speedSq = mx*mx + my*my + mz*mz;

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

        float explosionPitch = 0.9F + rand.nextFloat() * 0.2F;

        Vec3 startPos = this.position().add(0, 0.1, 0);

        playBlastSoundByDistance(serverLevel, startPos,
                ModSounds.M67_BLAST_CLOSE.get(), ModSounds.M67_BLAST_MID.get(),
                ModSounds.M67_BLAST_FAR.get(), ModSounds.M67_BLAST_DISTANT.get(),
                explosionPitch);

        AABB lethalSearchBox = new AABB(startPos, startPos).inflate(LETHAL_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, lethalSearchBox)) {
            AABB targetBox = target.getBoundingBox();

            double distSq = targetBox.distanceToSqr(startPos);
            boolean withinRadius = distSq <= LETHAL_RADIUS * LETHAL_RADIUS;

            Vec3 nearestPoint = closestPointOnAABB(targetBox, startPos);
            BlockHitResult cover = level.clip(new ClipContext(
                    startPos, nearestPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            boolean covered = cover.getType() != HitResult.Type.MISS;

            var lethalDamageSource = this.damageSources().thrown(this, this.getOwner());

            if (!withinRadius) continue;
            if (covered) continue;

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

    private static Vec3 closestPointOnAABB(AABB box, Vec3 point) {
        double x = Math.max(box.minX, Math.min(point.x, box.maxX));
        double y = Math.max(box.minY, Math.min(point.y, box.maxY));
        double z = Math.max(box.minZ, Math.min(point.z, box.maxZ));
        return new Vec3(x, y, z);
    }

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

    private PlayState predicate(AnimationState<M67Entity> event) {
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
package com.danilfb123.aasgranate;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.phys.EntityHitResult;
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
 * РГО — ручная граната оборонительная с ударно-дистанционным взрывателем (аналог УДЗ).
 *
 * Логика взрывателя:
 *  - если граната успела улететь на IMPACT_ARM_DISTANCE (15) блоков и более от точки
 *    броска, то первое же касание блока или существа вызывает МГНОВЕННЫЙ подрыв
 *    (ударный канал взрывателя);
 *  - если удар произошёл ближе 15 блоков — взрыватель ещё не взведён, граната
 *    отскакивает и укатывается как обычно, а подрыв происходит по самоликвидатору
 *    через 3.2–4.2 секунды с момента броска (64–84 тика).
 *
 * Всё остальное (осколки, физика отскоков, звуки, модель, анимации) — как у RGD-5/M67.
 */
public class RgoEntity extends ThrowableProjectile implements GeoEntity {

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

    // Гарантированное убийство в упор (радиус в блоках) при прямой видимости.
    private static final double LETHAL_RADIUS = 2.0;
    private static final float  LETHAL_DAMAGE = 1000.0F;

    // --- Ударно-дистанционный взрыватель РГО ---
    // Дистанция взведения ударного канала: если граната пролетела от точки броска
    // столько блоков и больше, удар о блок/существо подрывает её мгновенно.
    private static final double IMPACT_ARM_DISTANCE = 15.0;

    // Точка, из которой граната была брошена (для замера дистанции взведения).
    private Vec3 throwOrigin;
    // Защита от повторного подрыва (удар + таймер в одном тике).
    private boolean exploded = false;

    private static final int THROW_ANIM_TICKS = 15;
    private int ticksAlive = 0;

    private static final double BASE_RESTITUTION        = 0.21;
    private static final double RESTITUTION_DECAY       = 0.80;
    private static final double MIN_RESTITUTION         = 0.10;
    private static final double BOUNCE_TANGENT_FRICTION = 0.55;
    private static final double SETTLE_SPEED_SQ         = 0.03 * 0.03;

    private static final double BOUNCE_PUSH_OUT = 0.06;

    private static final float SPIN_DAMPING        = 0.985f;
    private static final float SPIN_DAMPING_ON_HIT  = 0.9f;
    private static final float SPIN_KICK_DEGREES    = 70f;
    private float pitchSpin;
    private float yawSpin;

    private boolean settled = false;
    private int bounceCount = 0;

    public RgoEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
        initFuse();
    }

    public RgoEntity(Level level, LivingEntity shooter) {
        super(ModEntities.RGO_PROJECTILE.get(), shooter, level);
        initFuse();
        this.throwOrigin = this.position();
    }

    /** Дистанция от точки броска до текущего положения гранаты. */
    private double distanceFromThrow() {
        return throwOrigin == null ? 0.0 : this.position().distanceTo(throwOrigin);
    }

    /** Взведён ли ударный канал взрывателя (граната улетела достаточно далеко). */
    private boolean isImpactArmed() {
        return distanceFromThrow() >= IMPACT_ARM_DISTANCE;
    }

    private void initFuse() {
        this.fuseTimer = 64 + new Random().nextInt(21);

        Random rand = new Random();
        this.pitchSpin = (rand.nextFloat() - 0.5f) * 50f;
        this.yawSpin   = (rand.nextFloat() - 0.5f) * 50f;
    }

    @Override
    protected float getGravity() { return 0.05F; }

    @Override
    public void tick() {
        super.tick();

        // На случай загрузки сущности из мира/спавна не через конструктор с игроком.
        if (this.throwOrigin == null) this.throwOrigin = this.position();

        ticksAlive++;
        this.fuseTimer--;

        if (!settled && ticksAlive > THROW_ANIM_TICKS) {
            this.setXRot(wrapDegrees(this.getXRot() + pitchSpin));
            this.setYRot(wrapDegrees(this.getYRot() + yawSpin));

            pitchSpin *= SPIN_DAMPING;
            yawSpin   *= SPIN_DAMPING;
        }

        // Самоликвидатор: 3.2–4.2 сек с момента броска.
        if (this.fuseTimer <= 0) explode();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (settled || exploded) return;

        Direction face = result.getDirection();

        // Ударный канал взрывателя: если граната прилетела с 15+ блоков — рвём сразу.
        // Решение принимает сервер; клиент просто доиграет отскок до пакета об удалении.
        if (!this.level().isClientSide && isImpactArmed()) {
            // Чуть отодвигаем точку подрыва от поверхности, чтобы осколки не упирались
            // в сам блок, в который прилетела граната.
            this.setPos(
                    this.getX() + face.getStepX() * 0.1,
                    this.getY() + face.getStepY() * 0.1,
                    this.getZ() + face.getStepZ() * 0.1);
            this.setDeltaMovement(Vec3.ZERO);
            explode();
            return;
        }

        // Взрыватель не взведён — обычный отскок, ждём самоликвидатор.
        Vec3 v = this.getDeltaMovement();
        double mx = v.x, my = v.y, mz = v.z;

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

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (settled || exploded) return;

        // Прямое попадание в существо после 15+ блоков полёта — тоже мгновенный подрыв.
        if (!this.level().isClientSide && isImpactArmed()) {
            this.setDeltaMovement(Vec3.ZERO);
            explode();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FuseTimer", this.fuseTimer);
        tag.putInt("TicksAlive", this.ticksAlive);
        tag.putBoolean("Settled", this.settled);
        if (this.throwOrigin != null) {
            tag.putDouble("OriginX", this.throwOrigin.x);
            tag.putDouble("OriginY", this.throwOrigin.y);
            tag.putDouble("OriginZ", this.throwOrigin.z);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FuseTimer"))  this.fuseTimer  = tag.getInt("FuseTimer");
        if (tag.contains("TicksAlive")) this.ticksAlive = tag.getInt("TicksAlive");
        if (tag.contains("Settled"))    this.settled    = tag.getBoolean("Settled");
        if (tag.contains("OriginX")) {
            this.throwOrigin = new Vec3(
                    tag.getDouble("OriginX"), tag.getDouble("OriginY"), tag.getDouble("OriginZ"));
        }
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
        if (exploded) return;
        exploded = true;

        Random rand = new Random();

        float explosionPitch = 0.9F + rand.nextFloat() * 0.2F;

        Vec3 startPos = this.position().add(0, 0.1, 0);

        playBlastSoundByDistance(serverLevel, startPos,
                ModSounds.RGO_BLAST_CLOSE.get(), ModSounds.RGO_BLAST_MID.get(),
                ModSounds.RGO_BLAST_FAR.get(), ModSounds.RGO_BLAST_DISTANT.get(),
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

    private PlayState predicate(AnimationState<RgoEntity> event) {
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

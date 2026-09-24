package com.danilfb123.aasgranate;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayDeque;
import java.util.Random;

/**
 * M18 — ручная дымовая граната.
 *
 * По логике дыма/шлейфа/искр/физики отскоков полностью повторяет Rdg2Entity.
 * Единственное смысловое отличие от РДГ-2: пока граната летит, у неё
 * проигрывается анимация throw (первые THROW_ANIM_TICKS тиков), а затем —
 * зацикленная анимация fly, точно как у M67 / RGD-5 / RGO. У РДГ-2 таких
 * анимаций в полёте нет вообще, у M18 — есть.
 */
public class M18Entity extends ThrowableProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // --- Тайминги дыма (идентичны РДГ-2) ---
    /** Задержка от броска до начала дымления. 0 — шашка дымит уже в полёте. */
    public static final int SMOKE_DELAY_TICKS = 0;
    /** Раскрытие облака до полного объёма. */
    public static final int GROW_TICKS        = 600;  // 10 сек
    /** Полный дым. */
    public static final int HOLD_TICKS        = 1200; // 60 сек
    /** Затухание. */
    public static final int FADE_TICKS        = 200;  // 10 сек
    public static final int TOTAL_SMOKE_TICKS = GROW_TICKS + HOLD_TICKS + FADE_TICKS;

    // --- Габариты облака (в блоках) ---
    public static final float SMOKE_RADIUS = 6.5F;  // 8 блоков в ширину
    public static final float SMOKE_HEIGHT = 3.0F;  // 2.5 блока в высоту

    // --- Дымный шлейф в полёте ---
    private static final int TRAIL_SPAWN_INTERVAL = 2;
    public static final int TRAIL_LIFE_TICKS = 80; // 4 сек
    private static final int TRAIL_MAX = 60;
    private static final double TRAIL_MIN_SPEED_SQ = 0.05 * 0.05;

    // --- Искры у корпуса, пока шашка дымит ---
    private static final int SPARK_SPAWN_INTERVAL = 2;
    private static final int SPARK_PER_SPAWN_MIN = 1;
    private static final int SPARK_PER_SPAWN_MAX = 2;
    private static final int SPARK_LIFE_MIN = 8;
    private static final int SPARK_LIFE_MAX = 18;
    private static final int SPARK_MAX = 40;
    private static final double SPARK_SPEED_MIN = 0.02;
    private static final double SPARK_SPEED_MAX = 0.09;
    private static final double SPARK_GRAVITY = 0.0035;
    private static final double SPARK_ORIGIN_RADIUS = 0.14;

    private static final EntityDataAccessor<Integer> DATA_SMOKE_TICKS =
            SynchedEntityData.defineId(M18Entity.class, EntityDataSerializers.INT);
    private static final int SYNC_INTERVAL = 20;

    private int smokeTicks = -1;
    private int lastSyncedSmokeTicks = -1;
    private boolean clientSoundStarted = false;

    private final ArrayDeque<TrailPuff> trail = new ArrayDeque<>();
    private int trailTimer = 0;

    private final ArrayDeque<Spark> sparks = new ArrayDeque<>();
    private int sparkTimer = 0;
    private final Random clientRandom = new Random();

    private int fuseTimer;
    private int ticksAlive = 0;

    // --- Анимация броска/полёта (то, чего нет у РДГ-2) ---
    private static final RawAnimation ANIM_THROW =
            RawAnimation.begin().thenPlayAndHold("animation.grenade.throw");

    private static final RawAnimation ANIM_FLY =
            RawAnimation.begin().thenLoop("animation.grenade.fly");

    private static final int THROW_ANIM_TICKS = 15;

    // --- Физика отскоков (как у остальных гранат мода) ---
    private static final double BASE_RESTITUTION        = 0.21;
    private static final double RESTITUTION_DECAY       = 0.80;
    private static final double MIN_RESTITUTION         = 0.10;
    private static final double BOUNCE_TANGENT_FRICTION = 0.55;
    private static final double SETTLE_SPEED_SQ         = 0.03 * 0.03;
    private static final double BOUNCE_PUSH_OUT         = 0.06;

    private static final float SPIN_DAMPING         = 0.985f;
    private static final float SPIN_DAMPING_ON_HIT  = 0.9f;
    private static final float SPIN_KICK_DEGREES    = 70f;
    private float pitchSpin;
    private float yawSpin;

    private boolean settled = false;
    private int bounceCount = 0;

    public M18Entity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
        initFuse();
    }

    public M18Entity(Level level, LivingEntity shooter) {
        super(ModEntities.M_18_PROJECTILE.get(), shooter, level);
        initFuse();
    }

    private void initFuse() {
        this.fuseTimer = SMOKE_DELAY_TICKS;

        Random rand = new Random();
        this.pitchSpin = (rand.nextFloat() - 0.5f) * 50f;
        this.yawSpin   = (rand.nextFloat() - 0.5f) * 50f;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SMOKE_TICKS, -1);
    }

    @Override
    protected float getGravity() { return 0.05F; }

    // ------------------------------------------------------------------
    // Состояние дыма
    // ------------------------------------------------------------------

    public boolean isSmoking() {
        return this.smokeTicks >= 0;
    }

    public float getSmokeTicks(float partialTick) {
        if (this.smokeTicks < 0) return -1.0F;
        return this.smokeTicks + partialTick;
    }

    public float getSmokeDensity(float partialTick) {
        float t = getSmokeTicks(partialTick);
        if (t < 0.0F) return 0.0F;
        if (t < GROW_TICKS) return t / GROW_TICKS;
        if (t < GROW_TICKS + HOLD_TICKS) return 1.0F;
        float f = (t - GROW_TICKS - HOLD_TICKS) / FADE_TICKS;
        return Math.max(0.0F, 1.0F - f);
    }

    public float getSmokeGrowth(float partialTick) {
        float t = getSmokeTicks(partialTick);
        if (t < 0.0F) return 0.0F;
        return Math.min(1.0F, t / GROW_TICKS);
    }

    public Iterable<TrailPuff> getTrail() {
        return this.trail;
    }

    public Iterable<Spark> getSparks() {
        return this.sparks;
    }

    public static final class TrailPuff {
        public final double x, y, z;
        public final int seed;
        public int age;

        TrailPuff(double x, double y, double z, int seed) {
            this.x = x; this.y = y; this.z = z; this.seed = seed;
        }
    }

    public static final class Spark {
        public double x, y, z;
        public double vx, vy, vz;
        public final int life;
        public final int seed;
        public int age;

        Spark(double x, double y, double z, double vx, double vy, double vz, int life, int seed) {
            this.x = x; this.y = y; this.z = z;
            this.vx = vx; this.vy = vy; this.vz = vz;
            this.life = life; this.seed = seed;
        }
    }

    // ------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        ticksAlive++;

        // Пока крутит throw-анимация — не мешаем ей своим вращением хитбокса,
        // как это сделано у M67/RGD-5/RGO.
        if (!settled && ticksAlive > THROW_ANIM_TICKS) {
            this.setXRot(wrapDegrees(this.getXRot() + pitchSpin));
            this.setYRot(wrapDegrees(this.getYRot() + yawSpin));

            pitchSpin *= SPIN_DAMPING;
            yawSpin   *= SPIN_DAMPING;
        }

        if (this.level().isClientSide) {
            tickClientSmoke();
            return;
        }

        // --- сервер ---
        if (!isSmoking()) {
            this.fuseTimer--;
            if (this.fuseTimer <= 0) {
                this.smokeTicks = 0;
                this.entityData.set(DATA_SMOKE_TICKS, 0);
            }
        } else {
            this.smokeTicks++;
            if (this.smokeTicks % SYNC_INTERVAL == 0) {
                this.entityData.set(DATA_SMOKE_TICKS, this.smokeTicks);
            }
            if (this.smokeTicks >= TOTAL_SMOKE_TICKS) {
                this.discard();
            }
        }
    }

    private void tickClientSmoke() {
        int synced = this.entityData.get(DATA_SMOKE_TICKS);

        if (synced < 0) {
            this.smokeTicks = -1;
        } else if (synced != this.lastSyncedSmokeTicks) {
            this.lastSyncedSmokeTicks = synced;
            this.smokeTicks = synced;
        } else if (this.smokeTicks >= 0) {
            this.smokeTicks++;
        } else {
            this.smokeTicks = synced;
        }

        if (isSmoking() && !clientSoundStarted) {
            clientSoundStarted = true;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.danilfb123.aasgranate.client.M18ClientSound.start(this));
        }

        tickTrail();
        tickSparks();
    }

    private void tickTrail() {
        for (TrailPuff puff : this.trail) {
            puff.age++;
        }
        while (!this.trail.isEmpty() && this.trail.peekFirst().age > TRAIL_LIFE_TICKS) {
            this.trail.pollFirst();
        }

        if (!isSmoking()) return;

        Vec3 v = this.getDeltaMovement();
        if (v.lengthSqr() < TRAIL_MIN_SPEED_SQ) return;

        if (++this.trailTimer < TRAIL_SPAWN_INTERVAL) return;
        this.trailTimer = 0;

        this.trail.addLast(new TrailPuff(this.getX(), this.getY(), this.getZ(),
                this.getId() * 7919 + this.tickCount * 131));

        while (this.trail.size() > TRAIL_MAX) {
            this.trail.pollFirst();
        }
    }

    private void tickSparks() {
        for (Spark spark : this.sparks) {
            spark.age++;
            spark.vy -= SPARK_GRAVITY;
            spark.x += spark.vx;
            spark.y += spark.vy;
            spark.z += spark.vz;
        }
        this.sparks.removeIf(sp -> sp.age >= sp.life);

        if (!isSmoking()) return;

        if (++this.sparkTimer < SPARK_SPAWN_INTERVAL) return;
        this.sparkTimer = 0;

        int count = SPARK_PER_SPAWN_MIN
                + clientRandom.nextInt(SPARK_PER_SPAWN_MAX - SPARK_PER_SPAWN_MIN + 1);

        for (int i = 0; i < count; i++) {
            double ox = (clientRandom.nextDouble() - 0.5) * 2.0 * SPARK_ORIGIN_RADIUS;
            double oy = (clientRandom.nextDouble() - 0.5) * 2.0 * SPARK_ORIGIN_RADIUS;
            double oz = (clientRandom.nextDouble() - 0.5) * 2.0 * SPARK_ORIGIN_RADIUS;

            double speed = SPARK_SPEED_MIN + clientRandom.nextDouble() * (SPARK_SPEED_MAX - SPARK_SPEED_MIN);
            double theta = clientRandom.nextDouble() * Math.PI * 2.0;
            double horiz = speed * (0.4 + 0.6 * clientRandom.nextDouble());

            double vx = Math.cos(theta) * horiz;
            double vz = Math.sin(theta) * horiz;
            double vy = speed * (0.5 + 0.9 * clientRandom.nextDouble());

            int life = SPARK_LIFE_MIN + clientRandom.nextInt(SPARK_LIFE_MAX - SPARK_LIFE_MIN + 1);

            this.sparks.addLast(new Spark(
                    this.getX() + ox, this.getY() + 0.05 + oy, this.getZ() + oz,
                    vx, vy, vz, life, clientRandom.nextInt()));
        }

        while (this.sparks.size() > SPARK_MAX) {
            this.sparks.pollFirst();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (settled) return;

        Direction face = result.getDirection();
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

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(
                SMOKE_RADIUS + 4.0, SMOKE_HEIGHT + 2.0, SMOKE_RADIUS + 4.0);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FuseTimer", this.fuseTimer);
        tag.putInt("SmokeTicks", this.smokeTicks);
        tag.putInt("TicksAlive", this.ticksAlive);
        tag.putBoolean("Settled", this.settled);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FuseTimer"))  this.fuseTimer  = tag.getInt("FuseTimer");
        if (tag.contains("TicksAlive")) this.ticksAlive = tag.getInt("TicksAlive");
        if (tag.contains("Settled"))    this.settled    = tag.getBoolean("Settled");
        if (tag.contains("SmokeTicks")) {
            this.smokeTicks = tag.getInt("SmokeTicks");
            this.entityData.set(DATA_SMOKE_TICKS, this.smokeTicks);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<M18Entity> event) {
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
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

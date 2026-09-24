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
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayDeque;
import java.util.Random;

/**
 * РДГ-2 — ручная дымовая граната.
 *
 * Отличия от боевых гранат мода:
 *  - нет взрыва, нет осколков, нет урона и нет звука взрыва;
 *  - шашка загорается сразу в момент броска и дымит уже в полёте, оставляя за собой
 *    шлейф (см. трейл ниже);
 *  - пока шашка дымит, из её корпуса летят искры (процедурные, без текстур);
 *  - пока граната дымит, клиент проигрывает зацикленный звук горения (rdg2_smoke_loop);
 *  - дым рисуется процедурной геометрией (client/SmokeCloudRenderer), без единой
 *    текстуры, поэтому его нельзя отключить или подменить ресурспаком;
 *  - анимаций у летящей шашки нет вообще (ни throw, ни fly) — она просто летит и крутится.
 *
 * Тайминги (20 тиков = 1 секунда):
 *  - GROW_TICKS = 200  — раскрытие облака до полного объёма за 10 секунд;
 *  - HOLD_TICKS = 1200 — полный дым держится минуту;
 *  - FADE_TICKS = 200  — затухание за 10 секунд, затем граната удаляется.
 *
 * Габариты облака: SMOKE_RADIUS * 2 в ширину (4 блока) и SMOKE_HEIGHT в высоту (2.5 блока).
 */
public class Rdg2Entity extends ThrowableProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // --- Тайминги дыма ---
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
    // Радиус удвоен по просьбе: было 2.0 (4 блока в ширину), стало 4.0 (8 блоков в ширину).
    public static final float SMOKE_RADIUS = 6.5F;  // 8 блоков в ширину
    public static final float SMOKE_HEIGHT = 3.0F;  // 2.5 блока в высоту

    // --- Дымный шлейф в полёте ---
    /** Через сколько тиков ставится новый клуб шлейфа. */
    private static final int TRAIL_SPAWN_INTERVAL = 2;
    /** Сколько живёт один клуб шлейфа. */
    public static final int TRAIL_LIFE_TICKS = 80; // 4 сек
    /** Максимум клубов шлейфа в памяти. */
    private static final int TRAIL_MAX = 60;
    /** Ниже этой скорости шлейф не ставится (шашка уже лежит). */
    private static final double TRAIL_MIN_SPEED_SQ = 0.05 * 0.05;

    // --- Искры у корпуса, пока шашка дымит ---
    /** Раз в сколько тиков пытаемся выпустить новую искру. */
    private static final int SPARK_SPAWN_INTERVAL = 2;
    /** Сколько искр рождается за один спавн (1 или 2). */
    private static final int SPARK_PER_SPAWN_MIN = 1;
    private static final int SPARK_PER_SPAWN_MAX = 2;
    /** Минимальная/максимальная жизнь одной искры, тиков. */
    private static final int SPARK_LIFE_MIN = 8;
    private static final int SPARK_LIFE_MAX = 18;
    /** Максимум искр одновременно в памяти (на случай лагов с накоплением). */
    private static final int SPARK_MAX = 40;
    /** Начальная скорость искры, блоков/тик. */
    private static final double SPARK_SPEED_MIN = 0.02;
    private static final double SPARK_SPEED_MAX = 0.09;
    /** Гравитация искры (ускорение вниз за тик). */
    private static final double SPARK_GRAVITY = 0.0035;
    /** Искры вылетают из случайной точки в этом радиусе от центра шашки. */
    private static final double SPARK_ORIGIN_RADIUS = 0.14;

    /**
     * Сколько тиков граната уже дымит. -1 = ещё не сработала.
     * На сервере значение считается каждый тик, клиенту отправляется раз в SYNC_INTERVAL
     * тиков, между отправками клиент докручивает счётчик сам.
     */
    private static final EntityDataAccessor<Integer> DATA_SMOKE_TICKS =
            SynchedEntityData.defineId(Rdg2Entity.class, EntityDataSerializers.INT);
    private static final int SYNC_INTERVAL = 20;

    private int smokeTicks = -1;
    private int lastSyncedSmokeTicks = -1;
    private boolean clientSoundStarted = false;

    /** Шлейф существует только на клиенте — синхронизировать его не нужно. */
    private final ArrayDeque<TrailPuff> trail = new ArrayDeque<>();
    private int trailTimer = 0;

    /** Искры — тоже чисто клиентский, чисто визуальный эффект. */
    private final ArrayDeque<Spark> sparks = new ArrayDeque<>();
    private int sparkTimer = 0;
    private final Random clientRandom = new Random();

    private int fuseTimer;
    private int ticksAlive = 0;

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

    public Rdg2Entity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
        initFuse();
    }

    public Rdg2Entity(Level level, LivingEntity shooter) {
        super(ModEntities.RDG_2_PROJECTILE.get(), shooter, level);
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

    /** Сколько тиков идёт дымление, с учётом partialTick (для плавного рендера). */
    public float getSmokeTicks(float partialTick) {
        if (this.smokeTicks < 0) return -1.0F;
        return this.smokeTicks + partialTick;
    }

    /**
     * Плотность/прозрачность облака: 0 — дыма нет, 1 — полный дым.
     * Растёт GROW_TICKS, держится HOLD_TICKS, падает за FADE_TICKS.
     */
    public float getSmokeDensity(float partialTick) {
        float t = getSmokeTicks(partialTick);
        if (t < 0.0F) return 0.0F;
        if (t < GROW_TICKS) return t / GROW_TICKS;
        if (t < GROW_TICKS + HOLD_TICKS) return 1.0F;
        float f = (t - GROW_TICKS - HOLD_TICKS) / FADE_TICKS;
        return Math.max(0.0F, 1.0F - f);
    }

    /** Насколько облако раскрылось по объёму: 0..1 (за GROW_TICKS). */
    public float getSmokeGrowth(float partialTick) {
        float t = getSmokeTicks(partialTick);
        if (t < 0.0F) return 0.0F;
        return Math.min(1.0F, t / GROW_TICKS);
    }

    /** Клубы шлейфа для рендера (только клиент). */
    public Iterable<TrailPuff> getTrail() {
        return this.trail;
    }

    /** Искры для рендера (только клиент). */
    public Iterable<Spark> getSparks() {
        return this.sparks;
    }

    /** Один клуб дымного шлейфа: точка в мире + возраст. */
    public static final class TrailPuff {
        public final double x, y, z;
        public final int seed;
        public int age;

        TrailPuff(double x, double y, double z, int seed) {
            this.x = x; this.y = y; this.z = z; this.seed = seed;
        }
    }

    /** Одна искра: точка в мире, скорость и возраст/срок жизни. */
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

        if (!settled) {
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

    /** На клиенте счётчик докручивается локально, а при приходе пакета — синхронизируется. */
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
                    () -> () -> com.danilfb123.aasgranate.client.Rdg2ClientSound.start(this));
        }

        tickTrail();
        tickSparks();
    }

    /**
     * Дымный шлейф: пока шашка летит/катится, раз в TRAIL_SPAWN_INTERVAL тиков в текущей
     * точке остаётся клуб дыма, который живёт TRAIL_LIFE_TICKS тиков, разрастается и тает.
     */
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

    /**
     * Искры у корпуса шашки, пока она дымит. Живут своей маленькой физикой:
     * вылетают в случайную сторону, гравитация тянет их вниз, гаснут по возрасту.
     * Полностью визуальный, детерминизм и синхронизация не нужны — на каждом
     * клиенте искры свои, это невидимая на глаз разница.
     */
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
            double vy = speed * (0.5 + 0.9 * clientRandom.nextDouble()); // искра "выстреливает" вверх/в сторону

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

    /**
     * Бокс, по которому игра решает, рисовать сущность или она за краем экрана.
     * Хитбокс шашки крошечный, а облако со шлейфом — несколько блоков, поэтому расширяем,
     * иначе дым будет пропадать, когда сама шашка уходит из кадра.
     */
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

    // У дымовой шашки анимаций нет — контроллер нужен только потому, что этого
    // требует GeckoLib для GeoEntity.
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<Rdg2Entity> event) {
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
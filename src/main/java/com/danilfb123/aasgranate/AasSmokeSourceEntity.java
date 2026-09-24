package com.danilfb123.aasgranate;

import com.danilfb123.aasgranate.api.SmokeCloudEmitter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Универсальный "источник" объёмного дыма — то же самое облако, что рисуется
 * у дымовых шашек (M18/RDG-2), но без гранаты/анимации/осколков. Сторонние
 * моды не создают эту сущность напрямую, а используют com.danilfb123.aasgranate.api.AasSmokeApi.
 *
 * Два режима использования (оба — просто эта же сущность):
 *
 *  1. "Разовое" облако (дымовая шашка/дымогенератор гранатомёта техники):
 *     AasSmokeApi.spawnSmokeCloud(...) один раз с фиксированным holdTicks —
 *     растёт → держится holdTicks → гаснет, точь-в-точь как M18Entity.
 *
 *  2. "Постоянный" дым, пока работает генератор (например, дымогенератор
 *     двигателя БМП/БТР): AasSmokeApi.spawnSmokeCloud(...) один раз, а затем
 *     AasSmokeApi.keepAlive(cloud) на каждом тике, пока генератор включён.
 *     keepAlive постоянно отодвигает "конец удержания" на небольшой запас
 *     вперёд; как только вызовы прекращаются (генератор выключен/техника
 *     уничтожена), запас истекает и через ~KEEPALIVE_GRACE_TICKS начинается
 *     обычное затухание — отдельного вызова "stop" не нужно.
 *
 * Может следовать за другой сущностью (например, за техникой) — тогда центр
 * облака на сервере каждый тик пересчитывается из позиции/поворота носителя
 * плюс локальное смещение. Если носитель исчез (уничтожен/выгружен), облако
 * просто перестаёт двигаться и доигрывает удержание/затухание на месте —
 * это нормально и выглядит реалистично (дым не исчезает мгновенно вместе с техникой).
 */
public class AasSmokeSourceEntity extends Entity implements SmokeCloudEmitter {

    /** Сколько тиков "про запас" даёт один вызов keepAlive() поверх текущего возраста облака. */
    public static final int KEEPALIVE_GRACE_TICKS = 40; // 2 сек

    private static final EntityDataAccessor<Integer> DATA_SMOKE_TICKS =
            SynchedEntityData.defineId(AasSmokeSourceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(AasSmokeSourceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(AasSmokeSourceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_GROW_TICKS =
            SynchedEntityData.defineId(AasSmokeSourceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HOLD_TICKS =
            SynchedEntityData.defineId(AasSmokeSourceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FADE_TICKS =
            SynchedEntityData.defineId(AasSmokeSourceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CARRIER_ID =
            SynchedEntityData.defineId(AasSmokeSourceEntity.class, EntityDataSerializers.INT);

    private static final int SYNC_INTERVAL = 10;

    // --- серверные (не синхронизируемые напрямую) поля ---
    private int smokeTicks = 0;
    private Vec3 carrierLocalOffset = Vec3.ZERO;

    // --- клиентские поля (сглаженное локальное продолжение тиков между синками) ---
    private int lastSyncedSmokeTicks = -1;

    public AasSmokeSourceEntity(EntityType<? extends AasSmokeSourceEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SMOKE_TICKS, 0);
        this.entityData.define(DATA_RADIUS, 6.5F);
        this.entityData.define(DATA_HEIGHT, 3.0F);
        this.entityData.define(DATA_GROW_TICKS, 600);
        this.entityData.define(DATA_HOLD_TICKS, 1200);
        this.entityData.define(DATA_FADE_TICKS, 200);
        this.entityData.define(DATA_CARRIER_ID, -1);
    }

    /** Вызывается сервером сразу после addFreshEntity(), см. AasSmokeApi. */
    public void configure(float radius, float height, int growTicks, int holdTicks, int fadeTicks) {
        this.entityData.set(DATA_RADIUS, radius);
        this.entityData.set(DATA_HEIGHT, height);
        this.entityData.set(DATA_GROW_TICKS, growTicks);
        this.entityData.set(DATA_HOLD_TICKS, holdTicks);
        this.entityData.set(DATA_FADE_TICKS, fadeTicks);
    }

    /** Привязать облако к движущемуся носителю (например, к технике). offset — локальные координаты относительно его позиции/поворота. */
    public void attachToCarrier(Entity carrier, Vec3 localOffset) {
        this.entityData.set(DATA_CARRIER_ID, carrier.getId());
        this.carrierLocalOffset = localOffset;
    }

    /** Продлить удержание облака ещё немного вперёд. Вызывать каждый тик, пока источник (генератор/шашка) активен. */
    public void keepAlive() {
        if (this.level().isClientSide) return;
        int growTicks = this.entityData.get(DATA_GROW_TICKS);
        int desiredHold = Math.max(this.entityData.get(DATA_HOLD_TICKS),
                (this.smokeTicks - growTicks) + KEEPALIVE_GRACE_TICKS);
        this.entityData.set(DATA_HOLD_TICKS, desiredHold);
    }

    // ------------------------------------------------------------------
    // SmokeCloudEmitter
    // ------------------------------------------------------------------

    @Override
    public boolean isSmoking() {
        return !this.isRemoved();
    }

    @Override
    public float getSmokeTicks(float partialTick) {
        return this.smokeTicks + partialTick;
    }

    @Override
    public float getSmokeDensity(float partialTick) {
        float t = getSmokeTicks(partialTick);
        int growTicks = this.entityData.get(DATA_GROW_TICKS);
        int holdTicks = this.entityData.get(DATA_HOLD_TICKS);
        int fadeTicks = Math.max(1, this.entityData.get(DATA_FADE_TICKS));
        if (t < growTicks) return t / growTicks;
        if (t < growTicks + holdTicks) return 1.0F;
        float f = (t - growTicks - holdTicks) / fadeTicks;
        return Math.max(0.0F, 1.0F - f);
    }

    @Override
    public float getSmokeGrowth(float partialTick) {
        float t = getSmokeTicks(partialTick);
        int growTicks = this.entityData.get(DATA_GROW_TICKS);
        return Math.min(1.0F, t / growTicks);
    }

    @Override
    public float getSmokeRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    @Override
    public float getSmokeHeight() {
        return this.entityData.get(DATA_HEIGHT);
    }

    // ------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            tickServer();
        } else {
            tickClient();
        }
    }

    private void tickServer() {
        int carrierId = this.entityData.get(DATA_CARRIER_ID);
        if (carrierId >= 0) {
            Entity carrier = this.level().getEntity(carrierId);
            if (carrier != null && carrier.isAlive()) {
                float yawRad = -carrier.getYRot() * ((float) Math.PI / 180F);
                double ox = carrierLocalOffset.x * Math.cos(yawRad) - carrierLocalOffset.z * Math.sin(yawRad);
                double oz = carrierLocalOffset.x * Math.sin(yawRad) + carrierLocalOffset.z * Math.cos(yawRad);
                this.setPos(carrier.getX() + ox, carrier.getY() + carrierLocalOffset.y, carrier.getZ() + oz);
            } else {
                // Носитель пропал — перестаём гоняться за ним, дым доигрывает на месте.
                this.entityData.set(DATA_CARRIER_ID, -1);
            }
        }

        this.smokeTicks++;
        if (this.smokeTicks % SYNC_INTERVAL == 0) {
            this.entityData.set(DATA_SMOKE_TICKS, this.smokeTicks);
        }

        int total = this.entityData.get(DATA_GROW_TICKS)
                + this.entityData.get(DATA_HOLD_TICKS)
                + this.entityData.get(DATA_FADE_TICKS);
        if (this.smokeTicks >= total) {
            this.discard();
        }
    }

    private void tickClient() {
        int synced = this.entityData.get(DATA_SMOKE_TICKS);
        if (synced != this.lastSyncedSmokeTicks) {
            this.lastSyncedSmokeTicks = synced;
            this.smokeTicks = synced;
        } else {
            this.smokeTicks++;
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        float radius = getSmokeRadius();
        float height = getSmokeHeight();
        return this.getBoundingBox().inflate(radius + 4.0, height + 2.0, radius + 4.0);
    }

    // Маркер: не толкается, не подбирает урон, не коллизирует.
    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    protected boolean canAddPassenger(Entity passenger) { return false; }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("SmokeTicks", this.smokeTicks);
        tag.putFloat("Radius", this.entityData.get(DATA_RADIUS));
        tag.putFloat("Height", this.entityData.get(DATA_HEIGHT));
        tag.putInt("GrowTicks", this.entityData.get(DATA_GROW_TICKS));
        tag.putInt("HoldTicks", this.entityData.get(DATA_HOLD_TICKS));
        tag.putInt("FadeTicks", this.entityData.get(DATA_FADE_TICKS));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("SmokeTicks")) this.smokeTicks = tag.getInt("SmokeTicks");
        if (tag.contains("Radius")) this.entityData.set(DATA_RADIUS, tag.getFloat("Radius"));
        if (tag.contains("Height")) this.entityData.set(DATA_HEIGHT, tag.getFloat("Height"));
        if (tag.contains("GrowTicks")) this.entityData.set(DATA_GROW_TICKS, tag.getInt("GrowTicks"));
        if (tag.contains("HoldTicks")) this.entityData.set(DATA_HOLD_TICKS, tag.getInt("HoldTicks"));
        if (tag.contains("FadeTicks")) this.entityData.set(DATA_FADE_TICKS, tag.getInt("FadeTicks"));
        this.entityData.set(DATA_SMOKE_TICKS, this.smokeTicks);
    }
}

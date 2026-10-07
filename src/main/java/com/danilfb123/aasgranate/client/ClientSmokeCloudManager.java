package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.AasGranate;
import com.danilfb123.aasgranate.M18Entity;
import com.danilfb123.aasgranate.Rdg2Entity;
import com.danilfb123.aasgranate.network.SmokeCloudSyncPacket.Kind;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр дымовых облаков на клиенте, полностью независимый от того, трекается
 * ли сейчас сама сущность-источник дыма. Наполняется из SmokeCloudSyncPacket,
 * который сервер рассылает через SmokeCloudBroadcaster с собственным,
 * большим радиусом (SmokeCloudBroadcaster.SYNC_RADIUS), не зависящим от
 * entity-broadcast-range-percentage / clientTrackingRange.
 *
 * Явного пакета "удалить облако" не нужно: облако само угасает по локальному
 * таймеру (те же GROW/HOLD/FADE_TICKS, что и у настоящей сущности), так что
 * пропадает оно ровно тогда же, когда исчезло бы и раньше — просто больше не
 * зависит от того, продолжает ли сервер считать сущность "видимой" данному
 * игроку.
 */
public final class ClientSmokeCloudManager {

    /** Сколько тиков без ре-синка ждём, прежде чем считать запись протухшей (страховка от "зависших" облаков). */
    private static final int STALE_AFTER_TICKS = 200; // ~10 секунд

    public static final class Cloud {
        public final Kind kind;
        public double x, y, z;
        public double xo, yo, zo;
        public int smokeTicks;
        private int ticksSinceSync;

        private Cloud(Kind kind, double x, double y, double z, int smokeTicks) {
            this.kind = kind;
            this.x = this.xo = x;
            this.y = this.yo = y;
            this.z = this.zo = z;
            this.smokeTicks = smokeTicks;
        }

        private void applySync(double x, double y, double z, int smokeTicks) {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            this.x = x;
            this.y = y;
            this.z = z;
            this.smokeTicks = smokeTicks;
            this.ticksSinceSync = 0;
        }

        private int growTicks() { return kind == Kind.M18 ? M18Entity.GROW_TICKS : Rdg2Entity.GROW_TICKS; }
        private int holdTicks() { return kind == Kind.M18 ? M18Entity.HOLD_TICKS : Rdg2Entity.HOLD_TICKS; }
        private int fadeTicks() { return kind == Kind.M18 ? M18Entity.FADE_TICKS : Rdg2Entity.FADE_TICKS; }
        private int totalTicks() { return growTicks() + holdTicks() + fadeTicks(); }

        public float getSmokeTicks(float partialTick) {
            return smokeTicks + partialTick;
        }

        public float getSmokeDensity(float partialTick) {
            float t = getSmokeTicks(partialTick);
            int grow = growTicks(), hold = holdTicks(), fade = Math.max(1, fadeTicks());
            if (t < grow) return t / grow;
            if (t < grow + hold) return 1.0F;
            float f = (t - grow - hold) / fade;
            return Math.max(0.0F, 1.0F - f);
        }

        public float getSmokeGrowth(float partialTick) {
            float t = getSmokeTicks(partialTick);
            return Math.min(1.0F, t / growTicks());
        }

        public float getRadius() { return kind == Kind.M18 ? M18Entity.SMOKE_RADIUS : Rdg2Entity.SMOKE_RADIUS; }
        public float getHeight() { return kind == Kind.M18 ? M18Entity.SMOKE_HEIGHT : Rdg2Entity.SMOKE_HEIGHT; }

        private boolean isExpired() {
            return smokeTicks >= totalTicks() || ticksSinceSync > STALE_AFTER_TICKS;
        }
    }

    private static final Map<Integer, Cloud> CLOUDS = new ConcurrentHashMap<>();

    private ClientSmokeCloudManager() {}

    public static void sync(int entityId, Kind kind, double x, double y, double z, int smokeTicks) {
        CLOUDS.compute(entityId, (id, existing) -> {
            if (existing == null) return new Cloud(kind, x, y, z, smokeTicks);
            existing.applySync(x, y, z, smokeTicks);
            return existing;
        });
    }

    /** Все текущие облака, ключ — id сущности-источника (может уже не существовать на клиенте). */
    public static Map<Integer, Cloud> all() {
        return CLOUDS;
    }

    public static void clear() {
        CLOUDS.clear();
    }

    @Mod.EventBusSubscriber(modid = AasGranate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Ticker {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            CLOUDS.entrySet().removeIf(e -> {
                Cloud c = e.getValue();
                c.smokeTicks++;
                c.ticksSinceSync++;
                if (c.isExpired()) {
                    DetachedSmokeCloudRenderer.onCloudRemoved(e.getKey());
                    return true;
                }
                return false;
            });
        }
    }
}
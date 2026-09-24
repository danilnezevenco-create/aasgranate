package com.danilfb123.aasgranate;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.loading.FMLEnvironment;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class Rgd5Item extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation ANIM_PULL =
            RawAnimation.begin().thenPlayAndHold("animation.grenade.pull_pin");

    private static final RawAnimation ANIM_THROW =
            RawAnimation.begin().thenPlayAndHold("animation.grenade.throw");

    private static final int THROW_COOLDOWN_TICKS = 20;

    private static final String TAG_PULL_START  = "PullStartTick";
    private static final int PULL_STALE_TICKS = 1200;

    // --- Сила броска в зависимости от времени удержания ---
    // Чем дольше игрок держит ПКМ (до MAX_CHARGE_TICKS), тем сильнее (дальше) летит граната.
    // На 2 секундах (40 тиков) и дальше — полная сила, как раньше (MAX_THROW_VELOCITY = 1.5F).
    // При мгновенном отпускании (сразу после порога отмены броска) — половина силы.
    private static final int   MAX_CHARGE_TICKS    = 40;   // 2 секунды = 40 тиков (20 тиков/сек)
    private static final float MAX_THROW_VELOCITY  = 2F; // прежнее фиксированное значение
    private static final float MIN_THROW_VELOCITY  = 0.75F; // минимум — половина от максимума

    public Rgd5Item(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        CompoundTag tag = stack.getOrCreateTag();
        // РЈР”РђР›РР›Р: tag.remove(TAG_THROW_START);
        tag.putLong(TAG_PULL_START, level.getGameTime());

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.RGD5_PIN_PULL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (!(entityLiving instanceof Player player)) return;

        int holdTicks = this.getUseDuration(stack) - timeLeft;

        if (holdTicks < 10) {
            CompoundTag cancelTag = stack.getTag();
            if (cancelTag != null) cancelTag.remove(TAG_PULL_START);
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.remove(TAG_PULL_START);
        // МЫ УДАЛИЛИ СТРОКУ, КОТОРАЯ ЗАПУСКАЛА АНИМАЦИЮ БРОСКА В РУКЕ

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.RGD5_THROW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        if (!level.isClientSide) {
            Rgd5Entity projectile = new Rgd5Entity(level, player);

            // Сила броска растёт линейно от MIN_THROW_VELOCITY до MAX_THROW_VELOCITY
            // по мере удержания кнопки, максимум достигается за MAX_CHARGE_TICKS (2 сек).
            float chargeFraction = Math.min(holdTicks, MAX_CHARGE_TICKS) / (float) MAX_CHARGE_TICKS;
            float throwVelocity = MIN_THROW_VELOCITY + (MAX_THROW_VELOCITY - MIN_THROW_VELOCITY) * chargeFraction;

            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, throwVelocity, 1.0F);
            level.addFreshEntity(projectile);
        }

        if (!player.getAbilities().instabuild) stack.shrink(1);

        player.getCooldowns().addCooldown(this, THROW_COOLDOWN_TICKS);
    }

    @Override
    public int getUseDuration(ItemStack stack) { return 72000; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                // РРЅРёС†РёР°Р»РёР·РёСЂСѓРµРј СЂРµРЅРґРµСЂ РўРћР›Р¬РљРћ РєРѕРіРґР° РѕРЅ СЂРµР°Р»СЊРЅРѕ РЅСѓР¶РµРЅ (РЅР° РєР»РёРµРЅС‚Рµ)
                if (this.renderer == null) {
                    this.renderer = new com.danilfb123.aasgranate.client.Rgd5ItemRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, this::predicate));
    }

    private PlayState predicate(AnimationState<Rgd5Item> event) {
        ItemStack stack = event.getData(DataTickets.ITEMSTACK);

        // Р•СЃР»Рё РїСЂРµРґРјРµС‚Р° РЅРµС‚, СЃР±СЂР°СЃС‹РІР°РµРј РїР»РµРµСЂ
        if (stack == null || stack.isEmpty()) {
            event.getController().forceAnimationReset();
            return PlayState.STOP;
        }

        CompoundTag tag = stack.getTag();

        // РџСЂРѕРІРµСЂСЏРµРј, Р·Р°Р¶Р°Р» Р»Рё РёРіСЂРѕРє РєРЅРѕРїРєСѓ (РµСЃС‚СЊ Р»Рё С‚РµРі)
        if (tag != null && tag.contains(TAG_PULL_START)) {
            long pullStart = tag.getLong(TAG_PULL_START);
            if (clientGameTime() - pullStart < PULL_STALE_TICKS) {
                // РљРЅРѕРїРєР° Р·Р°Р¶Р°С‚Р° вЂ” РёРіСЂР°РµРј Р°РЅРёРјР°С†РёСЋ
                return event.setAndContinue(ANIM_PULL);
            }
        }

        // Р’РђР–РќРћ: Р•СЃР»Рё С‡РµРєР° РЅРµ РІС‹РґРµСЂРЅСѓС‚Р° (РёРіСЂРѕРє РѕС‚РїСѓСЃС‚РёР» РєРЅРѕРїРєСѓ),
        // РїСЂРёРЅСѓРґРёС‚РµР»СЊРЅРѕ РЎР‘Р РђРЎР«Р’РђР•Рњ (РїРµСЂРµРјР°С‚С‹РІР°РµРј) Р°РЅРёРјР°С†РёСЋ РІ СЃР°РјРѕРµ РЅР°С‡Р°Р»Рѕ.
        // Р‘Р»Р°РіРѕРґР°СЂСЏ СЌС‚РѕРјСѓ СЃР»РµРґСѓСЋС‰РёР№ Р±СЂРѕСЃРѕРє СЃРЅРѕРІР° РїСЂРѕРёРіСЂР°РµС‚СЃСЏ СЃ 0-РіРѕ РєР°РґСЂР°.
        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    private long clientGameTime() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            // Р‘РµР·РѕРїР°СЃРЅС‹Р№ РІС‹Р·РѕРІ РІСЂРµРјРµРЅРё С‡РµСЂРµР· С‡РёСЃС‚Рѕ РєР»РёРµРЅС‚СЃРєРёР№ РєР»Р°СЃСЃ
            return com.danilfb123.aasgranate.client.ClientHitboxDebugTracker.getClientGameTime();
        }
        return 0L;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
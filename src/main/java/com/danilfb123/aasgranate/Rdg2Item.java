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

public class Rdg2Item extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation ANIM_PULL =
            RawAnimation.begin().thenPlayAndHold("animation.grenade.pull_pin");

    private static final int THROW_COOLDOWN_TICKS = 20;

    private static final String TAG_PULL_START  = "PullStartTick";
    private static final int PULL_STALE_TICKS = 1200;

    // --- Подготовка к броску ---
    // Дымовую шашку нужно подготовить: пока игрок держит ПКМ меньше PREPARE_TICKS,
    // бросок не происходит (отпустил раньше — бросок отменён).
    private static final int PREPARE_TICKS = 20; // 1 секунда

    // --- Сила броска ---
    // Отсчитывается уже ПОСЛЕ подготовки: ещё до 2 секунд удержания сила растёт
    // линейно от MIN_THROW_VELOCITY до MAX_THROW_VELOCITY.
    private static final int   MAX_CHARGE_TICKS   = 40;    // 2 секунды
    private static final float MAX_THROW_VELOCITY = 2F;
    private static final float MIN_THROW_VELOCITY = 0.75F;

    public Rdg2Item(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(TAG_PULL_START, level.getGameTime());

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.RDG2_PIN_PULL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (!(entityLiving instanceof Player player)) return;

        int holdTicks = this.getUseDuration(stack) - timeLeft;

        // Шашка ещё не подготовлена — бросок отменяется.
        if (holdTicks < PREPARE_TICKS) {
            CompoundTag cancelTag = stack.getTag();
            if (cancelTag != null) cancelTag.remove(TAG_PULL_START);
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.remove(TAG_PULL_START);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.RDG2_THROW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        if (!level.isClientSide) {
            Rdg2Entity projectile = new Rdg2Entity(level, player);

            int chargeTicks = Math.min(holdTicks - PREPARE_TICKS, MAX_CHARGE_TICKS);
            float chargeFraction = chargeTicks / (float) MAX_CHARGE_TICKS;
            float throwVelocity = MIN_THROW_VELOCITY
                    + (MAX_THROW_VELOCITY - MIN_THROW_VELOCITY) * chargeFraction;

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
                if (this.renderer == null) {
                    this.renderer = new com.danilfb123.aasgranate.client.Rdg2ItemRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, this::predicate));
    }

    private PlayState predicate(AnimationState<Rdg2Item> event) {
        ItemStack stack = event.getData(DataTickets.ITEMSTACK);

        if (stack == null || stack.isEmpty()) {
            event.getController().forceAnimationReset();
            return PlayState.STOP;
        }

        CompoundTag tag = stack.getTag();

        if (tag != null && tag.contains(TAG_PULL_START)) {
            long pullStart = tag.getLong(TAG_PULL_START);
            if (clientGameTime() - pullStart < PULL_STALE_TICKS) {
                return event.setAndContinue(ANIM_PULL);
            }
        }

        event.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    private long clientGameTime() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return com.danilfb123.aasgranate.client.ClientHitboxDebugTracker.getClientGameTime();
        }
        return 0L;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

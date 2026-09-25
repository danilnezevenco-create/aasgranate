package com.danilfb123.aasgranate.client;

import com.danilfb123.aasgranate.AasGranate;
import com.danilfb123.aasgranate.M18Entity;
import com.danilfb123.aasgranate.Rdg2Entity;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AasGranate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SmokeStageRenderer {
    private static final Set<Entity> CANDIDATES = new LinkedHashSet<>();
    private static final List<Entity> VISIBLE = new ArrayList<>();
    private static final MultiBufferSource.BufferSource SPARK_BUFFER =
            MultiBufferSource.immediate(new BufferBuilder(64 * 1024));
    private static ClientLevel trackedLevel;
    private static final MultiBufferSource.BufferSource SHADER_BUFFER =
            MultiBufferSource.immediate(new BufferBuilder(256 * 1024));
    private SmokeStageRenderer() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() == trackedLevel && isSmokeEntity(event.getEntity()))
            CANDIDATES.add(event.getEntity());
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        CANDIDATES.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) trackLevel(Minecraft.getInstance().level);
    }

    private static void trackLevel(ClientLevel level) {
        if (level == trackedLevel) return;
        CANDIDATES.clear();
        VISIBLE.clear();
        SmokePipeline.close();
        trackedLevel = level;
        if (level != null) for (Entity entity : level.entitiesForRendering())
            if (isSmokeEntity(entity)) CANDIDATES.add(entity);
    }

    private static boolean isSmokeEntity(Entity entity) {
        return entity instanceof Rdg2Entity || entity instanceof M18Entity;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            prepare(event);
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            SmokePipeline.afterParticles();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            SmokePipeline.afterLevel();
        }
    }

    private static void prepare(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        trackLevel(mc.level);
        if (mc.level == null || !SmokeShaders.ready()) return;
        VISIBLE.clear();
        float partial = event.getPartialTick();
        for (Entity entity : CANDIDATES) {
            if (entity.isRemoved()) continue;
            boolean smoking = entity instanceof Rdg2Entity e
                    ? e.isSmoking() && e.getSmokeDensity(partial) > 0.002F
                    : ((M18Entity) entity).isSmoking() && ((M18Entity) entity).getSmokeDensity(partial) > 0.002F;
            if (smoking && event.getFrustum().isVisible(bounds(entity, partial))) VISIBLE.add(entity);
        }
        if (VISIBLE.isEmpty()) return;

        PoseStack poses = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        boolean shaders = IrisCompat.shadersActive();
        SmokeBillboard.shaderMode = shaders;
        if (shaders) SmokePuffTexture.ensure();
        BufferBuilder builder = shaders ? null : SmokePipeline.begin(event.getProjectionMatrix());

        SmokeTimeOfDay.update(mc.level, partial);
        if (shaders) SmokeTimeOfDay.neutral();

        MultiBufferSource smoke = shaders
                ? type -> SHADER_BUFFER.getBuffer(SmokeRenderTypes.SMOKE_SHADERS)
                : type -> builder;

        for (Entity entity : VISIBLE) {
            poses.pushPose();
            try {
                poses.translate(Mth.lerp(partial, entity.xo, entity.getX()) - camera.x,
                        Mth.lerp(partial, entity.yo, entity.getY()) - camera.y,
                        Mth.lerp(partial, entity.zo, entity.getZ()) - camera.z);
                int light = dispatcher.getPackedLightCoords(entity, partial);
                SmokeBillboard.light = light;
                if (entity instanceof Rdg2Entity e) {
                    SmokeCloudRenderer.render(e, partial, poses, smoke, light);
                    SparkRenderer.render(e, partial, poses, SPARK_BUFFER);
                } else if (entity instanceof M18Entity e) {
                    M18SmokeCloudRenderer.render(e, partial, poses, smoke, light);
                    M18SparkRenderer.render(e, partial, poses, SPARK_BUFFER);
                }
            } finally {
                poses.popPose();
            }
        }

        if (shaders) SHADER_BUFFER.endBatch(SmokeRenderTypes.SMOKE_SHADERS);
        else SmokePipeline.upload();
        SPARK_BUFFER.endBatch();
    }

    private static AABB bounds(Entity entity, float partial) {
        double x = Mth.lerp(partial, entity.xo, entity.getX());
        double y = Mth.lerp(partial, entity.yo, entity.getY());
        double z = Mth.lerp(partial, entity.zo, entity.getZ());
        double radius = entity instanceof Rdg2Entity ? Rdg2Entity.SMOKE_RADIUS : M18Entity.SMOKE_RADIUS;
        double height = entity instanceof Rdg2Entity ? Rdg2Entity.SMOKE_HEIGHT : M18Entity.SMOKE_HEIGHT;
        double horizontal = radius * 1.85;
        double vertical = radius * 0.70;
        AABB result = new AABB(x - horizontal, y - vertical, z - horizontal,
                x + horizontal, y + height + vertical, z + horizontal);
        // A moving grenade can leave its visible trail far outside the grenade's own box.
        if (entity instanceof Rdg2Entity e) {
            for (Rdg2Entity.TrailPuff puff : e.getTrail())
                result = result.minmax(new AABB(puff.x - 2, puff.y - 2, puff.z - 2,
                        puff.x + 2, puff.y + 2, puff.z + 2));
        } else if (entity instanceof M18Entity e) {
            for (M18Entity.TrailPuff puff : e.getTrail())
                result = result.minmax(new AABB(puff.x - 2, puff.y - 2, puff.z - 2,
                        puff.x + 2, puff.y + 2, puff.z + 2));
        }
        return result;
    }
}

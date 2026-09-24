package com.danilfb123.aasgranate.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

/** One upload and global puff sort per frame; two depth-selected smoke passes. */
final class SmokePipeline {
    private static final BufferBuilder BUILDER = new BufferBuilder(256 * 1024);
    private static final BufferBuilder FULLSCREEN = new BufferBuilder(256);
    private static final Matrix4f MODEL_VIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static VertexBuffer mesh;
    private static RenderTarget glassTarget;
    private static RenderTarget opaqueDepth;
    private static RenderTarget activeGlass;
    private static boolean pending, captured, capturing, fabulous;
    private static int nesting;

    private SmokePipeline() {}

    static BufferBuilder begin(Matrix4f projection) {
        pending = captured = capturing = false;
        nesting = 0;
        activeGlass = null;
        fabulous = Minecraft.useShaderTransparency();
        MODEL_VIEW.set(RenderSystem.getModelViewMatrix());
        PROJECTION.set(projection);
        BUILDER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        return BUILDER;
    }

    static void upload() {
        BUILDER.setQuadSorting(RenderSystem.getVertexSorting());
        BufferBuilder.RenderedBuffer data = BUILDER.end();
        if (data.isEmpty()) {
            data.release();
            return;
        }
        if (mesh == null) mesh = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        mesh.bind();
        try {
            mesh.upload(data); // upload releases RenderedBuffer, including on failure
        } finally {
            VertexBuffer.unbind();
        }
        pending = true;
    }

    static void beginTranslucent() {
        if (!pending || captured) return;
        if (capturing) { nesting++; return; }
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (fabulous) {
            activeGlass = mc.levelRenderer.getTranslucentTarget();
            if (activeGlass == null) return;
            opaqueDepth = resize(opaqueDepth, main);
            opaqueDepth.copyDepthFrom(main);
            activeGlass.bindWrite(false);
        } else {
            glassTarget = resize(glassTarget, main);
            glassTarget.setClearColor(0, 0, 0, 0);
            glassTarget.clear(Minecraft.ON_OSX);
            glassTarget.copyDepthFrom(main);
            glassTarget.bindWrite(false);
            activeGlass = glassTarget;
        }
        capturing = true;
    }

    static void endTranslucent() {
        if (!capturing) return;
        if (nesting > 0) { nesting--; return; }
        capturing = false;
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        try {
            main.bindWrite(false);
            draw(1, false); // behind glass; still tested against opaque-world depth
            if (!fabulous) {
                composite();
                main.copyDepthFrom(activeGlass);
            }
            captured = true;
        } finally {
            main.bindWrite(false);
        }
    }

    static void afterParticles() {
        // If a third-party renderer bypasses the wrapped RenderType, draw before the
        // Fabulous composite clears opaque depth. The fallback must not reveal smoke
        // through solid walls (its glass ordering remains that renderer's limitation).
        if (pending && (!fabulous || !captured)) finish();
    }

    static void afterLevel() {
        // Also covers renderers which omit the usual terrain-layer callback.
        if (pending) finish();
    }

    private static void finish() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        try {
            main.bindWrite(false);
            draw(captured ? 2 : 0, fabulous && captured);
        } finally {
            pending = false;
            activeGlass = null;
            main.bindWrite(false);
        }
    }

    private static void draw(int pass, boolean sampleOpaqueDepth) {
        ShaderInstance shader = SmokeShaders.cloud();
        shader.safeGetUniform("Pass").set(pass);
        shader.safeGetUniform("UseOpaqueDepth").set(sampleOpaqueDepth ? 1 : 0);
        // Never sample a texture attached to the framebuffer being drawn into.
        if (activeGlass != null) shader.setSampler("GlassDepth", activeGlass.getDepthTextureId());
        if (opaqueDepth != null) shader.setSampler("OpaqueDepth", opaqueDepth.getDepthTextureId());
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 771, 1, 771);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        if (sampleOpaqueDepth) RenderSystem.disableDepthTest();
        else { RenderSystem.enableDepthTest(); RenderSystem.depthFunc(515); }
        mesh.bind();
        try {
            mesh.drawWithShader(MODEL_VIEW, PROJECTION, shader);
        } finally {
            VertexBuffer.unbind();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    private static void composite() {
        ShaderInstance shader = SmokeShaders.composite();
        shader.setSampler("GlassColor", activeGlass.getColorTextureId());
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(1, 771, 1, 771);
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shader);
        try {
            FULLSCREEN.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            FULLSCREEN.vertex(-1, -1, 0).uv(0, 0).endVertex();
            FULLSCREEN.vertex( 1, -1, 0).uv(1, 0).endVertex();
            FULLSCREEN.vertex( 1,  1, 0).uv(1, 1).endVertex();
            FULLSCREEN.vertex(-1,  1, 0).uv(0, 1).endVertex();
            BufferUploader.drawWithShader(FULLSCREEN.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    private static RenderTarget resize(RenderTarget target, RenderTarget main) {
        // Depth blits require matching depth formats, including Forge's stencil option.
        if (target != null && target.isStencilEnabled() != main.isStencilEnabled()) {
            target.destroyBuffers();
            target = null;
        }
        if (target == null) {
            target = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            if (main.isStencilEnabled()) target.enableStencil();
            return target;
        }
        if (target.width != main.width || target.height != main.height)
            target.resize(main.width, main.height, Minecraft.ON_OSX);
        return target;
    }

    static void close() {
        pending = captured = capturing = false;
        activeGlass = null;
        if (mesh != null) { mesh.close(); mesh = null; }
        if (glassTarget != null) { glassTarget.destroyBuffers(); glassTarget = null; }
        if (opaqueDepth != null) { opaqueDepth.destroyBuffers(); opaqueDepth = null; }
        SmokePuffCache.clear();
    }
}

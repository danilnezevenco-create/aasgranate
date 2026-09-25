package com.danilfb123.aasgranate.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;

public class SmokeRenderTypes extends RenderType {

    private SmokeRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                             boolean affectsCrumbling, boolean sortOnUpload,
                             Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    /** Дым без шейдерпака: свой шейдер smoke_cloud. */
    public static final RenderType SMOKE = RenderType.create(
            "aasgranate_smoke",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            512 * 1024,
            false,
            true,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(SmokeShaders::cloud))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setLightmapState(NO_LIGHTMAP)
                    .createCompositeState(false));

    /** Дым с шейдерпаком: ванильный шейдер частиц + мягкая текстура, пак обрабатывает как частицы. */
    public static final RenderType SMOKE_SHADERS = RenderType.create(
            "aasgranate_smoke_shaders",
            DefaultVertexFormat.PARTICLE,
            VertexFormat.Mode.QUADS,
            512 * 1024,
            false,
            true,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getParticleShader))
                    .setTextureState(new TextureStateShard(SmokePuffTexture.ID, true, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setLightmapState(LIGHTMAP)
                    .createCompositeState(false));

    /** Искры: аддитивные штрихи. */
    public static final RenderType SPARKS = RenderType.create(
            "aasgranate_sparks",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            64 * 1024,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setLightmapState(NO_LIGHTMAP)
                    .createCompositeState(false));
}
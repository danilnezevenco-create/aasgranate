#version 150

uniform sampler2D GlassDepth;
uniform sampler2D OpaqueDepth;
uniform int Pass;
uniform int UseOpaqueDepth;
uniform vec4 ColorModulator;
in vec4 vertexColor;
in vec2 puffCoord;
out vec4 fragColor;

void main() {
    vec2 p = abs(puffCoord);
    // Distance within the original eight-sided disk, not a replacement circular texture.
    float edge = max(p.x + 0.41421356237 * p.y, p.y + 0.41421356237 * p.x);
    float coverage = clamp((1.0 - edge) / 0.40, 0.0, 1.0);
    if (coverage <= 0.0) discard;

    ivec2 pixel = ivec2(gl_FragCoord.xy);
    if (Pass != 0) {
        float glass = texelFetch(GlassDepth, pixel, 0).r;
        bool behind = gl_FragCoord.z > glass;
        if ((Pass == 1 && !behind) || (Pass == 2 && behind)) discard;
    }
    // Fabulous clears main depth during its final composite; use the saved opaque depth.
    if (UseOpaqueDepth != 0 && gl_FragCoord.z > texelFetch(OpaqueDepth, pixel, 0).r) discard;
    fragColor = vertexColor * ColorModulator;
    fragColor.a *= coverage;
}

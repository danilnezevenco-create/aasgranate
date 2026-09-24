#version 150

uniform sampler2D GlassColor;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    // Already premultiplied by vanilla's blend into the transparent black target.
    fragColor = texture(GlassColor, texCoord);
}

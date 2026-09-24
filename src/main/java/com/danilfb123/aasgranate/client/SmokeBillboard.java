package com.danilfb123.aasgranate.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Four vertices; the shader reproduces the original octagon and 60% solid core. */
final class SmokeBillboard {
    private SmokeBillboard() {}

    static void draw(VertexConsumer vc, Matrix4f pose, Vector3f right, Vector3f up,
                     float x, float y, float z, float radius, int r, int g, int b, int a) {
        vertex(vc, pose, right, up, x, y, z, radius, -1, -1, r, g, b, a);
        vertex(vc, pose, right, up, x, y, z, radius,  1, -1, r, g, b, a);
        vertex(vc, pose, right, up, x, y, z, radius,  1,  1, r, g, b, a);
        vertex(vc, pose, right, up, x, y, z, radius, -1,  1, r, g, b, a);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Vector3f right, Vector3f up,
                               float x, float y, float z, float radius, float u, float v,
                               int r, int g, int b, int a) {
        vc.vertex(pose, x + radius * (right.x * u + up.x * v),
                        y + radius * (right.y * u + up.y * v),
                        z + radius * (right.z * u + up.z * v))
                .color(r, g, b, a).uv(u, v).endVertex();
    }
}

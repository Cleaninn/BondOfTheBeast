package com.bondofthebeast.client;

import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/** A collar with a compact catalyst reservoir beneath its buckle. */
public final class InfusedCollarModel extends Model {
    private final ModelPart collar;
    private final ModelPart metal;
    public InfusedCollarModel() {
        super(RenderLayer::getEntityCutoutNoCull);
        var data = new ModelData();
        data.getRoot().addChild("infused", ModelPartBuilder.create()
                .uv(8, 0).cuboid(-4.5f, 0, -3, 9, 1, 1)
                .uv(8, 0).cuboid(-4.5f, 0, 2, 9, 1, 1)
                .uv(8, 0).cuboid(-4.5f, 0, -2, 1, 1, 4)
                .uv(8, 0).cuboid(3.5f, 0, -2, 1, 1, 4)
                .uv(8, 0).cuboid(-0.75f, 1, -4, 1.5f, 2.5f, 1.5f), ModelTransform.NONE);
        data.getRoot().addChild("metal", ModelPartBuilder.create()
                .uv(1, 1).cuboid(-1, -0.5f, -3.5f, 2, 2, 1)
                .uv(1, 1).cuboid(-1, 1, -4.25f, 2, 0.5f, 2), ModelTransform.NONE);
        var root = TexturedModelData.of(data, 64, 64).createModel();
        collar = root.getChild("infused");
        metal = root.getChild("metal");
    }
    @Override public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                                 float red, float green, float blue, float alpha) {
        collar.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        metal.render(matrices, vertices, light, overlay, 0.65f, 0.65f, 0.7f, alpha);
    }
}

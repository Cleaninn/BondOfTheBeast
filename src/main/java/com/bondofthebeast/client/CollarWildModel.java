package com.bondofthebeast.client;

import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/** Closed neck band and buckle; follows the head of feral player forms. */
public class CollarWildModel extends Model {
    private final ModelPart band;
    public CollarWildModel() {
        super(RenderLayer::getEntityCutoutNoCull);
        ModelData data = new ModelData();
        data.getRoot().addChild("band", ModelPartBuilder.create()
                .uv(8, 0).cuboid(-4.5f, 0, -3, 9, 1, 1)
                .uv(8, 0).cuboid(-4.5f, 0, 2, 9, 1, 1)
                .uv(8, 0).cuboid(-4.5f, 0, -2, 1, 1, 4)
                .uv(8, 0).cuboid(3.5f, 0, -2, 1, 1, 4)
                .uv(1, 1).cuboid(-1, -0.5f, -3.5f, 2, 2, 1), ModelTransform.NONE);
        band = TexturedModelData.of(data, 64, 64).createModel().getChild("band");
    }
    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        band.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}

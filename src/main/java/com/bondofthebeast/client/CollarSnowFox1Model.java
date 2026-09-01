package com.bondofthebeast.client;

import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public class CollarSnowFox1Model extends Model {
    private final ModelPart bone;

    public CollarSnowFox1Model(ModelPart root) {
        super(RenderLayer::getEntityCutoutNoCull);
        this.bone = root.getChild("bone");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        modelPartData.addChild("bone", ModelPartBuilder.create()
                        .uv(14, 0).cuboid(-4.0F, -13.5F, -1.75F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(13, 0).cuboid(-10.0F, -14.5F, -1.75F, 3.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(6, 0).cuboid(-10.0F, -15.25F, 5.25F, 10.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(14, 0).cuboid(-8.0F, -13.5F, -1.75F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(13, 0).cuboid(-3.0F, -14.5F, -1.75F, 3.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(11, -4).cuboid(-1.0F, -14.5F, -0.75F, 1.0F, 1.0F, 5.0F, new Dilation(0.0F))
                        .uv(14, -1).cuboid(-10.0F, -14.5F, -0.75F, 1.0F, 1.0F, 2.0F, new Dilation(0.0F))
                        .uv(12, -3).cuboid(-1.0F, -15.25F, 1.25F, 1.0F, 1.0F, 4.0F, new Dilation(0.0F))
                        .uv(12, -3).cuboid(-10.0F, -15.25F, 1.25F, 1.0F, 1.0F, 4.0F, new Dilation(0.0F))
                        .uv(1, 1).cuboid(-6.0F, -13.5F, -1.75F, 2.0F, 2.0F, 1.0F, new Dilation(0.0F)),
                ModelTransform.pivot(5.0F, 14.0F, -2.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        bone.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
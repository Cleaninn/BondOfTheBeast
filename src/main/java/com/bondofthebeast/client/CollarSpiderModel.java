package com.bondofthebeast.client;

import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public class CollarSpiderModel extends Model {
    private final ModelPart bone;

    public CollarSpiderModel(ModelPart root) {
        super(RenderLayer::getEntityCutoutNoCull);
        this.bone = root.getChild("bone");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        modelPartData.addChild("bone", ModelPartBuilder.create()
                        .uv(14, 0).cuboid(-4.0F, -13.25F, -3.25F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(13, 0).cuboid(-10.55F, -14.25F, -3.0F, 3.25F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(6, 0).cuboid(-10.25F, -14.9F, 6.75F, 10.5F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(14, 0).cuboid(-8.0F, -13.25F, -3.25F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(13, 0).cuboid(-2.4F, -14.25F, -3.0F, 3.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(8, -7).cuboid(-0.35F, -14.9F, -2.25F, 1.0F, 1.0F, 9.0F, new Dilation(0.0F))
                        .uv(2, -1).cuboid(-10.65F, -14.9F, -2.25F, 1.0F, 1.0F, 9.0F, new Dilation(0.0F))
                        .uv(1, 1).cuboid(-6.0F, -13.05F, -3.5F, 2.0F, 2.0F, 1.0F, new Dilation(0.0F)),
                ModelTransform.pivot(5.0F, 14.0F, -2.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        bone.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
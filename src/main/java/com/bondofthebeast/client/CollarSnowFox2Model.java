package com.bondofthebeast.client;

import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public class CollarSnowFox2Model extends Model {
    private final ModelPart bone;

    public CollarSnowFox2Model(ModelPart root) {
        super(RenderLayer::getEntityCutoutNoCull);
        this.bone = root.getChild("bone");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        ModelPartData bone = modelPartData.addChild("bone", ModelPartBuilder.create()
                        .uv(14, 0).cuboid(-4.0F, -13.5F, -2.75F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(13, 0).cuboid(-10.1F, -14.5F, -2.5F, 3.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(14, 0).cuboid(-8.0F, -13.5F, -2.75F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(13, 0).cuboid(-2.9F, -14.5F, -2.5F, 3.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(6, 0).cuboid(-10.0F, -16.55F, 5.75F, 10.0F, 1.0F, 1.0F, new Dilation(0.0F))
                        .uv(1, 1).cuboid(-6.0F, -13.5F, -2.75F, 2.0F, 2.0F, 1.0F, new Dilation(0.0F)),
                ModelTransform.pivot(5.0F, 14.0F, -2.0F));

        bone.addChild("cube_r1", ModelPartBuilder.create()
                        .uv(8, -7).cuboid(0.0F, -1.0F, -1.0F, 1.0F, 1.0F, 8.0F, new Dilation(0.0F))
                        .uv(8, -7).cuboid(9.2F, -1.0F, -1.0F, 1.0F, 1.0F, 8.0F, new Dilation(0.0F)),
                ModelTransform.of(-10.1F, -14.0F, -0.75F, 0.2182F, 0.0F, 0.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        bone.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
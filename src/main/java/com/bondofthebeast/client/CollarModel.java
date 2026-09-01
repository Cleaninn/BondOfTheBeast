package com.bondofthebeast.client;

import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.model.Model;

public class CollarModel extends Model {
	private final ModelPart bone;

	public CollarModel(ModelPart root) {
		super(RenderLayer::getEntityCutoutNoCull);
		this.bone = root.getChild("bone");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();

		modelPartData.addChild("bone", ModelPartBuilder.create()
						.uv(14, 0).cuboid(-4.0F, -13.5F, -1.0F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
						.uv(14, 0).cuboid(-9.0F, -14.5F, -1.0F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
						.uv(8, 0).cuboid(-9.0F, -14.5F, 4.0F, 8.0F, 1.0F, 1.0F, new Dilation(0.0F))
						.uv(14, 0).cuboid(-8.0F, -13.5F, -1.0F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
						.uv(14, 0).cuboid(-3.0F, -14.5F, -1.0F, 2.0F, 1.0F, 1.0F, new Dilation(0.0F))
						.uv(10, -5).cuboid(-2.0F, -14.5F, -1.0F, 1.0F, 1.0F, 6.0F, new Dilation(0.0F))
						.uv(10, -5).cuboid(-9.0F, -14.5F, -1.0F, 1.0F, 1.0F, 6.0F, new Dilation(0.0F))
						.uv(1, 1).cuboid(-6.0F, -13.5F, -1.0F, 2.0F, 2.0F, 1.0F, new Dilation(0.0F)),
				ModelTransform.pivot(5.0F, 14.0F, -2.0F));

		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
		bone.render(matrices, vertices, light, overlay, red, green, blue, alpha);
	}
}
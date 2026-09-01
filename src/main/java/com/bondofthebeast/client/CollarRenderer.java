package com.bondofthebeast.client;

import com.bondofthebeast.BondOfTheBeast;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import net.onixary.shapeShifterCurseFabric.player_form.ability.PlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBodyType;

public class CollarRenderer implements TrinketRenderer {
    private static final Identifier TEXTURE = new Identifier(BondOfTheBeast.MOD_ID, "textures/entity/collar.png");

    private final CollarModel normalModel;
    private final CollarSnowFoxModel fox0Model;
    private final CollarSnowFox1Model fox1Model;
    private final CollarSnowFox2Model fox2Model;
    private final CollarSpiderModel spiderModel;

    public CollarRenderer() {
        this.normalModel = new CollarModel(CollarModel.getTexturedModelData().createModel());
        this.fox0Model = new CollarSnowFoxModel(CollarSnowFoxModel.getTexturedModelData().createModel());
        this.fox1Model = new CollarSnowFox1Model(CollarSnowFox1Model.getTexturedModelData().createModel());
        this.fox2Model = new CollarSnowFox2Model(CollarSnowFox2Model.getTexturedModelData().createModel());
        this.spiderModel = new CollarSpiderModel(CollarSpiderModel.getTexturedModelData().createModel()); // Инициализировали
    }

    @Override
    public void render(ItemStack stack, SlotReference slotReference, EntityModel<? extends LivingEntity> contextModel, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (entity instanceof PlayerEntity player && contextModel instanceof PlayerEntityModel<?> playerModel) {
            PlayerFormComponent formComponent = (PlayerFormComponent) RegPlayerFormComponent.PLAYER_FORM.get(player);
            String formId = "none";
            boolean isFeral = false;

            if (formComponent != null && formComponent.getCurrentForm() != null) {
                formId = formComponent.getCurrentForm().getIDString();
                isFeral = formComponent.getCurrentForm().getBodyType() == PlayerFormBodyType.FERAL;
            }

            matrices.push();
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.normalModel.getLayer(TEXTURE));

            if (formId.contains("spider_3") || formId.contains("spider_2")) {
                playerModel.body.rotate(matrices);
                this.spiderModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
            }
            else if (formId.contains("snow_fox")) {
                if (formId.equals("shape-shifter-curse:snow_fox_0")) {
                    playerModel.body.rotate(matrices);
                    this.fox0Model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
                }
                else if (formId.equals("shape-shifter-curse:snow_fox_1")) {
                    playerModel.body.rotate(matrices);
                    this.fox1Model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
                }
                else if (formId.equals("shape-shifter-curse:snow_fox_2")) {
                    playerModel.body.rotate(matrices);
                    this.fox2Model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
                }
                else {
                    playerModel.head.rotate(matrices);
                    this.fox0Model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
                }
            }
            else if (isFeral || formId.equals("shape-shifter-curse:feral_cat_sp") || formId.equals("shape-shifter-curse:anubis_wolf_3")) {
                playerModel.head.rotate(matrices);
                this.normalModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
            }
            else {
                playerModel.body.rotate(matrices);
                this.normalModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
            }

            matrices.pop();
        }
    }
}
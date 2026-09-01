package com.bondofthebeast.mixin;

import com.bondofthebeast.component.ModComponents;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFF)V", at = @At("TAIL"))
    private void rotateBatFormSleeping(AbstractClientPlayerEntity player, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta, CallbackInfo ci) {
        if (player.isSleeping()) {
            try {
                var sscComp = RegPlayerFormComponent.PLAYER_FORM.get(player);
                if (sscComp != null && sscComp.getCurrentForm() != null) {
                    String path = sscComp.getCurrentForm().FormID.getPath().toLowerCase();

                    // Если это летучая мышь 2 или 3 стадии
                    if (path.contains("bat") && (path.endsWith("_2") || path.endsWith("_3"))) {
                        // Меняем -90.0F на 90.0F, чтобы развернуть модель в правильную сторону
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));

                        // Смещаем модельку чуть ниже/выше, чтобы она идеально легла на подстилку
                        matrices.translate(0.0, -0.2, 0.0);
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
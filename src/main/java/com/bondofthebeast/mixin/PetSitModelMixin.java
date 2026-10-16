package com.bondofthebeast.mixin;

import com.bondofthebeast.component.ModComponents;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class PetSitModelMixin {
    @Inject(method = "setAngles", at = @At("HEAD"))
    private void botb$sitHumanoid(LivingEntity entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof PlayerEntity player && !player.isSleeping() && !player.isTouchingWater() && ModComponents.PLAYER_BOND.get(player).isSitting())
            ((BipedEntityModel<?>) (Object) this).riding = true;
    }
}

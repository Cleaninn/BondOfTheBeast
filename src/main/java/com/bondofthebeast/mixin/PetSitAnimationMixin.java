package com.bondofthebeast.mixin;

import com.bondofthebeast.component.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.onixary.shapeShifterCurseFabric.player_animation.v3.AnimRegistries;
import net.onixary.shapeShifterCurseFabric.player_animation.v3.AnimSystem;
import net.onixary.shapeShifterCurseFabric.player_animation.v3.AnimFSM.FSMUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FSMUtils.class, remap = false)
public abstract class PetSitAnimationMixin {
    @Inject(method = "ProcessUniversalAnim", at = @At("HEAD"), cancellable = true)
    private static void botb$sittingPose(PlayerEntity player, AnimSystem.AnimSystemData data, CallbackInfoReturnable<Identifier> cir) {
        if (ModComponents.PLAYER_BOND.get(player).isSitting() && !player.isSleeping() &&
                !player.hasVehicle() && !player.isTouchingWater() && data.IsOnGround)
            cir.setReturnValue(AnimRegistries.ANIM_STATE_RIDE);
    }
}

package com.bondofthebeast.mixin;

import com.bondofthebeast.block.PetBedBlock;
import com.bondofthebeast.component.ModComponents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBase;
import net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void forceSittingPose(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            try {
                if (ModComponents.PLAYER_BOND.get(player).isSitting()) {
                    cir.setReturnValue(true);
                }
            } catch (Exception ignored) {}
        }
    }

    @Inject(method = "getPose", at = @At("HEAD"), cancellable = true)
    private void modifyPoseOnPetBed(CallbackInfoReturnable<EntityPose> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            if (player.isSleeping() && player.getSleepingPosition().isPresent()) {
                BlockPos pos = player.getSleepingPosition().get();
                if (player.getWorld().getBlockState(pos).getBlock() instanceof PetBedBlock) {
                    try {
                        PlayerFormBase form = RegPlayerFormComponent.PLAYER_FORM.get(player).getCurrentForm();
                        if (form != null) {
                            String path = form.FormID.getPath().toLowerCase();
                            if ((path.contains("wolf") && path.endsWith("_3")) ||
                                    (path.contains("ocelot") && path.endsWith("_3")) ||
                                    (path.contains("fox") && path.endsWith("_3")) ||
                                    path.contains("feral") ||
                                    (path.contains("bat") && (path.endsWith("_2") || path.endsWith("_3")))) {
                                cir.setReturnValue(EntityPose.STANDING);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
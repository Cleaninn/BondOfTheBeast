package com.bondofthebeast.mixin;

import com.bondofthebeast.block.PetBedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "getSleepingDirection", at = @At("HEAD"), cancellable = true)
    private void fixPetBedRotation(CallbackInfoReturnable<Direction> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        entity.getSleepingPosition().ifPresent(pos -> {
            BlockState state = entity.getWorld().getBlockState(pos);
            if (state.getBlock() instanceof PetBedBlock) {
                cir.setReturnValue(state.get(PetBedBlock.FACING));
            }
        });
    }

    private void lockEntityOnPetBed(LivingEntity entity, BlockPos pos, BlockState state) {
        Direction facing = state.get(PetBedBlock.FACING);
        float yaw = facing.asRotation();

        double targetX = pos.getX() + 0.5;
        double targetZ = pos.getZ() + 0.5;
        double targetY = pos.getY() + 0.55;

        double shift = 0.1;
        double yOffset = -0.2;

        if (entity instanceof PlayerEntity player) {
            try {
                PlayerFormBase form = RegPlayerFormComponent.PLAYER_FORM.get(player).getCurrentForm();
                if (form != null) {
                    String path = form.FormID.getPath().toLowerCase();

                    // КАТЕГОРИЯ 1: ЧЕТВЕРОНОГИЕ (Стадия 3 + Ферал) — Ровно по центру, низко
                    if ((path.contains("wolf") && path.endsWith("_3")) ||
                            (path.contains("ocelot") && path.endsWith("_3")) ||
                            (path.contains("fox") && path.endsWith("_3")) ||
                            path.contains("feral")) {

                        shift = 0.5;
                        yOffset = -0.4;
                    }
                    // КАТЕГОРИЯ 2: ПОЛУЗВЕРИ (Стадия 2 + Аксолотли) — Чуть ближе к центру, чуть ниже человека
                    else if ((path.contains("wolf") && path.endsWith("_2")) ||
                            (path.contains("ocelot") && path.endsWith("_2")) ||
                            (path.contains("fox") && (path.endsWith("_2") || path.endsWith("_1") || path.endsWith("_0"))) ||
                            (path.contains("axolotl") && (path.endsWith("_2") || path.endsWith("_3")))) {

                        shift = 0.2;
                        yOffset = -0.2;
                    }
                    // КАТЕГОРИЯ 3: ЛЕТУЧИЕ МЫШИ (Стадии 2 и 3) — Центрируем под горизонтальный поворот
                    else if (path.contains("bat") && (path.endsWith("_2") || path.endsWith("_3"))) {
                        shift = 0.5;
                        yOffset = -0.3;
                    }
                }
            } catch (Exception ignored) {}
        }

        targetX -= facing.getOffsetX() * shift;
        targetZ -= facing.getOffsetZ() * shift;
        targetY += yOffset;

        entity.setVelocity(Vec3d.ZERO);
        entity.forwardSpeed = 0.0f;
        entity.sidewaysSpeed = 0.0f;
        entity.upwardSpeed = 0.0f;

        entity.setPos(targetX, targetY, targetZ);
        entity.setYaw(yaw);
        entity.setHeadYaw(yaw);
        entity.setBodyYaw(yaw);

        entity.prevX = targetX;
        entity.prevY = targetY;
        entity.prevZ = targetZ;
        entity.prevYaw = yaw;
        entity.prevHeadYaw = yaw;
        entity.prevBodyYaw = yaw;
    }

    @Inject(method = "sleep", at = @At("RETURN"))
    private void onSleepStart(BlockPos pos, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        BlockState state = entity.getWorld().getBlockState(pos);
        if (state.getBlock() instanceof PetBedBlock) lockEntityOnPetBed(entity, pos, state);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void keepLockedDuringSleep(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.isSleeping()) {
            entity.getSleepingPosition().ifPresent(pos -> {
                BlockState state = entity.getWorld().getBlockState(pos);
                if (state.getBlock() instanceof PetBedBlock) lockEntityOnPetBed(entity, pos, state);
            });
        }
    }
}
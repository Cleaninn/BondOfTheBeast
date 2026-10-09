package com.bondofthebeast.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PlayerStunMixin {

    // The slowness marker sets the movement attribute to zero; allow a small controlled sidestep.
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3d injectStunMovement(Vec3d movementInput) {
        if ((Object) this instanceof PlayerEntity player) {
            if (player.hasStatusEffect(StatusEffects.SLOWNESS) &&
                    player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() == 99) {
                Vec3d dodge = movementInput.lengthSquared() > 0 ? movementInput.normalize()
                        .rotateY(-player.getYaw() * ((float) Math.PI / 180))
                        .multiply(com.bondofthebeast.ForcedTamingService.STUN_DODGE_SPEED) : Vec3d.ZERO;
                player.setVelocity(dodge.x, player.getVelocity().y, dodge.z);
                return Vec3d.ZERO;
            }
        }
        return movementInput;
    }

    // 2. Блокируем саму попытку прыжка (Space)
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void injectStunJump(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player) {
            if (player.hasStatusEffect(StatusEffects.SLOWNESS) &&
                    player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() == 99) {
                // Прерываем выполнение метода jump(), игрок даже не оторвется от земли
                ci.cancel();
            }
        }
    }
}

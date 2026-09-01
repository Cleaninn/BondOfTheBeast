package com.bondofthebeast.mixin;

import com.bondofthebeast.component.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void overrideName(CallbackInfoReturnable<Text> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        try {
            var bond = ModComponents.PLAYER_BOND.get(player);
            if (bond.hasOwner() && bond.getPetNickname() != null && !bond.getPetNickname().isEmpty()) {
                cir.setReturnValue(Text.literal(bond.getPetNickname()));
            }
        } catch (Exception ignored) {}
    }

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void overrideDisplayName(CallbackInfoReturnable<Text> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        try {
            var bond = ModComponents.PLAYER_BOND.get(player);
            if (bond.hasOwner() && bond.getPetNickname() != null && !bond.getPetNickname().isEmpty()) {
                cir.setReturnValue(Text.literal(bond.getPetNickname()));
            }
        } catch (Exception ignored) {}
    }
}
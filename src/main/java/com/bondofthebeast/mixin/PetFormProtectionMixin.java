package com.bondofthebeast.mixin;

import com.bondofthebeast.BondRules;
import net.minecraft.entity.player.PlayerEntity;
import net.onixary.shapeShifterCurseFabric.player_form.IForm;
import net.onixary.shapeShifterCurseFabric.player_form.utils.FormUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FormUtils.class, remap = false)
public abstract class PetFormProtectionMixin {
    @Inject(method = "setForm", at = @At("HEAD"), cancellable = true)
    private static void botb$keepOwnedCatForm(PlayerEntity player, IForm form, CallbackInfo ci) {
        // Stop before powers, scale, or accessory effects change as well as the form itself.
        if (BondRules.blocksHumanReturn(player, form)) ci.cancel();
    }
}
